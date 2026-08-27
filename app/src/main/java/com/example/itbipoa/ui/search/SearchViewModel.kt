package com.example.itbipoa.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.repository.ItbiRepository
import kotlinx.coroutines.launch

data class SearchUiState(
    val logradouro: String = "",
    val numero: String = "",
    val anoSelecionado: Int? = null, // null = "Todos"
    val carregando: Boolean = false,
    val progresso: String? = null,
    val resultados: List<ItbiRecord> = emptyList(),
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

    fun buscar(forcarAtualizacao: Boolean = false) {
        val estadoAtual = uiState
        if (estadoAtual.logradouro.isBlank()) {
            uiState = estadoAtual.copy(erro = "Digite ao menos parte do nome da rua/avenida.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(carregando = true, erro = null, progresso = null)
            try {
                val resultados = repository.buscar(
                    ano = estadoAtual.anoSelecionado,
                    logradouro = estadoAtual.logradouro,
                    numero = estadoAtual.numero.ifBlank { null },
                    forcarAtualizacao = forcarAtualizacao,
                    onProgresso = { feitos, total ->
                        if (total > 1) {
                            uiState = uiState.copy(progresso = "Baixando dados... $feitos/$total anos")
                        }
                    }
                )
                uiState = uiState.copy(
                    carregando = false,
                    progresso = null,
                    resultados = resultados,
                    jaBuscou = true
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    carregando = false,
                    progresso = null,
                    erro = e.message ?: "Erro desconhecido ao buscar os dados."
                )
            }
        }
    }
}
