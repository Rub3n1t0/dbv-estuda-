@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.telas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dbv.estuda.data.Modo
import com.dbv.estuda.data.Pergunta
import com.dbv.estuda.data.RespostaDetalhe
import com.dbv.estuda.data.Repositorio
import com.dbv.estuda.data.TentativaComNome
import com.dbv.estuda.data.UsuarioLogado
import com.dbv.estuda.ui.comum.Barra
import com.dbv.estuda.ui.comum.BarraTopo
import com.dbv.estuda.ui.comum.Carregando
import com.dbv.estuda.ui.comum.Titulo
import com.dbv.estuda.ui.comum.Vazio
import com.dbv.estuda.ui.comum.container
import com.dbv.estuda.ui.comum.fmtData
import com.dbv.estuda.ui.comum.fmtNota
import com.dbv.estuda.ui.theme.Amarelo
import com.dbv.estuda.ui.theme.Azul
import com.dbv.estuda.ui.theme.AzulClaro
import com.dbv.estuda.ui.theme.Cinza
import com.dbv.estuda.ui.theme.Linha
import com.dbv.estuda.ui.theme.Verde
import com.dbv.estuda.ui.theme.VerdeClaro
import com.dbv.estuda.ui.theme.Vermelho
import com.dbv.estuda.ui.theme.VermelhoClaro
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- ViewModel ----------------

/** Uma pergunta com as opções embaralhadas. [ordem] guarda os índices originais na ordem exibida. */
data class ItemQuiz(val pergunta: Pergunta, val ordem: List<Int>)

data class EstadoQuiz(
    val carregando: Boolean = true,
    val titulo: String = "",
    val codigo: String = "",
    val itens: List<ItemQuiz> = emptyList(),
    val atual: Int = 0,
    /** id da pergunta -> índice original escolhido */
    val escolhas: Map<Long, Int> = emptyMap(),
    /** No treino, perguntas já conferidas (mostra certo/errado). */
    val conferidas: Set<Long> = emptySet(),
    val segundosRestantes: Int? = null,
    val notaMinima: Int = 70,
    val erro: String? = null,
    val salvando: Boolean = false,
    val tentativaId: Long? = null,
)

class QuizViewModel(
    private val repo: Repositorio,
    private val usuario: UsuarioLogado,
    val modo: String,
    private val codigo: String?,
    private val provaId: Long?,
    private val quantidade: Int,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoQuiz())
    val estado: StateFlow<EstadoQuiz> = _estado
    private val inicio = System.currentTimeMillis()

    init {
        viewModelScope.launch { carregar() }
    }

    private suspend fun carregar() {
        if (modo == Modo.PROVA) {
            val prova = provaId?.let { repo.provas.prova(it) }
            when {
                prova == null -> return falhar("Prova não encontrada.")
                !prova.aberta -> return falhar("Esta prova está fechada pelo instrutor.")
                repo.provas.tentativasNaProva(prova.id, usuario.id) > 0 -> return falhar("Você já fez esta prova.")
            }
            val qs = repo.provas.perguntasDaProva(prova!!.id)
            _estado.value = EstadoQuiz(
                carregando = false, titulo = prova.titulo, codigo = prova.especialidadeCodigo,
                itens = qs.map { embaralhar(it) }, notaMinima = prova.notaMinima,
                segundosRestantes = if (prova.tempoLimiteMin > 0) prova.tempoLimiteMin * 60 else null,
            )
            if (prova.tempoLimiteMin > 0) iniciarRelogio()
        } else {
            val esp = codigo?.let { repo.conteudo.especialidade(it) } ?: return falhar("Especialidade não encontrada.")
            val qs = repo.conteudo.perguntasAtivas(esp.codigo).shuffled().let { if (quantidade > 0) it.take(quantidade) else it }
            if (qs.isEmpty()) return falhar("Esta especialidade ainda não tem perguntas.")
            _estado.value = EstadoQuiz(carregando = false, titulo = "Treino · ${esp.nome}", codigo = esp.codigo,
                itens = qs.map { embaralhar(it) })
        }
    }

    private fun embaralhar(p: Pergunta) = ItemQuiz(p, p.opcoes.indices.shuffled())

    private fun falhar(msg: String) {
        _estado.value = EstadoQuiz(carregando = false, erro = msg)
    }

    private fun iniciarRelogio() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _estado.value
                if (s.tentativaId != null || s.salvando) break
                val r = (s.segundosRestantes ?: break) - 1
                _estado.update { it.copy(segundosRestantes = r) }
                if (r <= 0) { finalizar(); break }
            }
        }
    }

    fun escolher(indiceOriginal: Int) {
        val s = _estado.value
        val p = s.itens.getOrNull(s.atual)?.pergunta ?: return
        if (p.id in s.conferidas) return
        _estado.update { it.copy(escolhas = it.escolhas + (p.id to indiceOriginal)) }
    }

    fun conferir() {
        val s = _estado.value
        val p = s.itens.getOrNull(s.atual)?.pergunta ?: return
        if (p.id in s.escolhas) _estado.update { it.copy(conferidas = it.conferidas + p.id) }
    }

    fun irPara(i: Int) {
        _estado.update { it.copy(atual = i.coerceIn(0, (it.itens.size - 1).coerceAtLeast(0))) }
    }

    fun finalizar() {
        val s = _estado.value
        if (s.salvando || s.tentativaId != null || s.itens.isEmpty()) return
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val id = repo.salvarTentativa(
                usuarioId = usuario.id, codigo = s.codigo, provaId = provaId, modo = modo, inicio = inicio,
                perguntas = s.itens.map { it.pergunta }, escolhas = s.escolhas, notaMinima = s.notaMinima,
            )
            _estado.update { it.copy(salvando = false, tentativaId = id) }
        }
    }
}

// ---------------- Tela do quiz ----------------

@Composable
fun TelaQuiz(
    modo: String, codigo: String?, provaId: Long?, quantidade: Int,
    aoSair: () -> Unit, aoTerminar: (Long) -> Unit,
) {
    val c = container()
    val usuario = c.sessao.atual.collectAsState().value ?: return
    val vm: QuizViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(c.repo, usuario, modo, codigo, provaId, quantidade) as T
    })
    val s by vm.estado.collectAsState()
    var confirmarSaida by remember { mutableStateOf(false) }
    var confirmarEntrega by remember { mutableStateOf(false) }

    LaunchedEffect(s.tentativaId) { s.tentativaId?.let(aoTerminar) }
    BackHandler(enabled = s.erro == null && !s.carregando) { confirmarSaida = true }

    if (confirmarSaida) AlertDialog(
        onDismissRequest = { confirmarSaida = false },
        title = { Text("Sair?") },
        text = { Text(if (modo == Modo.PROVA) "Se sair agora a prova não será entregue e suas respostas serão perdidas." else "Seu treino não será salvo.") },
        confirmButton = { TextButton(onClick = { confirmarSaida = false; aoSair() }) { Text("Sair", color = Vermelho) } },
        dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar") } },
    )
    if (confirmarEntrega) {
        val brancos = s.itens.count { it.pergunta.id !in s.escolhas }
        AlertDialog(
            onDismissRequest = { confirmarEntrega = false },
            title = { Text("Entregar a prova?") },
            text = { Text(if (brancos > 0) "Você deixou $brancos pergunta(s) em branco. Depois de entregar não é possível alterar." else "Depois de entregar não é possível alterar as respostas.") },
            confirmButton = { TextButton(onClick = { confirmarEntrega = false; vm.finalizar() }) { Text("Entregar") } },
            dismissButton = { TextButton(onClick = { confirmarEntrega = false }) { Text("Revisar") } },
        )
    }

    Scaffold(topBar = {
        BarraTopo(s.titulo.ifEmpty { "Teste" }, aoVoltar = { if (s.erro != null) aoSair() else confirmarSaida = true }) {
            s.segundosRestantes?.let { seg ->
                Text(String.format("%d:%02d  ", seg / 60, seg % 60), color = if (seg < 60) Amarelo else Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                s.carregando -> Carregando()
                s.erro != null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Vazio(s.erro!!)
                    Button(onClick = aoSair) { Text("Voltar") }
                }
                else -> CorpoQuiz(s, vm, pedirEntrega = { confirmarEntrega = true })
            }
        }
    }
}

@Composable
private fun CorpoQuiz(s: EstadoQuiz, vm: QuizViewModel, pedirEntrega: () -> Unit) {
    val item = s.itens[s.atual]
    val p = item.pergunta
    val treino = vm.modo == Modo.TREINO
    val conferida = p.id in s.conferidas
    val escolha = s.escolhas[p.id]
    val ultima = s.atual == s.itens.lastIndex

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Pergunta ${s.atual + 1} de ${s.itens.size}", color = Cinza, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Barra((s.atual + 1f) / s.itens.size, Amarelo, altura = 6.dp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(p.enunciado, style = MaterialTheme.typography.titleLarge, color = Azul)
            Spacer(Modifier.height(6.dp))
            item.ordem.forEachIndexed { pos, original ->
                val selecionada = escolha == original
                val (fundo, borda) = when {
                    conferida && original == p.correta -> VerdeClaro to Verde
                    conferida && selecionada -> VermelhoClaro to Vermelho
                    selecionada -> AzulClaro to Azul
                    else -> Color.White to Linha
                }
                Surface(
                    shape = RoundedCornerShape(14.dp), color = fundo, border = BorderStroke(2.dp, borda),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !conferida) { vm.escolher(original) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (selecionada) borda else Linha, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(('A' + pos).toString(), fontWeight = FontWeight.Bold,
                                    color = if (selecionada) Color.White else Azul)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(p.opcoes[original], Modifier.weight(1f))
                        if (conferida && original == p.correta) Icon(Icons.Filled.Check, null, tint = Verde)
                        if (conferida && selecionada && original != p.correta) Icon(Icons.Filled.Close, null, tint = Vermelho)
                    }
                }
            }
            if (treino && conferida) {
                val acertou = escolha == p.correta
                Surface(color = if (acertou) VerdeClaro else VermelhoClaro, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (acertou) "Certa resposta!" else "Não foi dessa vez.", fontWeight = FontWeight.Bold,
                            color = if (acertou) Verde else Vermelho)
                        if (p.explicacao.isNotBlank()) Text(p.explicacao)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!treino) OutlinedButton(onClick = { vm.irPara(s.atual - 1) }, enabled = s.atual > 0, modifier = Modifier.weight(1f)) { Text("Anterior") }
            when {
                treino && !conferida -> Button(onClick = vm::conferir, enabled = escolha != null, modifier = Modifier.weight(1f).height(48.dp)) { Text("Conferir") }
                treino && !ultima -> Button(onClick = { vm.irPara(s.atual + 1) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Próxima") }
                treino -> Button(onClick = vm::finalizar, enabled = !s.salvando, modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde)) { Text("Ver resultado") }
                !ultima -> Button(onClick = { vm.irPara(s.atual + 1) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Próxima") }
                else -> Button(onClick = pedirEntrega, enabled = !s.salvando, modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde)) { Text("Entregar") }
            }
        }
        if (!treino) {
            // mapa de questões: permite pular entre perguntas na prova
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                s.itens.forEachIndexed { i, q ->
                    val respondida = q.pergunta.id in s.escolhas
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when { i == s.atual -> Amarelo; respondida -> Azul; else -> Linha },
                        modifier = Modifier.weight(1f).height(8.dp).clickable { vm.irPara(i) },
                    ) {}
                }
            }
        }
    }
}

// ---------------- Resultado ----------------

private data class DadosResultado(val tentativa: TentativaComNome?, val respostas: List<RespostaDetalhe>)

@Composable
fun TelaResultado(tentativaId: Long, aoVoltar: () -> Unit, treinarDeNovo: (String) -> Unit) {
    val repo = container().repo
    val sessao = container().sessao.atual.collectAsState().value
    val dados by produceState<DadosResultado?>(null, tentativaId) {
        value = DadosResultado(repo.desempenho.tentativa(tentativaId), repo.desempenho.respostas(tentativaId))
    }
    Scaffold(topBar = { BarraTopo("Resultado", aoVoltar) }) { pad ->
        val d = dados
        if (d == null) { Column(Modifier.padding(pad)) { Carregando() }; return@Scaffold }
        val t = d.tentativa ?: run { Column(Modifier.padding(pad)) { Vazio("Tentativa não encontrada.") }; return@Scaffold }
        val nota = t.tentativa.nota
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(color = Azul, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t.provaTitulo ?: t.especialidadeNome, color = Color(0xFFC9D6EA), textAlign = TextAlign.Center)
                        Text(fmtNota(nota), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = if (nota >= 7) Amarelo else Color.White)
                        Text("${t.tentativa.acertos} de ${t.tentativa.total} acertos", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        val (txt, cor) = if (t.tentativa.aprovado) "APROVADO" to Verde else "NÃO ATINGIU A NOTA MÍNIMA" to Vermelho
                        Surface(color = cor, shape = RoundedCornerShape(8.dp)) {
                            Text(txt, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        val min = (t.tentativa.fim - t.tentativa.inicio) / 60000
                        Text("${if (t.tentativa.modo == Modo.PROVA) "Prova" else "Treino"} · ${fmtData(t.tentativa.fim)} · ${min} min",
                            color = Color(0xFFC9D6EA), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (t.tentativa.modo == Modo.TREINO && sessao?.id == t.tentativa.usuarioId) item {
                Button(onClick = { treinarDeNovo(t.tentativa.especialidadeCodigo) }, modifier = Modifier.fillMaxWidth()) { Text("Treinar de novo") }
            }
            item { Titulo("Revisão das respostas") }
            itemsIndexed(d.respostas, key = { _, r -> r.resposta.id }) { i, r -> CartaoRevisao(i + 1, r) }
        }
    }
}

@Composable
private fun CartaoRevisao(n: Int, r: RespostaDetalhe) {
    val ok = r.resposta.correta
    Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Linha), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(if (ok) Icons.Filled.Check else Icons.Filled.Close, null, tint = if (ok) Verde else Vermelho)
                Spacer(Modifier.width(8.dp))
                Text("$n. ${r.enunciado}", fontWeight = FontWeight.SemiBold)
            }
            val sua = r.opcoes.getOrNull(r.resposta.escolhida) ?: "(em branco)"
            Text("Sua resposta: $sua", color = if (ok) Verde else Vermelho)
            if (!ok) Text("Correta: ${r.opcoes.getOrNull(r.indiceCorreto) ?: "–"}", color = Verde, fontWeight = FontWeight.Medium)
            if (r.explicacao.isNotBlank()) Text(r.explicacao, color = Cinza, style = MaterialTheme.typography.bodySmall)
        }
    }
}
