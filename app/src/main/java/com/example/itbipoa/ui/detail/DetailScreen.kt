package com.example.itbipoa.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.ui.components.BotaoLivro
import com.example.itbipoa.ui.components.CartaoSuave
import com.example.itbipoa.ui.components.LinhaCampo
import com.example.itbipoa.ui.components.LinhaFicha
import com.example.itbipoa.ui.theme.CeuAzulProfundo
import com.example.itbipoa.ui.theme.EstiloValorGrande
import com.example.itbipoa.util.ResultadoCorrecao
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun DetailScreen(
    registro: ItbiRecord,
    viewModel: DetailViewModel,
    onVoltar: () -> Unit
) {
    val estado = viewModel.uiState
    val dataInicio = registro.dataReferencia
    val valor = registro.baseCalculo

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Faixa azul no topo — a mesma cor do céu do cabeçalho da busca,
            // para as duas telas terem a mesma identidade visual.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(CeuAzulProfundo)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onVoltar)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "VOLTAR À PESQUISA",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                Text(registro.enderecoCompleto, style = MaterialTheme.typography.headlineMedium)
                registro.bairro?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))

                CartaoSuave(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "DADOS DA NEGOCIAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    LinhaFicha("Valor da negociação", valor?.let { formatoMoeda.format(it) } ?: "não informado")
                    LinhaFicha("Data de referência", dataInicio?.format(formatoDataBr) ?: "não informada")
                    LinhaFicha("Data estimativa", registro.dataEstimativa?.format(formatoDataBr) ?: "—")
                    LinhaFicha("Data pagamento", registro.dataPagamento?.format(formatoDataBr) ?: "—")
                    LinhaFicha("Percentual transmitido", registro.percTransmitido?.let { "%.2f%%".format(it) } ?: "—")
                    LinhaFicha("Finalidade", registro.finalidadeConstrucao ?: "—")
                }

                Spacer(Modifier.height(12.dp))

                CartaoSuave(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "DADOS DO IMÓVEL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    LinhaFicha("Área total do terreno", registro.areaTotalTerreno?.let { "%.2f m²".format(it) } ?: "—")
                    LinhaFicha("Área construída total", registro.areaConstrTotal?.let { "%.2f m²".format(it) } ?: "—")
                    LinhaFicha("Área construída privativa", registro.areaConstrPrivativa?.let { "%.2f m²".format(it) } ?: "—")
                    LinhaFicha("Ano de construção", registro.anoConstrucao?.toString() ?: "—")
                    LinhaFicha("Matrícula", registro.numeroMatricula ?: "—")
                    LinhaFicha("CEP", registro.cep ?: "—")
                    LinhaFicha("Situação", registro.situacao ?: "—")
                }

                Spacer(Modifier.height(28.dp))

                Text("Correção do valor", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Atualiza o valor da negociação até hoje, por CDI (% à sua escolha) e por IPCA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                if (valor == null || dataInicio == null) {
                    Text(
                        "Não é possível calcular: falta o valor ou a data desta negociação.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    CartaoSuave(modifier = Modifier.fillMaxWidth()) {
                        LinhaCampo(
                            valor = estado.percentualCdi,
                            aoMudar = viewModel::onPercentualCdiChange,
                            rotulo = "% do CDI desejado",
                            placeholder = "Ex: 90, 100, 110",
                            teclado = KeyboardType.Decimal
                        )

                        Spacer(Modifier.height(18.dp))
                        BotaoLivro(
                            texto = if (estado.calculando) "Calculando..." else "Calcular correção",
                            aoClicar = { viewModel.calcular(valor, dataInicio) },
                            habilitado = !estado.calculando,
                            icone = Icons.Default.Calculate,
                            modifier = Modifier.fillMaxWidth()
                        )

                        estado.erro?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Os dois blocos de resultado ficam sempre visíveis (mesmo antes de
                    // calcular), para deixar claro que a correção por CDI e por IPCA
                    // são duas opções disponíveis, não algo escondido.
                    BlocoResultado(
                        titulo = if (estado.resultadoCdi != null) "${estado.percentualCdi}% do CDI" else "CDI",
                        resultado = estado.resultadoCdi
                    )
                    Spacer(Modifier.height(12.dp))
                    BlocoResultado(
                        titulo = "IPCA",
                        resultado = estado.resultadoIpca
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BlocoResultado(titulo: String, resultado: ResultadoCorrecao?) {
    CartaoSuave(modifier = Modifier.fillMaxWidth()) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (resultado == null) {
            Text(
                "Toque em \"Calcular correção\" para ver o valor atualizado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            Text(formatoMoeda.format(resultado.valorCorrigido), style = EstiloValorGrande)
            Spacer(Modifier.height(6.dp))
            Text(
                "Variação acumulada: +${"%.2f".format(resultado.percentualAcumulado)}%  ·  " +
                    "${resultado.dataInicio.format(formatoDataBr)} a ${resultado.dataFim.format(formatoDataBr)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
