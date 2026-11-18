package com.dbv.estuda.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cores do protótipo DBV Estuda+
val Azul = Color(0xFF0E2A47)
val AzulMedio = Color(0xFF14365A)
val AzulClaro = Color(0xFFE8EFF8)
val Link = Color(0xFF1F5FAD)
val Amarelo = Color(0xFFF5B700)
val AmareloClaro = Color(0xFFFFF4CC)
val AmareloTexto = Color(0xFF7A4F00)
val Verde = Color(0xFF2E9E55)
val VerdeEscuro = Color(0xFF1E7A3E)
val VerdeClaro = Color(0xFFE6F4EA)
val Vermelho = Color(0xFFB42318)
val VermelhoClaro = Color(0xFFFDECEA)
val Texto = Color(0xFF1A2433)
val Cinza = Color(0xFF5E6A7A)
val Linha = Color(0xFFE5E9EF)
val FundoSuave = Color(0xFFF5F7FA)

/** Cor de cada área, para insígnias e gráficos. */
fun corDaArea(area: String): Color = when (area) {
    "ID" -> Azul
    "CS" -> Color(0xFFD63B3B)
    "EN" -> Color(0xFF2E9E55)
    "AR" -> Color(0xFF2F6FD6)
    "AM" -> Color(0xFF7B3FA0)
    "AM-EB" -> Color(0xFF5B3A8C)
    "HM" -> Color(0xFFE07A00)
    "HD" -> Color(0xFFB8860B)
    "AP" -> Color(0xFF455A64)
    "AA" -> Color(0xFF6D8B1E)
    "AD" -> Color(0xFF00897B)
    else -> Color(0xFF8A94A3)
}

fun corDaNota(nota: Double): Color = when {
    nota >= 7.0 -> Verde
    nota >= 5.0 -> Color(0xFFC98A00)
    else -> Vermelho
}

private val esquema = lightColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = Azul,
    secondary = Amarelo,
    onSecondary = Azul,
    secondaryContainer = AmareloClaro,
    onSecondaryContainer = AmareloTexto,
    tertiary = Verde,
    error = Vermelho,
    errorContainer = VermelhoClaro,
    background = Color.White,
    surface = Color.White,
    onSurface = Texto,
    surfaceVariant = FundoSuave,
    onSurfaceVariant = Cinza,
    outline = Color(0xFFD5DBE3),
    outlineVariant = Linha,
)

private val tipografia = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = Azul),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, color = Azul),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    )
}

@Composable
fun DbvTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, content = conteudo)
}
