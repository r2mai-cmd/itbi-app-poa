package com.example.itbipoa.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.itbipoa.data.model.ItbiRecord
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val formatoPercentual: NumberFormat = NumberFormat.getPercentInstance(Locale("pt", "BR")).apply {
    maximumFractionDigits = 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    registro: ItbiRecord,
    viewModel: DetailViewModel,
    onVoltar: () -> Unit
) {
    val estado = viewModel.uiState
    val dataInicio = registro.dataReferencia
    val valor = registro.baseCalculo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da negociação") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text(registro.enderecoCompleto, style = MaterialTheme.typography.titleLarge)
            registro.bairro?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(16.dp))
            LinhaInfo("Valor da negociação", valor?.let { formatoMoeda.format(it) } ?: "Não informado")
            LinhaInfo("Data de referência", dataInicio?.format(formatoDataBr) ?: "Não informada")
            LinhaInfo("Data estimativa", registro.dataEstimativa?.format(formatoDataBr) ?: "-")
            LinhaInfo("Data pagamento", registro.dataPagamento?.format(formatoDataBr) ?: "-")
            LinhaInfo("% transmitido", registro.percTransmitido?.let { "%.2f%%".format(it) } ?: "-")
            LinhaInfo("Finalidade", registro.finalidadeConstrucao ?: "-")
            LinhaInfo("Área total terreno", registro.areaTotalTerreno?.let { "%.2f m²".format(it) } ?: "-")
            LinhaInfo("Área construída total", registro.areaConstrTotal?.let { "%.2f m²".format(it) } ?: "-")
            LinhaInfo("Área construída privativa", registro.areaConstrPrivativa?.let { "%.2f m²".format(it) } ?: "-")
            LinhaInfo("Ano de construção", registro.anoConstrucao?.toString() ?: "-")
            LinhaInfo("Matrícula", registro.numeroMatricula ?: "-")
            LinhaInfo("CEP", registro.cep ?: "-")
            LinhaInfo("Situação", registro.situacao ?: "-")

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Corrigir valor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (valor == null || dataInicio == null) {
                Text(
                    "Não é possível calcular a correção: falta o valor ou a data desta negociação.",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            OutlinedTextField(
                value = estado.percentualCdi,
                onValueChange = viewModel::onPercentualCdiChange,
                label = { Text("% do CDI desejado") },
                placeholder = { Text("Ex: 90, 100, 110") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.calcular(valor, dataInicio) },
                enabled = !estado.calculando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.calculando) "Calculando..." else "Calcular correção (CDI e IPCA)")
            }

            estado.erro?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            estado.resultadoCdi?.let { cdi ->
                Spacer(Modifier.height(16.dp))
                CardResultadoCorrecao(
                    titulo = "Corrigido por ${estado.percentualCdi}% do CDI",
                    resultado = cdi
                )
            }

            estado.resultadoIpca?.let { ipca ->
                Spacer(Modifier.height(12.dp))
                CardResultadoCorrecao(
                    titulo = "Corrigido pelo IPCA",
                    resultado = ipca
                )
            }
        }
    }
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun CardResultadoCorrecao(titulo: String, resultado: com.example.itbipoa.util.ResultadoCorrecao) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                formatoMoeda.format(resultado.valorCorrigido),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Variação acumulada: +${"%.2f".format(resultado.percentualAcumulado)}%",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "De ${resultado.dataInicio.format(formatoDataBr)} até ${resultado.dataFim.format(formatoDataBr)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
