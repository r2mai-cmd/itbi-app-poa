package com.example.itbipoa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.itbipoa.ui.theme.DouradoLinha
import com.example.itbipoa.ui.theme.EstiloValor
import com.example.itbipoa.ui.theme.Terracota

/** Rótulo em versalete espaçado — usado no lugar de "labels" genéricos de Material. */
@Composable
fun RotuloCampo(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Campo de texto no estilo "linha de formulário de papel": sem caixa nem
 * preenchimento — só um rótulo em cima e um traço embaixo, que engrossa e
 * muda de cor quando o campo está em foco. Substitui o OutlinedTextField
 * padrão do Material, que é a cara mais reconhecível de "app genérico".
 */
@Composable
fun LinhaCampo(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    teclado: KeyboardType = KeyboardType.Text
) {
    val interacao = remember { MutableInteractionSource() }
    val focado by interacao.collectIsFocusedAsState()
    val corLinha = if (focado) Terracota else DouradoLinha
    val espessura = if (focado) 2.dp else 1.dp

    Column(modifier = modifier) {
        RotuloCampo(rotulo)
        Spacer(Modifier.height(6.dp))
        Box {
            if (valor.isEmpty() && placeholder != null) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            BasicTextField(
                value = valor,
                onValueChange = aoMudar,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(Terracota),
                keyboardOptions = KeyboardOptions(keyboardType = teclado),
                interactionSource = interacao,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(6.dp))
        HorizontalTraco(cor = corLinha, espessura = espessura)
    }
}

@Composable
fun HorizontalTraco(
    modifier: Modifier = Modifier,
    cor: Color = DouradoLinha,
    espessura: Dp = 1.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(espessura)
            .background(cor)
    )
}

/** Botão retangular (sem cantos de pílula) — foge do padrão visual mais óbvio do Material. */
@Composable
fun BotaoLivro(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    preenchido: Boolean = true
) {
    val corFundo = if (preenchido) Terracota else Color.Transparent
    val corTexto = if (preenchido) Color.White else Terracota

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .then(if (!preenchido) Modifier.border(BorderStroke(1.dp, Terracota)) else Modifier)
            .background(if (habilitado) corFundo else corFundo.copy(alpha = 0.35f))
            .clickable(enabled = habilitado, onClick = aoClicar)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (habilitado) corTexto else corTexto.copy(alpha = 0.7f)
        )
    }
}

/** Linha "rótulo: valor" usada nas fichas de detalhe — como um extrato/livro-caixa. */
@Composable
fun LinhaFicha(rotulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            rotulo.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = EstiloValor,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
    Spacer(Modifier.height(10.dp))
}

/** Caixa com apenas borda fina (sem sombra, sem preenchimento) — para destacar um resultado. */
@Composable
fun CaixaContorno(
    modifier: Modifier = Modifier,
    cor: Color = DouradoLinha,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, cor))
            .padding(16.dp),
        content = conteudo
    )
}
