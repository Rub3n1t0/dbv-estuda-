package com.dbv.estuda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dbv.estuda.data.Modo
import com.dbv.estuda.data.Papel
import com.dbv.estuda.ui.comum.container
import com.dbv.estuda.ui.telas.TelaBancoPerguntas
import com.dbv.estuda.ui.telas.TelaCadastro
import com.dbv.estuda.ui.telas.TelaDesbravador
import com.dbv.estuda.ui.telas.TelaDesempenhoAluno
import com.dbv.estuda.ui.telas.TelaEditorPergunta
import com.dbv.estuda.ui.telas.TelaEspecialidade
import com.dbv.estuda.ui.telas.TelaInstrutor
import com.dbv.estuda.ui.telas.TelaLogin
import com.dbv.estuda.ui.telas.TelaNovaProva
import com.dbv.estuda.ui.telas.TelaProvaDetalhe
import com.dbv.estuda.ui.telas.TelaQuiz
import com.dbv.estuda.ui.telas.TelaResultado
import com.dbv.estuda.ui.theme.DbvTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DbvTema {
                Surface { Navegacao() }
            }
        }
    }
}

private fun rotaInicial(papel: String?) = when (papel) {
    null -> "login"
    Papel.INSTRUTOR -> "instrutor"
    else -> "desbravador"
}

/** Vai para a tela inicial do papel, limpando a pilha de navegação. */
private fun NavHostController.irParaInicio(papel: String?) {
    navigate(rotaInicial(papel)) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun Navegacao() {
    val c = container()
    val nav = rememberNavController()
    // Calculados uma vez só: o grafo de navegação não pode mudar a cada recomposição.
    val inicio = remember { rotaInicial(c.sessao.atual.value?.papel) }
    val sair: () -> Unit = remember(nav) { { c.sessao.sair(); nav.irParaInicio(null) } }

    NavHost(nav, startDestination = inicio) {
        composable("login") {
            TelaLogin(aoEntrar = { nav.irParaInicio(it.papel) }, aoCadastrar = { nav.navigate("cadastro") })
        }
        composable("cadastro") {
            TelaCadastro(aoVoltar = { nav.popBackStack() }, aoCriar = { nav.irParaInicio(it.papel) })
        }

        // ----- Desbravador -----
        composable("desbravador") {
            val u = c.sessao.atual.collectAsState().value ?: return@composable
            TelaDesbravador(
                usuario = u,
                abrirEspecialidade = { nav.navigate("especialidade/$it") },
                abrirProva = { nav.navigate("quiz/${Modo.PROVA}?prova=$it") },
                abrirResultado = { nav.navigate("resultado/$it") },
                sair = sair,
            )
        }
        composable("especialidade/{codigo}") { e ->
            val codigo = e.arguments?.getString("codigo") ?: return@composable
            TelaEspecialidade(
                codigo = codigo,
                aoVoltar = { nav.popBackStack() },
                treinar = { cod, qtd -> nav.navigate("quiz/${Modo.TREINO}?codigo=$cod&qtd=$qtd") },
                abrirResultado = { nav.navigate("resultado/$it") },
            )
        }
        composable(
            "quiz/{modo}?codigo={codigo}&prova={prova}&qtd={qtd}",
            arguments = listOf(
                navArgument("modo") { type = NavType.StringType },
                navArgument("codigo") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("prova") { type = NavType.LongType; defaultValue = -1L },
                navArgument("qtd") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { e ->
            val a = e.arguments ?: return@composable
            TelaQuiz(
                modo = a.getString("modo") ?: Modo.TREINO,
                codigo = a.getString("codigo"),
                provaId = a.getLong("prova").takeIf { it > 0 },
                quantidade = a.getInt("qtd"),
                aoSair = { nav.popBackStack() },
                aoTerminar = { id ->
                    nav.navigate("resultado/$id") {
                        popUpTo(e.destination.id) { inclusive = true }
                    }
                },
            )
        }
        composable("resultado/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TelaResultado(
                tentativaId = e.arguments?.getLong("id") ?: 0L,
                aoVoltar = { nav.popBackStack() },
                treinarDeNovo = { cod ->
                    nav.navigate("quiz/${Modo.TREINO}?codigo=$cod&qtd=10") {
                        popUpTo(e.destination.id) { inclusive = true }
                    }
                },
            )
        }

        // ----- Instrutor -----
        composable("instrutor") {
            val u = c.sessao.atual.collectAsState().value ?: return@composable
            TelaInstrutor(
                usuario = u,
                novaProva = { nav.navigate("novaProva") },
                abrirProva = { nav.navigate("prova/$it") },
                abrirAluno = { nav.navigate("aluno/$it") },
                abrirBanco = { nav.navigate("banco/$it") },
                sair = sair,
            )
        }
        composable("novaProva") {
            val u = c.sessao.atual.collectAsState().value ?: return@composable
            TelaNovaProva(u, aoVoltar = { nav.popBackStack() }, aoCriar = { id ->
                nav.navigate("prova/$id") { popUpTo("instrutor") }
            })
        }
        composable("prova/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TelaProvaDetalhe(e.arguments?.getLong("id") ?: 0L, aoVoltar = { nav.popBackStack() },
                abrirResultado = { nav.navigate("resultado/$it") })
        }
        composable("aluno/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TelaDesempenhoAluno(e.arguments?.getLong("id") ?: 0L, aoVoltar = { nav.popBackStack() },
                abrirResultado = { nav.navigate("resultado/$it") })
        }
        composable("banco/{codigo}") { e ->
            val codigo = e.arguments?.getString("codigo") ?: return@composable
            TelaBancoPerguntas(codigo, aoVoltar = { nav.popBackStack() },
                editar = { cod, id -> nav.navigate("pergunta/$cod/$id") })
        }
        composable("pergunta/{codigo}/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            val codigo = e.arguments?.getString("codigo") ?: return@composable
            TelaEditorPergunta(codigo, e.arguments?.getLong("id") ?: 0L, autorId = c.sessao.atual.value?.id ?: 0L,
                aoVoltar = { nav.popBackStack() })
        }
    }
}
