package com.example.itbipoa.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.PoaDataSource
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAbrirDetalhe: (ItbiRecord) -> Unit
) {
    val estado = viewModel.uiState
    var menuAnoAberto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ITBI Porto Alegre") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = estado.logradouro,
                onValueChange = viewModel::onLogradouroChange,
                label = { Text("Endereço (rua/avenida)") },
                placeholder = { Text("Ex: Barbedo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = estado.numero,
                    onValueChange = viewModel::onNumeroChange,
                    label = { Text("Número (opcional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                ExposedDropdownMenuBox(
                    expanded = menuAnoAberto,
                    onExpandedChange = { menuAnoAberto = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = estado.anoSelecionado?.toString() ?: "Todos",
                        onValueChange = {},
                        label = { Text("Ano") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAnoAberto) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuAnoAberto,
                        onDismissRequest = { menuAnoAberto = false }
                    ) {
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
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.buscar(forcarAtualizacao = false) },
                    enabled = !estado.carregando,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Buscar")
                }
                OutlinedButton(
                    onClick = { viewModel.buscar(forcarAtualizacao = true) },
                    enabled = !estado.carregando
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar dados")
                }
            }

            if (estado.carregando) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                estado.progresso?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }

            estado.erro?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))

            if (estado.jaBuscou && !estado.carregando) {
                Text(
                    "${estado.resultados.size} resultado(s)",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(estado.resultados) { registro ->
                    ResultadoCard(registro, onClick = { onAbrirDetalhe(registro) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultadoCard(registro: ItbiRecord, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(registro.enderecoCompleto, fontWeight = FontWeight.Bold)
            registro.bairro?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    registro.baseCalculo?.let { formatoMoeda.format(it) } ?: "Valor não informado",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    registro.dataReferencia?.format(formatoDataBr) ?: "Data indisponível",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            registro.finalidadeConstrucao?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
