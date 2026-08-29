package com.example.itbipoa.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itbipoa.data.model.Cidade
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.repository.ItbiRepository
import com.example.itbipoa.data.source.RegistroFontesItbi
import kotlinx.coroutines.launch

private val CIDADE_PADRAO = Cidade.PORTO_ALEGRE

data class SearchUiState(
    val cidadeSelecionada: Cidade = CIDADE_PADRAO,
    val logradouro: String = "",
    val numero: String = "",
    // Padrão = ano mais recente disponível (nunca "Todos"), para a primeira busca
    // ser rápida e leve. "Todos" continua disponível, só não é o padrão.
    val anoSelecionado: Int? = RegistroFontesItbi.fonteParaCidade(CIDADE_PADRAO).anosDisponiveis.firstOrNull(),
    val carregando: Boolean = false,
    val progresso: String? = null,
    val resultados: List<ItbiRecord> = emptyList(),
    val avisoAnosComErro: String? = null,
    val jaBuscou: Boolean = false,
    val erro: String? = null
)

class SearchViewModel(private val repository: ItbiRepository) : ViewModel() {

    var uiState by mutableStateOf(SearchUiState())
        private set

    fun onLogradouroChange(valor: String) {
        uiState = uiState.copy(logradouro = valor)
    }

    fun onNumeroChange(valor: String) {
        uiState = uiState.copy(numero = valor.filter { it.isDigit() })
    }

    fun onAnoChange(ano: Int?) {
        uiState = uiState.copy(anoSelecionado = ano)
    }

    /**
     * Troca a cidade pesquisada. O ano volta para o mais recente disponível
     * nessa cidade (as cidades podem ter anos diferentes disponíveis), e os
     * resultados anteriores são descartados, já que pertenciam à cidade
     * antiga.
     */
    fun onCidadeChange(cidade: Cidade) {
        val anoPadraoDaCidade = RegistroFontesItbi.fonteParaCidade(cidade).anosDisponiveis.firstOrNull()
        uiState = uiState.copy(
            cidadeSelecionada = cidade,
            anoSelecionado = anoPadraoDaCidade,
            resultados = emptyList(),
            jaBuscou = false,
            erro = null,
            avisoAnosComErro = null
        )
    }

    /** Limpa os campos de busca e os resultados, voltando ao estado inicial da tela. */
    fun limpar() {
        uiState = SearchUiState()
    }

    fun buscar(forcarAtualizacao: Boolean = false) {
        val estadoAtual = uiState
        if (estadoAtual.logradouro.isBlank()) {
            uiState = estadoAtual.copy(erro = "Digite ao menos parte do nome da rua/avenida.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(carregando = true, erro = null, progresso = null, avisoAnosComErro = null)
            try {
                val resultado = repository.buscar(
                    cidade = estadoAtual.cidadeSelecionada,
                    ano = estadoAtual.anoSelecionado,
                    logradouro = estadoAtual.logradouro,
                    numero = estadoAtual.numero.ifBlank { null },
                    forcarAtualizacao = forcarAtualizacao,
                    onProgresso = { feitos, total ->
                        if (total > 1) {
                            uiState = uiState.copy(progresso = "Consultando dados... $feitos/$total anos")
                        }
                    }
                )
                val aviso = if (resultado.anosComErro.isNotEmpty()) {
                    "Não foi possível consultar: ${resultado.anosComErro.sorted().joinToString(", ")}"
                } else null

                uiState = uiState.copy(
                    carregando = false,
                    progresso = null,
                    resultados = resultado.registros,
                    avisoAnosComErro = aviso,
                    jaBuscou = true
                )
            } catch (t: Throwable) {
                // Captura qualquer falha (rede, memória, parsing) para nunca derrubar o app.
                uiState = uiState.copy(
                    carregando = false,
                    progresso = null,
                    erro = t.message ?: "Não foi possível concluir a busca. Tente novamente."
                )
            }
        }
    }
}
