package com.dbv.estuda.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.json.JSONArray

// ---------- Constantes ----------

object Papel {
    const val DESBRAVADOR = "DESBRAVADOR"
    const val INSTRUTOR = "INSTRUTOR"
}

object Modo {
    const val TREINO = "TREINO"
    const val PROVA = "PROVA"
}

val CLASSES = listOf("Amigo", "Companheiro", "Pesquisador", "Pioneiro", "Excursionista", "Guia")

/** Nome de cada área, pelo prefixo do código da especialidade. */
val AREAS = linkedMapOf(
    "ID" to "Ideais e Classes",
    "AD" to "ADRA",
    "HM" to "Artes e Habilidades Manuais",
    "AA" to "Atividades Agrícolas",
    "AM" to "Atividades Missionárias",
    "AM-EB" to "Estudos Bíblicos",
    "AP" to "Atividades Profissionais",
    "AR" to "Atividades Recreativas",
    "CS" to "Ciência e Saúde",
    "EN" to "Estudo da Natureza",
    "HD" to "Habilidades Domésticas",
    "ME" to "Mestrados",
)

fun nomeArea(area: String) = AREAS[area] ?: area

// ---------- Tabelas ----------

@Entity(tableName = "usuarios", indices = [Index(value = ["nome"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val senhaHash: String,
    val papel: String,
    val clube: String,
    val classe: String? = null,
    val criadoEm: Long = System.currentTimeMillis(),
)

@Entity(tableName = "especialidades")
data class Especialidade(
    @PrimaryKey val codigo: String,
    val nome: String,
    val area: String,
    val nivel: Int = 1,
    val descricao: String = "",
)

@Entity(
    tableName = "perguntas",
    foreignKeys = [ForeignKey(
        entity = Especialidade::class,
        parentColumns = ["codigo"],
        childColumns = ["especialidadeCodigo"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("especialidadeCodigo")],
)
data class Pergunta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val especialidadeCodigo: String,
    val enunciado: String,
    val opcoes: List<String>,
    /** Índice (base 0) da opção correta dentro de [opcoes]. */
    val correta: Int,
    val explicacao: String = "",
    /** Perguntas excluídas ficam inativas para não quebrar o histórico. */
    val ativa: Boolean = true,
    val criadaPor: Long? = null,
)

@Entity(
    tableName = "provas",
    foreignKeys = [ForeignKey(
        entity = Especialidade::class,
        parentColumns = ["codigo"],
        childColumns = ["especialidadeCodigo"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("especialidadeCodigo"), Index("clube")],
)
data class Prova(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val especialidadeCodigo: String,
    val instrutorId: Long,
    val clube: String,
    /** Percentual mínimo de acertos para aprovação (0–100). */
    val notaMinima: Int = 70,
    /** 0 = sem limite de tempo. */
    val tempoLimiteMin: Int = 0,
    val aberta: Boolean = true,
    val criadaEm: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "prova_perguntas",
    primaryKeys = ["provaId", "perguntaId"],
    foreignKeys = [ForeignKey(
        entity = Prova::class,
        parentColumns = ["id"],
        childColumns = ["provaId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("perguntaId")],
)
data class ProvaPergunta(
    val provaId: Long,
    val perguntaId: Long,
    val ordem: Int,
)

@Entity(
    tableName = "tentativas",
    foreignKeys = [ForeignKey(
        entity = Usuario::class,
        parentColumns = ["id"],
        childColumns = ["usuarioId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("usuarioId"), Index("provaId"), Index("especialidadeCodigo")],
)
data class Tentativa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val especialidadeCodigo: String,
    val provaId: Long?,
    val modo: String,
    val inicio: Long,
    val fim: Long,
    val acertos: Int,
    val total: Int,
    /** Nota de 0 a 10. */
    val nota: Double,
    val aprovado: Boolean,
)

@Entity(
    tableName = "respostas",
    foreignKeys = [ForeignKey(
        entity = Tentativa::class,
        parentColumns = ["id"],
        childColumns = ["tentativaId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tentativaId"), Index("perguntaId")],
)
data class Resposta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tentativaId: Long,
    val perguntaId: Long,
    /** Índice original escolhido; -1 = em branco. */
    val escolhida: Int,
    val correta: Boolean,
)

// ---------- Resultados de consultas ----------

data class EspecialidadeResumo(
    val codigo: String,
    val nome: String,
    val area: String,
    val nivel: Int,
    val descricao: String,
    val qtdPerguntas: Int,
)

data class TentativaComNome(
    @Embedded val tentativa: Tentativa,
    val especialidadeNome: String,
    val provaTitulo: String?,
)

data class MediaArea(val area: String, val media: Double, val qtd: Int)

data class DesbravadorResumo(
    val id: Long,
    val nome: String,
    val classe: String?,
    val tentativas: Int,
    val media: Double?,
    val provasAprovadas: Int,
)

data class ProvaResumo(
    @Embedded val prova: Prova,
    val especialidadeNome: String,
    val qtdPerguntas: Int,
    val realizadas: Int,
    val aprovados: Int,
    val mediaNota: Double?,
)

data class ProvaParaAluno(
    @Embedded val prova: Prova,
    val especialidadeNome: String,
    val qtdPerguntas: Int,
    val minhaNota: Double?,
    val minhaTentativaId: Long?,
)

data class ResultadoAluno(
    val tentativaId: Long,
    val nome: String,
    val nota: Double,
    val acertos: Int,
    val total: Int,
    val aprovado: Boolean,
    val fim: Long,
)

data class AcertoPergunta(
    val perguntaId: Long,
    val enunciado: String,
    val respondidas: Int,
    val acertos: Int,
)

data class RespostaDetalhe(
    @Embedded val resposta: Resposta,
    val enunciado: String,
    val opcoes: List<String>,
    val indiceCorreto: Int,
    val explicacao: String,
)

// ---------- Conversores ----------

class Conversores {
    @TypeConverter
    fun deLista(lista: List<String>): String = JSONArray(lista).toString()

    @TypeConverter
    fun paraLista(texto: String): List<String> {
        val arr = JSONArray(texto)
        return List(arr.length()) { arr.getString(it) }
    }
}
