@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.telas

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dbv.estuda.data.AREAS
import com.dbv.estuda.data.Especialidade
import com.dbv.estuda.data.Pergunta
import com.dbv.estuda.data.ProvaResumo
import com.dbv.estuda.data.Repositorio
import com.dbv.estuda.data.UsuarioLogado
import com.dbv.estuda.data.nomeArea
import com.dbv.estuda.ui.comum.Barra
import com.dbv.estuda.ui.comum.BarraTopo
import com.dbv.estuda.ui.comum.CartaoNumero
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
import com.dbv.estuda.ui.theme.Linha
import com.dbv.estuda.ui.theme.Verde
import com.dbv.estuda.ui.theme.VerdeClaro
import com.dbv.estuda.ui.theme.Vermelho
import com.dbv.estuda.ui.theme.corDaNota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------- Painel do instrutor ----------------

@Composable
fun TelaInstrutor(
    usuario: UsuarioLogado,
    novaProva: () -> Unit,
    abrirProva: (Long) -> Unit,
    abrirAluno: (Long) -> Unit,
    abrirBanco: (String) -> Unit,
    sair: () -> Unit,
) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Provas", "Turma", "Perguntas")
    Scaffold(
        topBar = {
            BarraTopo("${titulos[aba]} · ${usuario.clube}") {
                IconButton(onClick = sair) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sair") }
            }
        },
        floatingActionButton = {
            if (aba == 0) ExtendedFloatingActionButton(onClick = novaProva, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nova prova") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(aba == 0, { aba = 0 }, { Icon(Icons.Filled.Star, null) }, label = { Text(titulos[0]) })
                NavigationBarItem(aba == 1, { aba = 1 }, { Icon(Icons.Filled.Person, null) }, label = { Text(titulos[1]) })
                NavigationBarItem(aba == 2, { aba = 2 }, { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(titulos[2]) })
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            when (aba) {
                0 -> AbaProvas(usuario, abrirProva)
                1 -> AbaTurma(usuario, abrirAluno)
                else -> ListaEspecialidades(aoAbrir = abrirBanco, cabecalho = { FerramentasBanco(usuario) })
            }
        }
    }
}

@Composable
private fun AbaProvas(usuario: UsuarioLogado, abrirProva: (Long) -> Unit) {
    val provas by container().repo.provas.provasDoClube(usuario.clube).collectAsState(initial = emptyList())
    LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (provas.isEmpty()) item { Vazio("Nenhuma prova criada. Toque em \"Nova prova\" para aplicar uma prova à turma.") }
        items(provas, key = { it.prova.id }) { p -> CartaoProva(p) { abrirProva(p.prova.id) } }
    }
}

@Composable
private fun CartaoProva(p: ProvaResumo, aoClicar: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Linha), modifier = Modifier.fillMaxWidth().clickable(onClick = aoClicar)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Insignia(p.prova.especialidadeCodigo, Repositorio.areaDoCodigo(p.prova.especialidadeCodigo), 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.prova.titulo, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${p.especialidadeNome} · ${fmtDataCurta(p.prova.criadaEm)}", style = MaterialTheme.typography.bodySmall, color = Cinza)
                }
                if (p.prova.aberta) Selo("ABERTA", VerdeClaro, Verde) else Selo("FECHADA", AmareloClaro, AmareloTexto)
            }
            Text("${p.qtdPerguntas} perguntas · mínimo ${p.prova.notaMinima}% · ${p.realizadas} entregue(s) · ${p.aprovados} aprovado(s) · média ${fmtNota(p.mediaNota)}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AbaTurma(usuario: UsuarioLogado, abrirAluno: (Long) -> Unit) {
    val turma by container().repo.desempenho.turma(usuario.clube).collectAsState(initial = emptyList())
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            val comNota = turma.mapNotNull { it.media }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartaoNumero(turma.size.toString(), "desbravadores", Modifier.weight(1f))
                val m = comNota.average().takeIf { comNota.isNotEmpty() }
                CartaoNumero(fmtNota(m), "média da turma", Modifier.weight(1f), cor = m?.let { corDaNota(it) } ?: Azul)
            }
        }
        item { Titulo("Toque para ver o desempenho individual") }
        if (turma.isEmpty()) item { Vazio("Nenhum desbravador cadastrado neste clube.") }
        items(turma, key = { it.id }) { d ->
            Row(Modifier.fillMaxWidth().clickable { abrirAluno(d.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.nome, fontWeight = FontWeight.Medium)
                    Text("${d.classe ?: "–"} · ${d.tentativas} tentativa(s) · ${d.provasAprovadas} prova(s) aprovada(s)",
                        style = MaterialTheme.typography.bodySmall, color = Cinza)
                }
                if (d.media != null) SeloNota(d.media) else Text("sem notas", color = Cinza, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}

/** Importar / exportar perguntas em JSON e criar especialidade. */
@Composable
private fun FerramentasBanco(usuario: UsuarioLogado) {
    val c = container()
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var dialogoNova by remember { mutableStateOf(false) }

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            val msg = try {
                val texto = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                val r = c.repo.importarJson(texto, usuario.id)
                "Importado: ${r.perguntasNovas} pergunta(s) nova(s) em ${r.especialidades} especialidade(s). ${r.ignoradas} ignorada(s)."
            } catch (e: Exception) {
                "Não foi possível importar: ${e.message}"
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) escopo.launch {
            val msg = try {
                val json = c.repo.exportarJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(json) } }
                "Banco de perguntas exportado."
            } catch (e: Exception) {
                "Falha ao exportar: ${e.message}"
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

    Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { dialogoNova = true }) { Text("Nova especialidade") }
        OutlinedButton(onClick = { importar.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Importar JSON") }
        OutlinedButton(onClick = { exportar.launch("perguntas-dbv.json") }) { Text("Exportar JSON") }
    }
    if (dialogoNova) DialogoNovaEspecialidade(fechar = { dialogoNova = false })
}

@Composable
private fun DialogoNovaEspecialidade(fechar: () -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    var codigo by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = fechar,
        title = { Text("Nova especialidade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(codigo, { codigo = it.uppercase() }, label = { Text("Código (ex.: AR-024)") }, singleLine = true)
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(descricao, { descricao = it }, label = { Text("Descrição (opcional)") })
                val area = Repositorio.areaDoCodigo(codigo.trim())
                Text(if (area in AREAS) "Área: ${nomeArea(area)}" else "Use um prefixo de área: ${AREAS.keys.joinToString()}",
                    style = MaterialTheme.typography.bodySmall, color = Cinza)
                erro?.let { Text(it, color = Vermelho) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cod = codigo.trim()
                val area = Repositorio.areaDoCodigo(cod)
                if (cod.length < 4 || area !in AREAS || nome.isBlank()) { erro = "Preencha código válido e nome."; return@TextButton }
                escopo.launch {
                    if (c.repo.conteudo.especialidade(cod) != null) { erro = "Esse código já existe."; return@launch }
                    c.repo.conteudo.salvarEspecialidade(Especialidade(cod, nome.trim(), area, 1, descricao.trim()))
                    fechar()
                }
            }) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = fechar) { Text("Cancelar") } },
    )
}

// ---------------- Nova prova ----------------

@Composable
fun TelaNovaProva(usuario: UsuarioLogado, aoVoltar: () -> Unit, aoCriar: (Long) -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    val especialidades by c.repo.conteudo.especialidades().collectAsState(initial = emptyList())
    val comPerguntas = especialidades.filter { it.qtdPerguntas > 0 }
    var codigo by rememberSaveable { mutableStateOf<String?>(null) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var qtd by rememberSaveable { mutableFloatStateOf(10f) }
    var minima by rememberSaveable { mutableFloatStateOf(70f) }
    var tempo by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val esp = comPerguntas.firstOrNull { it.codigo == codigo }
    val maxQtd = (esp?.qtdPerguntas ?: 1).coerceAtLeast(1)

    LaunchedEffect(esp?.codigo) {
        if (esp != null) {
            if (titulo.isBlank() || titulo.startsWith("Prova teórica – ")) titulo = "Prova teórica – ${esp.nome}"
            qtd = qtd.coerceAtMost(maxQtd.toFloat())
        }
    }

    Scaffold(topBar = { BarraTopo("Nova prova", aoVoltar) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Especialidade", fontWeight = FontWeight.SemiBold)
            Column {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(esp?.let { "${it.codigo} · ${it.nome} (${it.qtdPerguntas})" } ?: "Escolher especialidade")
                }
                DropdownMenu(menu, { menu = false }) {
                    comPerguntas.forEach { e ->
                        DropdownMenuItem(text = { Text("${e.codigo} · ${e.nome} (${e.qtdPerguntas})") }, onClick = { codigo = e.codigo; menu = false })
                    }
                }
            }
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título da prova") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Número de perguntas: ${qtd.toInt().coerceAtMost(maxQtd)} de $maxQtd", fontWeight = FontWeight.SemiBold)
            val topo = maxOf(2, maxQtd)
            Slider(qtd.coerceIn(1f, topo.toFloat()), { qtd = it }, valueRange = 1f..topo.toFloat(),
                steps = (topo - 2).coerceAtLeast(0), enabled = esp != null && maxQtd > 1)
            Text("Nota mínima para aprovação: ${minima.toInt()}%", fontWeight = FontWeight.SemiBold)
            Slider(minima, { minima = it }, valueRange = 50f..100f, steps = 9)
            Text("Tempo limite", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 15, 20, 30).forEach { t ->
                    FilterChip(tempo == t, { tempo = t }, label = { Text(if (t == 0) "Livre" else "$t min") })
                }
            }
            Surface(color = AzulClaro, shape = RoundedCornerShape(12.dp)) {
                Text("As perguntas são sorteadas do banco da especialidade e ficam fixas para toda a turma. Cada desbravador do clube \"${usuario.clube}\" pode entregar uma vez. A ordem das alternativas é embaralhada para cada um.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
            erro?.let { Text(it, color = Vermelho) }
            Button(
                onClick = {
                    val e = esp ?: run { erro = "Escolha a especialidade."; return@Button }
                    if (titulo.isBlank()) { erro = "Dê um título à prova."; return@Button }
                    escopo.launch {
                        val id = c.repo.criarProva(titulo, e.codigo, qtd.toInt().coerceAtMost(maxQtd), minima.toInt(), tempo, usuario)
                        aoCriar(id)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Criar e abrir para a turma") }
        }
    }
}

// ---------------- Detalhe da prova (resultados) ----------------

@Composable
fun TelaProvaDetalhe(provaId: Long, aoVoltar: () -> Unit, abrirResultado: (Long) -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    val prova by c.repo.provas.observarProva(provaId).collectAsState(initial = null)
    val resultados by c.repo.provas.resultadosDaProva(provaId).collectAsState(initial = emptyList())
    val acertos by c.repo.provas.acertosPorPergunta(provaId).collectAsState(initial = emptyList())
    var excluir by remember { mutableStateOf(false) }

    if (excluir) AlertDialog(
        onDismissRequest = { excluir = false },
        title = { Text("Excluir prova?") },
        text = { Text("A prova sai da lista. As notas já registradas continuam no histórico dos desbravadores.") },
        confirmButton = { TextButton(onClick = { escopo.launch { c.repo.provas.excluir(provaId); aoVoltar() } }) { Text("Excluir", color = Vermelho) } },
        dismissButton = { TextButton(onClick = { excluir = false }) { Text("Cancelar") } },
    )

    Scaffold(topBar = {
        BarraTopo(prova?.titulo ?: "Prova", aoVoltar) {
            IconButton(onClick = { excluir = true }) { Icon(Icons.Filled.Delete, "Excluir") }
        }
    }) { pad ->
        val p = prova ?: return@Scaffold
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (p.aberta) "Aberta para entregas" else "Fechada", fontWeight = FontWeight.Bold)
                        Text("Mínimo ${p.notaMinima}% · ${if (p.tempoLimiteMin > 0) "${p.tempoLimiteMin} min" else "sem limite de tempo"}",
                            color = Cinza, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(p.aberta, { escopo.launch { c.repo.provas.definirAberta(provaId, it) } })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CartaoNumero(resultados.size.toString(), "entregas", Modifier.weight(1f))
                    CartaoNumero(resultados.count { it.aprovado }.toString(), "aprovados", Modifier.weight(1f), cor = Verde)
                    val m = resultados.map { it.nota }.average().takeIf { resultados.isNotEmpty() }
                    CartaoNumero(fmtNota(m), "média", Modifier.weight(1f), cor = m?.let { corDaNota(it) } ?: Azul)
                }
            }
            item { Titulo("Notas") }
            if (resultados.isEmpty()) item { Vazio("Ninguém entregou ainda.") }
            items(resultados, key = { "r" + it.tentativaId }) { r ->
                Row(Modifier.fillMaxWidth().clickable { abrirResultado(r.tentativaId) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.nome, fontWeight = FontWeight.Medium)
                        Text("${r.acertos}/${r.total} · ${fmtData(r.fim)} · ${if (r.aprovado) "aprovado" else "reprovado"}",
                            style = MaterialTheme.typography.bodySmall, color = if (r.aprovado) Verde else Vermelho)
                    }
                    SeloNota(r.nota)
                }
            }
            item { Titulo("Acertos por pergunta") }
            itemsIndexed(acertos, key = { _, a -> "a" + a.perguntaId }) { i, a ->
                val frac = if (a.respondidas > 0) a.acertos.toFloat() / a.respondidas else 0f
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text("${i + 1}. ${a.enunciado}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Barra(frac, corDaNota(frac * 10.0), Modifier.weight(1f))
                        Text(if (a.respondidas > 0) "  ${(frac * 100).toInt()}%" else "  –", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Banco de perguntas ----------------

@Composable
fun TelaBancoPerguntas(codigo: String, aoVoltar: () -> Unit, editar: (String, Long) -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    val esp by c.repo.conteudo.observarEspecialidade(codigo).collectAsState(initial = null)
    val perguntas by c.repo.conteudo.perguntas(codigo).collectAsState(initial = emptyList())
    var excluir by remember { mutableStateOf<Pergunta?>(null) }

    excluir?.let { p ->
        AlertDialog(
            onDismissRequest = { excluir = null },
            title = { Text("Excluir pergunta?") },
            text = { Text(p.enunciado) },
            confirmButton = { TextButton(onClick = { escopo.launch { c.repo.conteudo.desativarPergunta(p.id) }; excluir = null }) { Text("Excluir", color = Vermelho) } },
            dismissButton = { TextButton(onClick = { excluir = null }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = { BarraTopo(esp?.nome ?: codigo, aoVoltar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editar(codigo, 0L) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Pergunta") })
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("${perguntas.size} pergunta(s) ativa(s)", color = Cinza) }
            if (perguntas.isEmpty()) item { Vazio("Nenhuma pergunta. Toque em \"Pergunta\" para adicionar ou importe um arquivo JSON.") }
            itemsIndexed(perguntas, key = { _, p -> p.id }) { i, p ->
                Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Linha), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("${i + 1}. ${p.enunciado}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(top = 10.dp))
                            IconButton(onClick = { editar(codigo, p.id) }) { Icon(Icons.Filled.Edit, "Editar", tint = Azul) }
                            IconButton(onClick = { excluir = p }) { Icon(Icons.Filled.Delete, "Excluir", tint = Vermelho) }
                        }
                        p.opcoes.forEachIndexed { j, o ->
                            Text("${'A' + j}) $o", color = if (j == p.correta) Verde else Cinza,
                                fontWeight = if (j == p.correta) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaEditorPergunta(codigo: String, perguntaId: Long, autorId: Long, aoVoltar: () -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    var carregada by remember { mutableStateOf(perguntaId == 0L) }
    var original by remember { mutableStateOf<Pergunta?>(null) }
    var enunciado by rememberSaveable { mutableStateOf("") }
    val opcoes = remember { mutableStateListOf("", "", "", "") }
    var correta by rememberSaveable { mutableIntStateOf(0) }
    var explicacao by rememberSaveable { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(perguntaId) {
        if (perguntaId != 0L && !carregada) {
            c.repo.conteudo.pergunta(perguntaId)?.let { p ->
                original = p
                enunciado = p.enunciado
                opcoes.clear(); opcoes.addAll(p.opcoes); while (opcoes.size < 4) opcoes.add("")
                correta = p.correta
                explicacao = p.explicacao
            }
            carregada = true
        }
    }

    Scaffold(topBar = { BarraTopo(if (perguntaId == 0L) "Nova pergunta" else "Editar pergunta", aoVoltar) }) { pad ->
        if (!carregada) return@Scaffold
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(enunciado, { enunciado = it }, label = { Text("Enunciado") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Text("Alternativas (marque a correta). Deixe em branco as que não usar.", color = Cinza, style = MaterialTheme.typography.bodySmall)
            opcoes.forEachIndexed { i, o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = correta == i, onClick = { correta = i })
                    OutlinedTextField(o, { opcoes[i] = it }, label = { Text("Alternativa ${'A' + i}") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            if (opcoes.size < 6) TextButton(onClick = { opcoes.add("") }) { Text("+ alternativa") }
            OutlinedTextField(explicacao, { explicacao = it }, label = { Text("Explicação (aparece no treino e na revisão)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            erro?.let { Text(it, color = Vermelho) }
            Button(onClick = {
                // remove alternativas vazias mantendo a correta apontando para o texto certo
                val textoCorreto = opcoes.getOrNull(correta)?.trim().orEmpty()
                val validas = opcoes.map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    enunciado.isBlank() -> erro = "Escreva o enunciado."
                    validas.size < 2 -> erro = "Informe pelo menos 2 alternativas."
                    textoCorreto.isEmpty() -> erro = "A alternativa marcada como correta está vazia."
                    validas.toSet().size != validas.size -> erro = "Há alternativas repetidas."
                    else -> escopo.launch {
                        val idx = validas.indexOf(textoCorreto)
                        val o = original
                        if (o == null) {
                            c.repo.conteudo.inserirPergunta(Pergunta(especialidadeCodigo = codigo, enunciado = enunciado.trim(),
                                opcoes = validas, correta = idx, explicacao = explicacao.trim(), criadaPor = autorId))
                        } else {
                            c.repo.editarPergunta(o.copy(enunciado = enunciado.trim(), opcoes = validas,
                                correta = idx, explicacao = explicacao.trim()))
                        }
                        aoVoltar()
                    }
                }
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Salvar") }
            if (original != null) Text("Se a pergunta já foi respondida, é salva uma nova versão e a antiga continua nas revisões já feitas.",
                color = Cinza, style = MaterialTheme.typography.bodySmall)
        }
    }
}
