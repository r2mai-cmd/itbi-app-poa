package com.example.itbipoa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CorPrimaria = Color(0xFF2E5D4B) // verde “prefeitura”, discreto

private val EsquemaClaro = lightColorScheme(primary = CorPrimaria)
private val EsquemaEscuro = darkColorScheme(primary = CorPrimaria)

@Composable
fun ItbiPoaTheme(content: @Composable () -> Unit) {
    val esquema = if (isSystemInDarkTheme()) EsquemaEscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, content = content)
}
