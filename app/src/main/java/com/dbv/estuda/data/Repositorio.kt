package com.dbv.estuda.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.roundToInt
import kotlin.random.Random

data class ResultadoImportacao(val especialidades: Int, val perguntasNovas: Int, val ignoradas: Int)

class Repositorio(private val db: AppDatabase) {
    val usuarios = db.usuarios()
    val conteudo = db.conteudo()
    val provas = db.provas()
    val desempenho = db.desempenho()

    // ---------- Contas ----------

    fun hash(senha: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("dbv-estuda:$senha".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun entrar(nome: String, senha: String): Usuario? {
        val u = usuarios.porNome(nome.trim()) ?: return null
        return if (u.senhaHash == hash(senha)) u else null
    }

    /** Devolve o usuário criado ou lança IllegalArgumentException com a mensagem para a tela. */
    suspend fun cadastrar(nome: String, senha: String, papel: String, clube: String, classe: String?): Usuario {
        val n = nome.trim()
        require(n.length >= 3) { "Informe o nome completo." }
        require(senha.length >= 4) { "A senha precisa de pelo menos 4 caracteres." }
        require(clube.isNotBlank()) { "Informe o clube." }
        require(usuarios.porNome(n) == null) { "Já existe uma conta com esse nome." }
        val u = Usuario(nome = n, senhaHash = hash(senha), papel = papel, clube = clube.trim(),
            classe = if (papel == Papel.DESBRAVADOR) classe else null)
        return u.copy(id = usuarios.inserir(u))
    }

    // ---------- Provas e treinos ----------

    suspend fun criarProva(
        titulo: String, codigo: String, qtd: Int, notaMinima: Int, tempoMin: Int, instrutor: UsuarioLogado,
    ): Long = db.withTransaction {
        val banco = conteudo.perguntasAtivas(codigo)
        require(banco.isNotEmpty()) { "Essa especialidade ainda não tem perguntas." }
        val escolhidas = banco.shuffled().take(qtd.coerceIn(1, banco.size))
        val id = provas.inserirProva(
            Prova(titulo = titulo.trim(), especialidadeCodigo = codigo, instrutorId = instrutor.id,
                clube = instrutor.clube, notaMinima = notaMinima, tempoLimiteMin = tempoMin)
        )
        provas.inserirProvaPerguntas(escolhidas.mapIndexed { i, p -> ProvaPergunta(id, p.id, i) })
        id
    }

    suspend fun salvarTentativa(
        usuarioId: Long, codigo: String, provaId: Long?, modo: String, inicio: Long,
        perguntas: List<Pergunta>, escolhas: Map<Long, Int>, notaMinima: Int,
        fim: Long = System.currentTimeMillis(),
    ): Long {
        val respostas = perguntas.map { p ->
            val e = escolhas[p.id] ?: -1
            Resposta(tentativaId = 0, perguntaId = p.id, escolhida = e, correta = e == p.correta)
        }
        val acertos = respostas.count { it.correta }
        val total = perguntas.size
        val nota = if (total == 0) 0.0 else ((acertos * 100.0 / total).roundToInt() / 10.0)
        val t = Tentativa(
            usuarioId = usuarioId, especialidadeCodigo = codigo, provaId = provaId, modo = modo,
            inicio = inicio, fim = fim, acertos = acertos, total = total,
            nota = nota, aprovado = total > 0 && acertos * 100 >= notaMinima * total,
        )
        return desempenho.salvar(t, respostas)
    }

    /**
     * Se a pergunta já foi respondida, cria uma versão nova e desativa a antiga,
     * para que provas e revisões já feitas continuem coerentes.
     */
    suspend fun editarPergunta(p: Pergunta) = db.withTransaction {
        if (conteudo.vezesRespondida(p.id) > 0) {
            conteudo.desativarPergunta(p.id)
            conteudo.inserirPergunta(p.copy(id = 0, ativa = true))
        } else {
            conteudo.atualizarPergunta(p)
        }
    }

    // ---------- Importar / exportar JSON ----------

    /**
     * Formato aceito (o mesmo de assets/perguntas.json):
     * { "especialidades": [ { "codigo", "nome", "area", "nivel", "descricao",
     *     "perguntas": [ { "enunciado", "opcoes": [..], "correta": 0, "explicacao" } ] } ] }
     */
    suspend fun importarJson(texto: String, autorId: Long? = null): ResultadoImportacao = db.withTransaction {
        val raiz = JSONObject(texto)
        val lista = raiz.getJSONArray("especialidades")
        var esp = 0
        var novas = 0
        var ignoradas = 0
        for (i in 0 until lista.length()) {
            val o = lista.getJSONObject(i)
            val codigo = o.getString("codigo").trim().uppercase()
            val existente = conteudo.especialidade(codigo)
            conteudo.salvarEspecialidade(
                Especialidade(
                    codigo = codigo,
                    nome = o.optString("nome", existente?.nome ?: codigo),
                    area = o.optString("area", existente?.area ?: areaDoCodigo(codigo)),
                    nivel = o.optInt("nivel", existente?.nivel ?: 1),
                    descricao = o.optString("descricao", existente?.descricao ?: ""),
                )
            )
            esp++
            val jaTem = conteudo.enunciados(codigo).map { normalizar(it) }.toMutableSet()
            val perguntas = o.optJSONArray("perguntas") ?: JSONArray()
            val inserir = mutableListOf<Pergunta>()
            for (j in 0 until perguntas.length()) {
                val q = perguntas.getJSONObject(j)
                val enunciado = q.optString("enunciado").trim()
                val ops = q.optJSONArray("opcoes") ?: JSONArray()
                val opcoes = List(ops.length()) { ops.getString(it).trim() }.filter { it.isNotEmpty() }
                val correta = q.optInt("correta", -1)
                if (enunciado.isEmpty() || opcoes.size !in 2..6 || correta !in opcoes.indices ||
                    !jaTem.add(normalizar(enunciado))
                ) {
                    ignoradas++
                    continue
                }
                inserir += Pergunta(
                    especialidadeCodigo = codigo, enunciado = enunciado, opcoes = opcoes,
                    correta = correta, explicacao = q.optString("explicacao", ""), criadaPor = autorId,
                )
            }
            conteudo.inserirPerguntas(inserir)
            novas += inserir.size
        }
        ResultadoImportacao(esp, novas, ignoradas)
    }

    suspend fun exportarJson(): String {
        val arr = JSONArray()
        for (e in conteudo.todasEspecialidades()) {
            val qs = JSONArray()
            conteudo.perguntasAtivas(e.codigo).sortedBy { it.id }.forEach { p ->
                qs.put(JSONObject().apply {
                    put("enunciado", p.enunciado)
                    put("opcoes", JSONArray(p.opcoes))
                    put("correta", p.correta)
                    put("explicacao", p.explicacao)
                })
            }
            arr.put(JSONObject().apply {
                put("codigo", e.codigo); put("nome", e.nome); put("area", e.area)
                put("nivel", e.nivel); put("descricao", e.descricao); put("perguntas", qs)
            })
        }
        return JSONObject().put("especialidades", arr).toString(2)
    }

    // ---------- Dados iniciais ----------

    /** Na primeira abertura carrega o banco de perguntas e as contas de demonstração. */
    suspend fun semearSeNecessario(context: Context) {
        if (conteudo.contarEspecialidades() > 0) return
        val texto = context.assets.open("perguntas.json").bufferedReader().use { it.readText() }
        // tudo ou nada: se o app fechar no meio, a carga recomeça na próxima abertura
        db.withTransaction {
            importarJson(texto)
            criarDemonstracao()
        }
    }

    private suspend fun criarDemonstracao() {
        val clube = "Águias do Vale"
        val carlos = cadastrar("Carlos Lima", "instrutor26", Papel.INSTRUTOR, clube, null)
        val ana = cadastrar("Ana Souza", "aguias2026", Papel.DESBRAVADOR, clube, "Pesquisador")
        val pedro = cadastrar("Pedro Henrique", "dbv2026", Papel.DESBRAVADOR, clube, "Companheiro")
        val marina = cadastrar("Marina Costa", "dbv2026", Papel.DESBRAVADOR, clube, "Pesquisador")
        val julia = cadastrar("Júlia Santos", "dbv2026", Papel.DESBRAVADOR, clube, "Amigo")

        val rnd = Random(2026)
        val dia = 24L * 60 * 60 * 1000
        val agora = System.currentTimeMillis()

        suspend fun simular(u: Usuario, codigo: String, chance: Double, diasAtras: Int, perguntas: List<Pergunta>? = null, provaId: Long? = null) {
            val qs = perguntas ?: conteudo.perguntasAtivas(codigo).shuffled(rnd).take(8)
            if (qs.isEmpty()) return
            val escolhas = qs.associate { p ->
                p.id to if (rnd.nextDouble() < chance) p.correta
                else (p.opcoes.indices - p.correta).random(rnd)
            }
            val fim = agora - diasAtras * dia
            salvarTentativa(u.id, codigo, provaId, if (provaId != null) Modo.PROVA else Modo.TREINO,
                fim - 6 * 60_000, qs, escolhas, 70, fim)
        }

        simular(ana, "ID-001", 0.85, 20); simular(ana, "EN-003", 0.9, 14)
        simular(ana, "CS-003", 0.6, 6); simular(ana, "CS-003", 0.8, 2)
        simular(pedro, "AR-001", 0.7, 18); simular(pedro, "AR-021", 0.55, 9); simular(pedro, "ID-001", 0.75, 4)
        simular(marina, "EN-007", 0.85, 12); simular(marina, "CS-003", 0.9, 5); simular(marina, "AM-054", 0.8, 3)
        simular(julia, "ID-001", 0.6, 10); simular(julia, "AR-001", 0.5, 1)

        val carlosLogado = UsuarioLogado(carlos.id, carlos.nome, carlos.papel, carlos.clube)
        val provaId = criarProva("Prova teórica – Primeiros socorros", "CS-003", 8, 70, 15, carlosLogado)
        val qs = provas.perguntasDaProva(provaId)
        simular(pedro, "CS-003", 0.6, 1, qs, provaId)
        simular(marina, "CS-003", 0.95, 1, qs, provaId)
    }

    companion object {
        fun normalizar(t: String) = java.text.Normalizer.normalize(t.lowercase().trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        fun areaDoCodigo(codigo: String): String =
            if (codigo.startsWith("AM-EB")) "AM-EB" else codigo.substringBefore('-')
    }
}
