package com.example.itbipoa.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itbipoa.data.repository.IndiceRepository
import com.example.itbipoa.util.CorrecaoMonetaria
import com.example.itbipoa.util.ResultadoCorrecao
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DetailUiState(
    val percentualCdi: String = "100",
    val calculando: Boolean = false,
    val resultadoCdi: ResultadoCorrecao? = null,
    val resultadoIpca: ResultadoCorrecao? = null,
    val erro: String? = null
)

class DetailViewModel(private val indiceRepository: IndiceRepository) : ViewModel() {

    var uiState by mutableStateOf(DetailUiState())
        private set

    fun onPercentualCdiChange(valor: String) {
        // aceita números com até uma casa decimal (ex.: 90, 91.5)
        if (valor.isEmpty() || valor.matches(Regex("^\\d{0,3}(\\.\\d{0,2})?$"))) {
            uiState = uiState.copy(percentualCdi = valor)
        }
    }

    fun calcular(valorOriginal: Double, dataInicio: LocalDate) {
        val percentual = uiState.percentualCdi.toDoubleOrNull()
        if (percentual == null || percentual <= 0) {
            uiState = uiState.copy(erro = "Informe um percentual de CDI válido (ex.: 90, 100, 110).")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(calculando = true, erro = null)
            try {
                val cdi = CorrecaoMonetaria.corrigirPorCdi(
                    valor = valorOriginal,
                    dataInicio = dataInicio,
                    percentualCdiDesejado = percentual,
                    indiceRepository = indiceRepository
                )
                val ipca = CorrecaoMonetaria.corrigirPorIpca(
                    valor = valorOriginal,
                    dataInicio = dataInicio,
                    indiceRepository = indiceRepository
                )
                uiState = uiState.copy(calculando = false, resultadoCdi = cdi, resultadoIpca = ipca)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    calculando = false,
                    erro = e.message ?: "Erro ao consultar os índices do Banco Central."
                )
            }
        }
    }
}
