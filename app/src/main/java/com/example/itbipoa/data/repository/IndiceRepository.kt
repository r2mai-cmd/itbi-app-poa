package com.example.itbipoa.data.repository

import com.example.itbipoa.data.network.BcbApi
import java.time.LocalDate

class IndiceRepository {

    /** Taxas diárias do CDI (% ao dia) entre [inicio] e hoje. */
    suspend fun taxasCdiDiarias(inicio: LocalDate): List<Double> {
        return BcbApi.buscarSerie(BcbApi.SERIE_CDI_DIARIO, inicio, LocalDate.now())
            .map { it.valor }
    }

    /** Variações mensais do IPCA (%) entre [inicio] e hoje. */
    suspend fun taxasIpcaMensais(inicio: LocalDate): List<Double> {
        return BcbApi.buscarSerie(BcbApi.SERIE_IPCA_MENSAL, inicio, LocalDate.now())
            .map { it.valor }
    }
}
