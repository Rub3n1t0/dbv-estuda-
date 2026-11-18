@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbv.estuda.data.CLASSES
import com.dbv.estuda.data.Papel
import com.dbv.estuda.data.Usuario
import com.dbv.estuda.ui.comum.BarraTopo
import com.dbv.estuda.ui.comum.container
import com.dbv.estuda.ui.theme.Amarelo
import com.dbv.estuda.ui.theme.Azul
import com.dbv.estuda.ui.theme.Cinza
import com.dbv.estuda.ui.theme.Vermelho
import kotlinx.coroutines.launch

@Composable
fun TelaLogin(aoEntrar: (Usuario) -> Unit, aoCadastrar: () -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    var nome by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    fun entrar(n: String = nome, s: String = senha) {
        escopo.launch {
            val u = c.repo.entrar(n, s)
            if (u == null) erro = "Nome ou senha incorretos." else { c.sessao.entrar(u); aoEntrar(u) }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Azul).verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(buildAnnotatedString {
            append("DBV Estuda")
            withStyle(SpanStyle(color = Amarelo)) { append("+") }
        }, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("Perguntas, provas e desempenho nas especialidades do Clube de Desbravadores.",
            color = Color(0xFFC9D6EA))
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Entrar", style = MaterialTheme.typography.titleLarge, color = Azul)
                OutlinedTextField(nome, { nome = it; erro = null }, label = { Text("Nome") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(senha, { senha = it; erro = null }, label = { Text("Senha") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth())
                erro?.let { Text(it, color = Vermelho) }
                Button(onClick = { entrar() }, enabled = nome.isNotBlank() && senha.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Entrar") }
                TextButton(onClick = aoCadastrar, modifier = Modifier.fillMaxWidth()) { Text("Criar uma conta") }
            }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF14365A)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Contas de demonstração", color = Amarelo, fontWeight = FontWeight.Bold)
                listOf(
                    Triple("Ana Souza", "aguias2026", "Desbravadora"),
                    Triple("Carlos Lima", "instrutor26", "Instrutor"),
                ).forEach { (n, s, p) ->
                    OutlinedButton(onClick = { nome = n; senha = s; entrar(n, s) }, modifier = Modifier.fillMaxWidth()) {
                        Text("$p · $n", color = Color.White)
                    }
                }
                Text("Outros desbravadores: Pedro Henrique, Marina Costa, Júlia Santos (senha dbv2026).",
                    color = Color(0xFFC9D6EA), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TelaCadastro(aoVoltar: () -> Unit, aoCriar: (Usuario) -> Unit) {
    val c = container()
    val escopo = rememberCoroutineScope()
    val clubes by c.repo.usuarios.clubes().collectAsState(initial = emptyList())
    var nome by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var papel by rememberSaveable { mutableStateOf(Papel.DESBRAVADOR) }
    var clube by rememberSaveable { mutableStateOf("") }
    var classe by rememberSaveable { mutableStateOf(CLASSES.first()) }
    var menuClasse by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BarraTopo("Criar conta", aoVoltar) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Você é:", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(papel == Papel.DESBRAVADOR, { papel = Papel.DESBRAVADOR }, label = { Text("Desbravador(a)") })
                FilterChip(papel == Papel.INSTRUTOR, { papel = Papel.INSTRUTOR }, label = { Text("Instrutor(a)") })
            }
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(senha, { senha = it }, label = { Text("Senha (mín. 4 caracteres)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(clube, { clube = it }, label = { Text("Clube") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (clubes.isNotEmpty()) {
                Text("Clubes já cadastrados:", color = Cinza, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    clubes.forEach { cl -> FilterChip(clube == cl, { clube = cl }, label = { Text(cl) }) }
                }
            }
            if (papel == Papel.DESBRAVADOR) {
                Text("Classe atual", fontWeight = FontWeight.SemiBold)
                Column {
                    OutlinedButton(onClick = { menuClasse = true }, modifier = Modifier.fillMaxWidth()) { Text(classe) }
                    DropdownMenu(menuClasse, { menuClasse = false }) {
                        CLASSES.forEach { cl -> DropdownMenuItem(text = { Text(cl) }, onClick = { classe = cl; menuClasse = false }) }
                    }
                }
            }
            erro?.let { Text(it, color = Vermelho) }
            Button(
                onClick = {
                    escopo.launch {
                        try {
                            val u = c.repo.cadastrar(nome, senha, papel, clube, classe)
                            c.sessao.entrar(u)
                            aoCriar(u)
                        } catch (e: IllegalArgumentException) {
                            erro = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Criar conta") }
            Text("Os dados ficam salvos neste aparelho.", color = Cinza, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
