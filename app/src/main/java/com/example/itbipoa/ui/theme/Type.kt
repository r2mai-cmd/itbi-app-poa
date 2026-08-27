package com.example.itbipoa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sem custom font files: usamos as famílias do sistema, mas combinadas de um
// jeito que foge do "Roboto padrão em tudo" — serifada para títulos (dá ar de
// registro/cartório) e monoespaçada para valores monetários (lê como um
// livro-caixa, reforça que são dados/números precisos).

val Serifada = FontFamily.Serif
val Monoespacada = FontFamily.Monospace
val TextoCorpo = FontFamily.SansSerif

val TipografiaApp = Typography(
    headlineLarge = TextStyle(
        fontFamily = Serifada,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Serifada,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Serifada,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Serifada,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = TextoCorpo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = TextoCorpo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = TextoCorpo,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = TextoCorpo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = TextoCorpo,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

/** Estilo para valores monetários e datas — sempre monoespaçado, "de livro-caixa". */
val EstiloValor = TextStyle(
    fontFamily = Monoespacada,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp
)

val EstiloValorGrande = TextStyle(
    fontFamily = Monoespacada,
    fontWeight = FontWeight.Medium,
    fontSize = 26.sp
)
