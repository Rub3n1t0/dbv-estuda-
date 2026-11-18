@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dbv.estuda.data.nomeArea
import com.dbv.estuda.ui.comum.BarraTopo
import com.dbv.estuda.ui.comum.Carregando
import com.dbv.estuda.ui.comum.Insignia
import com.dbv.estuda.ui.comum.Titulo
import com.dbv.estuda.ui.comum.Vazio
import com.dbv.estuda.ui.comum.container
import com.dbv.estuda.ui.comum.fmtNota
import com.dbv.estuda.ui.theme.AzulClaro
import com.dbv.estuda.ui.theme.Cinza
import com.dbv.estuda.ui.theme.corDaNota
import java.net.URLEncoder

/** Detalhe da especialidade para o desbravador: descrição, link dos requisitos e treino. */
@Composable
fun TelaEspecialidade(codigo: String, aoVoltar: () -> Unit, treinar: (String, Int) -> Unit, abrirResultado: (Long) -> Unit) {
    val c = container()
    val usuario = c.sessao.atual.collectAsState().value
    val esp by c.repo.conteudo.observarEspecialidade(codigo).collectAsState(initial = null)
    val perguntas by c.repo.conteudo.perguntas(codigo).collectAsState(initial = emptyList())
    val historico by c.repo.desempenho.historico(usuario?.id ?: -1).collectAsState(initial = emptyList())
    val minhas = historico.filter { it.tentativa.especialidadeCodigo == codigo }
    var qtd by rememberSaveable { mutableIntStateOf(10) }
    val uri = LocalUriHandler.current

    Scaffold(topBar = { BarraTopo(esp?.nome ?: "Especialidade", aoVoltar) }) { pad ->
        val e = esp ?: run { Column(Modifier.padding(pad)) { Carregando() }; return@Scaffold }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Insignia(e.codigo, e.area, 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(e.nome, style = MaterialTheme.typography.headlineSmall)
                        Text("${e.codigo} · ${nomeArea(e.area)} · nível ${e.nivel}", color = Cinza)
                    }
                }
            }
            if (e.descricao.isNotBlank()) item { Text(e.descricao) }
            item {
                OutlinedButton(onClick = {
                    uri.openUri("https://mda.wiki.br/index.php?search=" + URLEncoder.encode("Especialidade de ${e.nome}", "UTF-8"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Ver requisitos oficiais no MDA Wiki") }
            }
            item {
                Surface(color = AzulClaro, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Treinar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${perguntas.size} perguntas no banco. No treino você vê a resposta certa e a explicação depois de cada pergunta.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 20, 0).forEach { n ->
                                FilterChip(qtd == n, { qtd = n }, label = { Text(if (n == 0) "Todas" else "$n") })
                            }
                        }
                        Button(onClick = { treinar(codigo, qtd) }, enabled = perguntas.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Começar treino") }
                    }
                }
            }
            item { Titulo("Minhas tentativas nesta especialidade") }
            if (minhas.isEmpty()) item { Vazio("Nenhuma ainda.") }
            else item {
                val melhor = minhas.maxOf { it.tentativa.nota }
                Text("Melhor nota: ${fmtNota(melhor)}", color = corDaNota(melhor), fontWeight = FontWeight.Bold)
            }
            items(minhas, key = { it.tentativa.id }) { t -> LinhaTentativa(t) { abrirResultado(t.tentativa.id) } }
        }
    }
}
