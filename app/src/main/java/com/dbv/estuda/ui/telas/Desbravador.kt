@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbv.estuda.data.AREAS
import com.dbv.estuda.data.EspecialidadeResumo
import com.dbv.estuda.data.Modo
import com.dbv.estuda.data.ProvaParaAluno
import com.dbv.estuda.data.Repositorio
import com.dbv.estuda.data.TentativaComNome
import com.dbv.estuda.data.UsuarioLogado
import com.dbv.estuda.data.nomeArea
import com.dbv.estuda.ui.comum.BarraTopo
import com.dbv.estuda.ui.comum.CartaoNumero
import com.dbv.estuda.ui.comum.GraficoBarras
import com.dbv.estuda.ui.comum.GraficoLinha
import com.dbv.estuda.ui.comum.Insignia
import com.dbv.estuda.ui.comum.Selo
import com.dbv.estuda.ui.comum.SeloNota
import com.dbv.estuda.ui.comum.Titulo
import com.dbv.estuda.ui.comum.Vazio
import com.dbv.estuda.ui.comum.container
import com.dbv.estuda.ui.comum.fmtData
import com.dbv.estuda.ui.comum.fmtDataCurta
import com.dbv.estuda.ui.comum.fmtNota
import com.dbv.estuda.ui.theme.AmareloClaro
import com.dbv.estuda.ui.theme.AmareloTexto
import com.dbv.estuda.ui.theme.Azul
import com.dbv.estuda.ui.theme.AzulClaro
import com.dbv.estuda.ui.theme.Cinza
import com.dbv.estuda.ui.theme.Verde
import com.dbv.estuda.ui.theme.VerdeClaro
import com.dbv.estuda.ui.theme.corDaArea
import com.dbv.estuda.ui.theme.corDaNota

/** Tela principal do desbravador, com três abas. */
@Composable
fun TelaDesbravador(
    usuario: UsuarioLogado,
    abrirEspecialidade: (String) -> Unit,
    abrirProva: (Long) -> Unit,
    abrirResultado: (Long) -> Unit,
    sair: () -> Unit,
) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Início", "Especialidades", "Desempenho")
    Scaffold(
        topBar = {
            BarraTopo(if (aba == 0) "Olá, ${usuario.nome.substringBefore(' ')}" else titulos[aba]) {
                IconButton(onClick = sair) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sair") }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(aba == 0, { aba = 0 }, { Icon(Icons.Filled.Home, null) }, label = { Text(titulos[0]) })
                NavigationBarItem(aba == 1, { aba = 1 }, { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(titulos[1]) })
                NavigationBarItem(aba == 2, { aba = 2 }, { Icon(Icons.Filled.Star, null) }, label = { Text(titulos[2]) })
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            when (aba) {
                0 -> InicioDesbravador(usuario, abrirProva, abrirResultado, irParaEspecialidades = { aba = 1 })
                1 -> ListaEspecialidades(aoAbrir = abrirEspecialidade)
                else -> ConteudoDesempenho(usuario.id, abrirResultado)
            }
        }
    }
}

@Composable
private fun InicioDesbravador(
    usuario: UsuarioLogado,
    abrirProva: (Long) -> Unit,
    abrirResultado: (Long) -> Unit,
    irParaEspecialidades: () -> Unit,
) {
    val repo = container().repo
    val provas by repo.provas.provasDoAluno(usuario.clube, usuario.id).collectAsState(initial = emptyList())
    val historico by repo.desempenho.historico(usuario.id).collectAsState(initial = emptyList())
    val media = historico.map { it.tentativa.nota }.average().takeIf { historico.isNotEmpty() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoNumero(fmtNota(media), "média geral", Modifier.weight(1f), cor = media?.let { corDaNota(it) } ?: Azul)
                CartaoNumero(historico.size.toString(), "tentativas", Modifier.weight(1f))
                CartaoNumero(historico.count { it.tentativa.modo == Modo.PROVA && it.tentativa.aprovado }.toString(),
                    "provas aprovadas", Modifier.weight(1f), cor = Verde)
            }
        }
        item { Titulo("Provas do clube") }
        if (provas.isEmpty()) item { Vazio("Nenhuma prova aberta no momento. Enquanto isso, treine nas especialidades!") }
        items(provas, key = { "p" + it.prova.id }) { p -> CartaoProvaAluno(p, abrirProva, abrirResultado) }
        item {
            Spacer(Modifier.height(4.dp))
            Button(onClick = irParaEspecialidades, modifier = Modifier.fillMaxWidth()) { Text("Treinar uma especialidade") }
        }
        item { Titulo("Últimas atividades") }
        if (historico.isEmpty()) item { Vazio("Você ainda não respondeu nenhum teste.") }
        items(historico.take(5), key = { "t" + it.tentativa.id }) { t -> LinhaTentativa(t) { abrirResultado(t.tentativa.id) } }
    }
}

@Composable
private fun CartaoProvaAluno(p: ProvaParaAluno, abrirProva: (Long) -> Unit, abrirResultado: (Long) -> Unit) {
    val feita = p.minhaTentativaId != null
    Surface(
        color = if (feita) MaterialTheme.colorScheme.surfaceVariant else AmareloClaro,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            if (feita) abrirResultado(p.minhaTentativaId!!) else abrirProva(p.prova.id)
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Insignia(p.prova.especialidadeCodigo, Repositorio.areaDoCodigo(p.prova.especialidadeCodigo), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.prova.titulo, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val tempo = if (p.prova.tempoLimiteMin > 0) " · ${p.prova.tempoLimiteMin} min" else ""
                Text("${p.qtdPerguntas} perguntas · mínimo ${p.prova.notaMinima}%$tempo",
                    style = MaterialTheme.typography.bodySmall, color = Cinza)
            }
            if (feita) SeloNota(p.minhaNota ?: 0.0)
            else Selo("FAZER", Azul, Color.White)
        }
    }
}

@Composable
fun LinhaTentativa(t: TentativaComNome, aoClicar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = aoClicar).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Insignia(t.tentativa.especialidadeCodigo, Repositorio.areaDoCodigo(t.tentativa.especialidadeCodigo), 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.provaTitulo ?: t.especialidadeNome, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (t.tentativa.modo == Modo.PROVA) Selo("PROVA", AzulClaro, Azul) else Selo("TREINO", VerdeClaro, Verde)
                Text("  ${t.tentativa.acertos}/${t.tentativa.total} · ${fmtData(t.tentativa.fim)}",
                    style = MaterialTheme.typography.bodySmall, color = Cinza)
            }
        }
        SeloNota(t.tentativa.nota)
    }
}

/** Catálogo com busca e filtro por área. Reaproveitado pelo instrutor. */
@Composable
fun ListaEspecialidades(aoAbrir: (String) -> Unit, cabecalho: @Composable () -> Unit = {}) {
    val repo = container().repo
    val todas by repo.conteudo.especialidades().collectAsState(initial = emptyList())
    var busca by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("TODAS") }
    val areasPresentes = remember(todas) { todas.map { it.area }.distinct() }
    val chave = Repositorio.normalizar(busca)
    val lista = todas.filter { (area == "TODAS" || it.area == area) &&
        (chave.isBlank() || Repositorio.normalizar(it.nome).contains(chave) || it.codigo.lowercase().contains(chave)) }

    Column(Modifier.fillMaxSize()) {
        cabecalho()
        OutlinedTextField(
            busca, { busca = it }, placeholder = { Text("Buscar por nome ou código") },
            leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(area == "TODAS", { area = "TODAS" }, label = { Text("Todas") })
            AREAS.keys.filter { it in areasPresentes }.forEach { a ->
                FilterChip(area == a, { area = a }, label = { Text(nomeArea(a)) })
            }
        }
        Text("${lista.size} especialidade(s)", color = Cinza, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (lista.isEmpty()) item { Vazio("Nenhuma especialidade encontrada.") }
            items(lista, key = { it.codigo }) { e -> LinhaEspecialidade(e) { aoAbrir(e.codigo) } }
        }
    }
}

@Composable
private fun LinhaEspecialidade(e: EspecialidadeResumo, aoClicar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = aoClicar).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Insignia(e.codigo, e.area)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.nome, fontWeight = FontWeight.Medium)
            Text("${e.codigo} · ${nomeArea(e.area)} · nível ${e.nivel}", style = MaterialTheme.typography.bodySmall, color = Cinza)
        }
        Selo("${e.qtdPerguntas} perg.", if (e.qtdPerguntas > 0) AzulClaro else AmareloClaro,
            if (e.qtdPerguntas > 0) Azul else AmareloTexto)
    }
}

/** Painel de desempenho de um desbravador. Usado pelo próprio desbravador e pelo instrutor. */
@Composable
fun ConteudoDesempenho(usuarioId: Long, abrirResultado: (Long) -> Unit) {
    val repo = container().repo
    val historico by repo.desempenho.historico(usuarioId).collectAsState(initial = emptyList())
    val porArea by repo.desempenho.mediaPorArea(usuarioId).collectAsState(initial = emptyList())
    val provas = historico.filter { it.tentativa.modo == Modo.PROVA }
    val treinos = historico.filter { it.tentativa.modo == Modo.TREINO }
    // melhor nota por especialidade
    val porEspecialidade = historico.groupBy { it.tentativa.especialidadeCodigo }
        .map { (_, l) -> l.maxBy { it.tentativa.nota } }.sortedByDescending { it.tentativa.nota }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val m = historico.map { it.tentativa.nota }.average().takeIf { historico.isNotEmpty() }
                CartaoNumero(fmtNota(m), "média geral", Modifier.weight(1f), cor = m?.let { corDaNota(it) } ?: Azul)
                val acertos = historico.sumOf { it.tentativa.acertos }
                val total = historico.sumOf { it.tentativa.total }
                CartaoNumero(if (total > 0) "${acertos * 100 / total}%" else "–", "de acertos", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoNumero("${provas.count { it.tentativa.aprovado }}/${provas.size}", "provas aprovadas", Modifier.weight(1f), cor = Verde)
                CartaoNumero(treinos.size.toString(), "treinos", Modifier.weight(1f))
            }
        }
        item { Titulo("Evolução (últimas 12)") }
        item { GraficoLinha(historico.take(12).reversed().map { it.tentativa.nota }) }
        item { Titulo("Média por área") }
        if (porArea.isEmpty()) item { Vazio("Sem dados ainda.") }
        else item { GraficoBarras(porArea.map { Triple(nomeArea(it.area), it.media, corDaArea(it.area)) }) }
        item { Titulo("Melhor nota por especialidade") }
        items(porEspecialidade, key = { "m" + it.tentativa.id }) { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t.especialidadeNome, Modifier.weight(1f))
                Text(fmtDataCurta(t.tentativa.fim) + "  ", color = Cinza, style = MaterialTheme.typography.bodySmall)
                SeloNota(t.tentativa.nota)
            }
        }
        item { Titulo("Histórico completo") }
        if (historico.isEmpty()) item { Vazio("Nenhuma tentativa registrada.") }
        items(historico, key = { "h" + it.tentativa.id }) { t -> LinhaTentativa(t) { abrirResultado(t.tentativa.id) } }
    }
}

/** Desempenho de um desbravador visto pelo instrutor. */
@Composable
fun TelaDesempenhoAluno(usuarioId: Long, aoVoltar: () -> Unit, abrirResultado: (Long) -> Unit) {
    val u by container().repo.usuarios.observar(usuarioId).collectAsState(initial = null)
    Scaffold(topBar = { BarraTopo(u?.nome ?: "Desempenho", aoVoltar) }) { pad ->
        Column(Modifier.padding(pad)) {
            u?.classe?.let {
                Text("Classe: $it · ${u?.clube}", color = Cinza, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
            }
            ConteudoDesempenho(usuarioId, abrirResultado)
        }
    }
}
