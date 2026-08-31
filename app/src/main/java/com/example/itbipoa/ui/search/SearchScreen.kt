package com.example.itbipoa.ui.search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.itbipoa.R
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.source.RegistroFontesItbi
import com.example.itbipoa.ui.components.BotaoLivro
import com.example.itbipoa.ui.components.BotaoTexto
import com.example.itbipoa.ui.components.CartaoSuave
import com.example.itbipoa.ui.components.HorizontalTraco
import com.example.itbipoa.ui.components.LinhaCampo
import com.example.itbipoa.ui.components.RotuloCampo
import com.example.itbipoa.ui.theme.DouradoLinha
import com.example.itbipoa.ui.theme.EstiloValor
import com.example.itbipoa.ui.theme.Terracota
import com.example.itbipoa.ui.theme.TextoSobreFundoSutil
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
    var menuCidadeAberto by remember { mutableStateOf(false) }
    val anosDaCidadeAtual = remember(estado.cidadeSelecionada) {
        RegistroFontesItbi.fonteParaCidade(estado.cidadeSelecionada).anosDisponiveis
    }
    val gerenciadorFoco = LocalFocusManager.current
    val controladorTeclado = LocalSoftwareKeyboardController.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            CabecalhoUsina(compacto = estado.jaBuscou)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
            ) {
                item {
                    CartaoSuave(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            RotuloCampo("Cidade")
                            Spacer(Modifier.height(6.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { menuCidadeAberto = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${estado.cidadeSelecionada.nomeExibicao} - ${estado.cidadeSelecionada.estadoSigla}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Selecionar cidade",
                                        tint = Terracota,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(expanded = menuCidadeAberto, onDismissRequest = { menuCidadeAberto = false }) {
                                    RegistroFontesItbi.cidadesDisponiveis.forEach { cidade ->
                                        DropdownMenuItem(
                                            text = { Text("${cidade.nomeExibicao} - ${cidade.estadoSigla}") },
                                            onClick = { viewModel.onCidadeChange(cidade); menuCidadeAberto = false }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            HorizontalTraco(cor = DouradoLinha, espessura = 1.dp)
                        }

                        Spacer(Modifier.height(20.dp))

                        LinhaCampo(
                            valor = estado.logradouro,
                            aoMudar = viewModel::onLogradouroChange,
                            rotulo = "Endereço (rua ou avenida)",
                            placeholder = "Ex: Nome da rua ou avenida"
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { menuAnoAberto = true },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            estado.anoSelecionado?.toString() ?: "Todos",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Selecionar ano",
                                            tint = Terracota,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(expanded = menuAnoAberto, onDismissRequest = { menuAnoAberto = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Todos") },
                                            onClick = { viewModel.onAnoChange(null); menuAnoAberto = false }
                                        )
                                        anosDaCidadeAtual.forEach { ano ->
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

                        BotaoLivro(
                            texto = "Pesquisar",
                            aoClicar = {
                                gerenciadorFoco.clearFocus()
                                controladorTeclado?.hide()
                                viewModel.buscar(forcarAtualizacao = false)
                            },
                            habilitado = !estado.carregando,
                            icone = Icons.Default.Search,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BotaoTexto(
                                texto = "Limpar",
                                aoClicar = viewModel::limpar,
                                icone = Icons.Default.Close,
                                habilitado = !estado.carregando
                            )
                            BotaoTexto(
                                texto = "Atualizar dados",
                                aoClicar = { viewModel.buscar(forcarAtualizacao = true) },
                                icone = Icons.Default.Refresh,
                                habilitado = !estado.carregando
                            )
                        }

                        if (estado.carregando) {
                            Spacer(Modifier.height(10.dp))
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
                            Spacer(Modifier.height(14.dp))
                            AvisoFaixa(texto = it, cor = MaterialTheme.colorScheme.error)
                        }

                        estado.avisoAnosComErro?.let {
                            Spacer(Modifier.height(14.dp))
                            AvisoFaixa(texto = it, cor = Terracota)
                        }
                    }
                }

                if (estado.jaBuscou && !estado.carregando) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "${estado.resultados.size} REGISTRO(S) ENCONTRADO(S)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextoSobreFundoSutil,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }

                items(estado.resultados) { registro ->
                    LinhaResultado(registro, onClick = { onAbrirDetalhe(registro) })
                    Spacer(Modifier.height(10.dp))
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CabecalhoUsina(compacto: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compacto) Modifier.height(130.dp) else Modifier)
            .animateContentSize()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.header_usina),
            contentDescription = "Ilustração da Usina do Gasômetro ao pôr do sol, Porto Alegre",
            contentScale = if (compacto) ContentScale.Crop else ContentScale.FillWidth,
            modifier = if (compacto) Modifier.matchParentSize() else Modifier.fillMaxWidth()
        )
        // Véu sutil só na parte de cima, garantindo contraste do título
        // mesmo se o céu da ilustração for mais claro em algum ponto.
        // matchParentSize() (não fillMaxSize()) é essencial: ele copia o
        // tamanho que a Box já tem, em vez de tentar expandir a Box.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Transparent
                        ),
                        endY = 260f
                    )
                )
        )
        Text(
            "ITBI POA",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        )
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
    CartaoSuave(modifier = Modifier.fillMaxWidth(), aoClicar = onClick) {
        Row(
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
            Spacer(Modifier.width(10.dp))
            Text(
                "+",
                style = MaterialTheme.typography.titleLarge,
                color = Terracota,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
