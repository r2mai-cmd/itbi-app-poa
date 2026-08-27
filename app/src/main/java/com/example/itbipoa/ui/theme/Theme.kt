package com.example.itbipoa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = TintaVerde,
    onPrimary = Pergaminho,
    secondary = Terracota,
    onSecondary = Pergaminho,
    background = Pergaminho,
    onBackground = TintaVerde,
    surface = PergaminhoEscuro,
    onSurface = TintaVerde,
    surfaceVariant = PergaminhoEscuro,
    onSurfaceVariant = TintaVerdeClara,
    outline = DouradoLinha,
    error = VermelhoErro,
    onError = Pergaminho
)

private val EsquemaEscuro = darkColorScheme(
    primary = TerracotaClara,
    onPrimary = NoiteVerde,
    secondary = TerracotaClara,
    onSecondary = NoiteVerde,
    background = NoiteVerde,
    onBackground = PergaminhoSobreNoite,
    surface = NoiteSuperficie,
    onSurface = PergaminhoSobreNoite,
    surfaceVariant = NoiteSuperficie,
    onSurfaceVariant = PergaminhoSobreNoite,
    outline = TintaVerdeClara,
    error = TerracotaClara,
    onError = NoiteVerde
)

@Composable
fun ItbiPoaTheme(content: @Composable () -> Unit) {
    val esquema = if (isSystemInDarkTheme()) EsquemaEscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, typography = TipografiaApp, content = content)
}
