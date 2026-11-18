@file:OptIn(ExperimentalMaterial3Api::class)

package com.dbv.estuda.ui.comum

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dbv.estuda.AppContainer
import com.dbv.estuda.DbvApp
import com.dbv.estuda.ui.theme.Amarelo
import com.dbv.estuda.ui.theme.Azul
import com.dbv.estuda.ui.theme.Cinza
import com.dbv.estuda.ui.theme.Linha
import com.dbv.estuda.ui.theme.corDaArea
import com.dbv.estuda.ui.theme.corDaNota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun container(): AppContainer = (LocalContext.current.applicationContext as DbvApp).container

fun fmtNota(n: Double?): String = if (n == null) "–" else String.format(Locale("pt", "BR"), "%.1f", n)

fun fmtData(ms: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(ms))

fun fmtDataCurta(ms: Long): String = SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(ms))

@Composable
fun BarraTopo(titulo: String, aoVoltar: (() -> Unit)? = null, acoes: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(titulo, maxLines = 1) },
        navigationIcon = {
            if (aoVoltar != null) IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = { acoes() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Azul, titleContentColor = Color.White,
            navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
        ),
    )
}

/** Insígnia redonda com a cor da área e o código da especialidade. */
@Composable
fun Insignia(codigo: String, area: String, tamanho: Dp = 48.dp) {
    Box(
        Modifier.size(tamanho).clip(CircleShape).background(corDaArea(area)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            codigo.substringAfterLast('-'),
            color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = (tamanho.value / 3.6f).sp, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SeloNota(nota: Double, modifier: Modifier = Modifier) {
    val cor = corDaNota(nota)
    Surface(color = cor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(fmtNota(nota), color = cor, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun Selo(texto: String, fundo: Color, cor: Color) {
    Surface(color = fundo, shape = RoundedCornerShape(6.dp)) {
        Text(texto, color = cor, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun CartaoNumero(valor: String, rotulo: String, modifier: Modifier = Modifier, cor: Color = Azul) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(valor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = cor)
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = Cinza)
        }
    }
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelSmall, color = Cinza,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun Vazio(texto: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = Cinza, textAlign = TextAlign.Center)
    }
}

@Composable
fun Carregando() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

/** Barra de progresso horizontal simples (0..1). */
@Composable
fun Barra(fracao: Float, cor: Color, modifier: Modifier = Modifier, altura: Dp = 10.dp) {
    Box(modifier.fillMaxWidth().height(altura).clip(RoundedCornerShape(altura)).background(Linha)) {
        Box(Modifier.fillMaxWidth(fracao.coerceIn(0f, 1f)).height(altura).clip(RoundedCornerShape(altura)).background(cor))
    }
}

/** Média por área em barras horizontais (nota de 0 a 10). */
@Composable
fun GraficoBarras(itens: List<Triple<String, Double, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itens.forEach { (rotulo, valor, cor) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rotulo, style = MaterialTheme.typography.bodyMedium)
                    Text(fmtNota(valor), fontWeight = FontWeight.Bold, color = corDaNota(valor))
                }
                Spacer(Modifier.height(4.dp))
                Barra((valor / 10.0).toFloat(), cor)
            }
        }
    }
}

/** Evolução das notas (mais antiga à esquerda). */
@Composable
fun GraficoLinha(notas: List<Double>, modifier: Modifier = Modifier) {
    if (notas.size < 2) {
        Vazio("Faça pelo menos duas tentativas para ver sua evolução.")
        return
    }
    Column(modifier) {
    Canvas(Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
        val w = size.width
        val h = size.height
        // linha de aprovação (7,0)
        val y7 = h - (7f / 10f) * h
        drawLine(Amarelo, Offset(0f, y7), Offset(w, y7), strokeWidth = 2f)
        val passo = w / (notas.size - 1)
        val pontos = notas.mapIndexed { i, n -> Offset(i * passo, h - (n.toFloat() / 10f) * h) }
        val caminho = Path().apply {
            moveTo(pontos.first().x, pontos.first().y)
            pontos.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(caminho, Azul, style = Stroke(width = 5f))
        pontos.forEachIndexed { i, p -> drawCircle(corDaNota(notas[i]), radius = 9f, center = p) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("mais antiga", style = MaterialTheme.typography.labelSmall, color = Cinza)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(14.dp).height(3.dp).background(Amarelo))
            Text("  nota 7,0", style = MaterialTheme.typography.labelSmall, color = Cinza)
        }
        Text("mais recente", style = MaterialTheme.typography.labelSmall, color = Cinza)
    }
    }
}
