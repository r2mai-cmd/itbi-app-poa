package com.example.itbipoa.data.model

import java.time.LocalDate

/**
 * Representa uma linha (uma transação de ITBI) de um dos CSVs abertos
 * da Prefeitura de Porto Alegre.
 *
 * Dicionário de dados oficial:
 * https://dadosabertos.poa.br/dataset/itbi/resource/b0fe9643-82bb-4b91-be26-b6292b7aeac7
 */
data class ItbiRecord(
    val ano: Int,
    val dataEstimativa: LocalDate?,
    val dataPagamento: LocalDate?,
    val baseCalculo: Double?,
    val percTransmitido: Double?,
    val finalidadeConstrucao: String?,
    val logradouro: String?,
    val numeroEndereco: String?,
    val numeroUnidade: String?,
    val complementoEndereco: String?,
    val bairro: String?,
    val cep: String?,
    val areaTotalTerreno: Double?,
    val areaConstrTotal: Double?,
    val areaConstrPrivativa: Double?,
    val anoConstrucao: Int?,
    val numeroMatricula: String?,
    val numeroZona: String?,
    val situacao: String?
) {
    /** Data usada como referência para a correção monetária: pagamento se existir, senão estimativa. */
    val dataReferencia: LocalDate?
        get() = dataPagamento ?: dataEstimativa

    val enderecoCompleto: String
        get() {
            val partes = mutableListOf<String>()
            if (!logradouro.isNullOrBlank()) partes.add(logradouro)
            if (!numeroEndereco.isNullOrBlank() && numeroEndereco != "0") partes.add(numeroEndereco)
            if (!numeroUnidade.isNullOrBlank()) partes.add("Unid. $numeroUnidade")
            if (!complementoEndereco.isNullOrBlank()) partes.add(complementoEndereco)
            return partes.joinToString(", ")
        }
}
