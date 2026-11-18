package com.dbv.estuda.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun inserir(u: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE nome = :nome COLLATE NOCASE LIMIT 1")
    suspend fun porNome(nome: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun porId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun observar(id: Long): Flow<Usuario?>

    @Query("SELECT DISTINCT clube FROM usuarios ORDER BY clube")
    fun clubes(): Flow<List<String>>
}

@Dao
interface ConteudoDao {
    @Upsert
    suspend fun salvarEspecialidade(e: Especialidade)

    @Query("SELECT * FROM especialidades WHERE codigo = :codigo")
    suspend fun especialidade(codigo: String): Especialidade?

    @Query("SELECT * FROM especialidades WHERE codigo = :codigo")
    fun observarEspecialidade(codigo: String): Flow<Especialidade?>

    @Query(
        """SELECT e.codigo, e.nome, e.area, e.nivel, e.descricao,
           (SELECT COUNT(*) FROM perguntas p WHERE p.especialidadeCodigo = e.codigo AND p.ativa = 1) AS qtdPerguntas
           FROM especialidades e ORDER BY e.area, e.nome"""
    )
    fun especialidades(): Flow<List<EspecialidadeResumo>>

    @Query("SELECT * FROM especialidades ORDER BY area, nome")
    suspend fun todasEspecialidades(): List<Especialidade>

    @Query("SELECT COUNT(*) FROM especialidades")
    suspend fun contarEspecialidades(): Int

    @Insert
    suspend fun inserirPergunta(p: Pergunta): Long

    @Insert
    suspend fun inserirPerguntas(p: List<Pergunta>)

    @Update
    suspend fun atualizarPergunta(p: Pergunta)

    @Query("UPDATE perguntas SET ativa = 0 WHERE id = :id")
    suspend fun desativarPergunta(id: Long)

    @Query("SELECT * FROM perguntas WHERE especialidadeCodigo = :codigo AND ativa = 1 ORDER BY id")
    fun perguntas(codigo: String): Flow<List<Pergunta>>

    @Query("SELECT * FROM perguntas WHERE especialidadeCodigo = :codigo AND ativa = 1")
    suspend fun perguntasAtivas(codigo: String): List<Pergunta>

    @Query("SELECT * FROM perguntas WHERE id = :id")
    suspend fun pergunta(id: Long): Pergunta?

    @Query("SELECT COUNT(*) FROM respostas WHERE perguntaId = :id")
    suspend fun vezesRespondida(id: Long): Int

    @Query("SELECT enunciado FROM perguntas WHERE especialidadeCodigo = :codigo")
    suspend fun enunciados(codigo: String): List<String>
}

@Dao
interface ProvaDao {
    @Insert
    suspend fun inserirProva(p: Prova): Long

    @Insert
    suspend fun inserirProvaPerguntas(l: List<ProvaPergunta>)

    @Query("UPDATE provas SET aberta = :aberta WHERE id = :id")
    suspend fun definirAberta(id: Long, aberta: Boolean)

    @Query("DELETE FROM provas WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("SELECT * FROM provas WHERE id = :id")
    suspend fun prova(id: Long): Prova?

    @Query("SELECT * FROM provas WHERE id = :id")
    fun observarProva(id: Long): Flow<Prova?>

    @Query(
        """SELECT p.* FROM perguntas p JOIN prova_perguntas pp ON pp.perguntaId = p.id
           WHERE pp.provaId = :provaId ORDER BY pp.ordem"""
    )
    suspend fun perguntasDaProva(provaId: Long): List<Pergunta>

    @Query("SELECT COUNT(*) FROM tentativas WHERE provaId = :provaId AND usuarioId = :usuarioId")
    suspend fun tentativasNaProva(provaId: Long, usuarioId: Long): Int

    @Query(
        """SELECT pr.*, e.nome AS especialidadeNome,
           (SELECT COUNT(*) FROM prova_perguntas pp WHERE pp.provaId = pr.id) AS qtdPerguntas,
           (SELECT COUNT(*) FROM tentativas t WHERE t.provaId = pr.id) AS realizadas,
           (SELECT COUNT(*) FROM tentativas t WHERE t.provaId = pr.id AND t.aprovado = 1) AS aprovados,
           (SELECT AVG(t.nota) FROM tentativas t WHERE t.provaId = pr.id) AS mediaNota
           FROM provas pr JOIN especialidades e ON e.codigo = pr.especialidadeCodigo
           WHERE pr.clube = :clube ORDER BY pr.criadaEm DESC"""
    )
    fun provasDoClube(clube: String): Flow<List<ProvaResumo>>

    @Query(
        """SELECT pr.*, e.nome AS especialidadeNome,
           (SELECT COUNT(*) FROM prova_perguntas pp WHERE pp.provaId = pr.id) AS qtdPerguntas,
           (SELECT t.nota FROM tentativas t WHERE t.provaId = pr.id AND t.usuarioId = :usuarioId ORDER BY t.fim DESC LIMIT 1) AS minhaNota,
           (SELECT t.id FROM tentativas t WHERE t.provaId = pr.id AND t.usuarioId = :usuarioId ORDER BY t.fim DESC LIMIT 1) AS minhaTentativaId
           FROM provas pr JOIN especialidades e ON e.codigo = pr.especialidadeCodigo
           WHERE pr.clube = :clube AND (pr.aberta = 1 OR EXISTS
               (SELECT 1 FROM tentativas t WHERE t.provaId = pr.id AND t.usuarioId = :usuarioId))
           ORDER BY pr.criadaEm DESC"""
    )
    fun provasDoAluno(clube: String, usuarioId: Long): Flow<List<ProvaParaAluno>>

    @Query(
        """SELECT t.id AS tentativaId, u.nome AS nome, t.nota AS nota, t.acertos AS acertos,
           t.total AS total, t.aprovado AS aprovado, t.fim AS fim
           FROM tentativas t JOIN usuarios u ON u.id = t.usuarioId
           WHERE t.provaId = :provaId ORDER BY t.nota DESC, u.nome"""
    )
    fun resultadosDaProva(provaId: Long): Flow<List<ResultadoAluno>>

    @Query(
        """SELECT p.id AS perguntaId, p.enunciado AS enunciado,
           COUNT(r.id) AS respondidas, COALESCE(SUM(r.correta), 0) AS acertos
           FROM prova_perguntas pp JOIN perguntas p ON p.id = pp.perguntaId
           LEFT JOIN respostas r ON r.perguntaId = p.id
               AND r.tentativaId IN (SELECT t.id FROM tentativas t WHERE t.provaId = :provaId)
           WHERE pp.provaId = :provaId
           GROUP BY p.id ORDER BY pp.ordem"""
    )
    fun acertosPorPergunta(provaId: Long): Flow<List<AcertoPergunta>>
}

@Dao
abstract class DesempenhoDao {
    @Insert
    abstract suspend fun inserirTentativa(t: Tentativa): Long

    @Insert
    abstract suspend fun inserirRespostas(r: List<Resposta>)

    /** Grava a tentativa e todas as respostas de uma vez. */
    @Transaction
    open suspend fun salvar(t: Tentativa, respostas: List<Resposta>): Long {
        val id = inserirTentativa(t)
        inserirRespostas(respostas.map { it.copy(tentativaId = id) })
        return id
    }

    @Query(
        """SELECT t.*, e.nome AS especialidadeNome, pr.titulo AS provaTitulo
           FROM tentativas t JOIN especialidades e ON e.codigo = t.especialidadeCodigo
           LEFT JOIN provas pr ON pr.id = t.provaId
           WHERE t.usuarioId = :usuarioId ORDER BY t.fim DESC"""
    )
    abstract fun historico(usuarioId: Long): Flow<List<TentativaComNome>>

    @Query(
        """SELECT t.*, e.nome AS especialidadeNome, pr.titulo AS provaTitulo
           FROM tentativas t JOIN especialidades e ON e.codigo = t.especialidadeCodigo
           LEFT JOIN provas pr ON pr.id = t.provaId
           WHERE t.id = :id"""
    )
    abstract suspend fun tentativa(id: Long): TentativaComNome?

    @Query(
        """SELECT e.area AS area, AVG(t.nota) AS media, COUNT(*) AS qtd
           FROM tentativas t JOIN especialidades e ON e.codigo = t.especialidadeCodigo
           WHERE t.usuarioId = :usuarioId GROUP BY e.area ORDER BY e.area"""
    )
    abstract fun mediaPorArea(usuarioId: Long): Flow<List<MediaArea>>

    @Query(
        """SELECT r.*, p.enunciado AS enunciado, p.opcoes AS opcoes,
           p.correta AS indiceCorreto, p.explicacao AS explicacao
           FROM respostas r JOIN perguntas p ON p.id = r.perguntaId
           WHERE r.tentativaId = :tentativaId ORDER BY r.id"""
    )
    abstract suspend fun respostas(tentativaId: Long): List<RespostaDetalhe>

    @Query(
        """SELECT u.id AS id, u.nome AS nome, u.classe AS classe,
           COUNT(t.id) AS tentativas, AVG(t.nota) AS media,
           COALESCE(SUM(CASE WHEN t.modo = 'PROVA' AND t.aprovado = 1 THEN 1 ELSE 0 END), 0) AS provasAprovadas
           FROM usuarios u LEFT JOIN tentativas t ON t.usuarioId = u.id
           WHERE u.papel = 'DESBRAVADOR' AND u.clube = :clube
           GROUP BY u.id ORDER BY u.nome"""
    )
    abstract fun turma(clube: String): Flow<List<DesbravadorResumo>>
}
