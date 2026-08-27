package com.example.itbipoa.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.PoaDataSource
import com.example.itbipoa.ui.components.BotaoLivro
import com.example.itbipoa.ui.components.HorizontalTraco
import com.example.itbipoa.ui.components.LinhaCampo
import com.example.itbipoa.ui.components.RotuloCampo
import com.example.itbipoa.ui.theme.DouradoLinha
import com.example.itbipoa.ui.theme.EstiloValor
import com.example.itbipoa.ui.theme.Terracota
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAbrirDetalhe: (ItbiRecord) -> Unit
) {
    val estado = viewModel.uiState
    var menuAnoAberto by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Cabeçalho autoral: sem TopAppBar padrão do Material.
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text("Livro do ITBI", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "REGISTRO DE TRANSMISSÕES IMOBILIÁRIAS · PORTO ALEGRE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalTraco(cor = DouradoLinha, espessura = 1.dp)

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
            ) {
                LinhaCampo(
                    valor = estado.logradouro,
                    aoMudar = viewModel::onLogradouroChange,
                    rotulo = "Endereço (rua ou avenida)",
                    placeholder = "Ex: Barbedo"
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LinhaCampo(
                        valor = estado.numero,
                        aoMudar = viewModel::onNumeroChange,
                        rotulo = "Número",
                        placeholder = "opcional",
                        teclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        RotuloCampo("Ano")
                        Spacer(Modifier.height(6.dp))
                        Box {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { menuAnoAberto = true }
                                ) {
                                    Text(
                                        estado.anoSelecionado?.toString() ?: "Todos",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text("▾", color = Terracota, style = MaterialTheme.typography.bodyLarge)
                            }
                            DropdownMenu(expanded = menuAnoAberto, onDismissRequest = { menuAnoAberto = false }) {
                                DropdownMenuItem(
                                    text = { Text("Todos") },
                                    onClick = { viewModel.onAnoChange(null); menuAnoAberto = false }
                                )
                                PoaDataSource.anosDisponiveis.forEach { ano ->
                                    DropdownMenuItem(
                                        text = { Text(ano.toString()) },
                                        onClick = { viewModel.onAnoChange(ano); menuAnoAberto = false }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        HorizontalTraco(cor = DouradoLinha, espessura = 1.dp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BotaoLivro(
                        texto = "Pesquisar",
                        aoClicar = { viewModel.buscar(forcarAtualizacao = false) },
                        habilitado = !estado.carregando,
                        modifier = Modifier.weight(1f)
                    )
                    BotaoLivro(
                        texto = "Atualizar",
                        aoClicar = { viewModel.buscar(forcarAtualizacao = true) },
                        habilitado = !estado.carregando,
                        preenchido = false
                    )
                }

                if (estado.carregando) {
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Terracota,
                        trackColor = DouradoLinha
                    )
                    estado.progresso?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }

                estado.erro?.let {
                    Spacer(Modifier.height(16.dp))
                    AvisoFaixa(texto = it, cor = MaterialTheme.colorScheme.error)
                }

                estado.avisoAnosComErro?.let {
                    Spacer(Modifier.height(16.dp))
                    AvisoFaixa(texto = it, cor = Terracota)
                }

                if (estado.jaBuscou && !estado.carregando) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "${estado.resultados.size} REGISTRO(S) ENCONTRADO(S)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                items(estado.resultados) { registro ->
                    LinhaResultado(registro, onClick = { onAbrirDetalhe(registro) })
                    HorizontalTraco(cor = DouradoLinha, espessura = 1.dp)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AvisoFaixa(texto: String, cor: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .background(cor)
        )
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = cor)
    }
}

@Composable
private fun LinhaResultado(registro: ItbiRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(registro.enderecoCompleto, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(registro.bairro, registro.finalidadeConstrucao).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                registro.baseCalculo?.let { formatoMoeda.format(it) } ?: "—",
                style = EstiloValor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                registro.dataReferencia?.format(formatoDataBr) ?: "sem data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
