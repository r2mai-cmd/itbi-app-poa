package com.example.itbipoa.util

import com.example.itbipoa.data.repository.IndiceRepository
import java.time.LocalDate

data class ResultadoCorrecao(
    val valorOriginal: Double,
    val valorCorrigido: Double,
    val percentualAcumulado: Double, // ex.: 12.34 significa +12,34%
    val dataInicio: LocalDate,
    val dataFim: LocalDate
)

object CorrecaoMonetaria {

    /**
     * Corrige [valor] pelo CDI, aplicando apenas [percentualCdiDesejado]% do CDI
     * (ex.: 90 = 90% do CDI, 110 = 110% do CDI) desde [dataInicio] até hoje.
     *
     * O CDI é composto diariamente: cada taxa diária divulgada pelo BCB (série 12,
     * em % ao dia) é multiplicada pelo percentual desejado antes de compor.
     */
    suspend fun corrigirPorCdi(
        valor: Double,
        dataInicio: LocalDate,
        percentualCdiDesejado: Double,
        indiceRepository: IndiceRepository
    ): ResultadoCorrecao {
        val taxasDiarias = indiceRepository.taxasCdiDiarias(dataInicio)
        var fator = 1.0
        for (taxaDia in taxasDiarias) {
            val taxaAjustada = (taxaDia / 100.0) * (percentualCdiDesejado / 100.0)
            fator *= (1 + taxaAjustada)
        }
        return ResultadoCorrecao(
            valorOriginal = valor,
            valorCorrigido = valor * fator,
            percentualAcumulado = (fator - 1) * 100,
            dataInicio = dataInicio,
            dataFim = LocalDate.now()
        )
    }

    /**
     * Corrige [valor] pelo IPCA acumulado (composição das variações mensais,
     * série 433 do BCB) desde [dataInicio] até hoje.
     */
    suspend fun corrigirPorIpca(
        valor: Double,
        dataInicio: LocalDate,
        indiceRepository: IndiceRepository
    ): ResultadoCorrecao {
        return corrigirPorTaxasMensais(valor, dataInicio, indiceRepository.taxasIpcaMensais(dataInicio))
    }

    /**
     * Corrige [valor] pelo IGP-M acumulado (composição das variações mensais,
     * série 189 do BCB — calculada pela FGV) desde [dataInicio] até hoje.
     * É o índice mais comum em reajuste de aluguel e contratos imobiliários,
     * além de ser usado em alguns financiamentos.
     */
    suspend fun corrigirPorIgpm(
        valor: Double,
        dataInicio: LocalDate,
        indiceRepository: IndiceRepository
    ): ResultadoCorrecao {
        return corrigirPorTaxasMensais(valor, dataInicio, indiceRepository.taxasIgpmMensais(dataInicio))
    }

    private fun corrigirPorTaxasMensais(
        valor: Double,
        dataInicio: LocalDate,
        taxasMensais: List<Double>
    ): ResultadoCorrecao {
        var fator = 1.0
        for (taxaMes in taxasMensais) {
            fator *= (1 + taxaMes / 100.0)
        }
        return ResultadoCorrecao(
            valorOriginal = valor,
            valorCorrigido = valor * fator,
            percentualAcumulado = (fator - 1) * 100,
            dataInicio = dataInicio,
            dataFim = LocalDate.now()
        )
    }
}
