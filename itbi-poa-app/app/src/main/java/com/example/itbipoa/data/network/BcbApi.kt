package com.example.itbipoa.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Consome a API pública de Séries Temporais (SGS) do Banco Central do Brasil.
 * Documentação: https://dadosabertos.bcb.gov.br/dataset/12-taxa-de-juros---cdi
 *
 * Séries usadas:
 *  - 12  = CDI, taxa diária (% ao dia)
 *  - 433 = IPCA, variação mensal (%)
 */
object BcbApi {

    const val SERIE_CDI_DIARIO = 12
    const val SERIE_IPCA_MENSAL = 433

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val formatoBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    data class PontoSerie(val data: LocalDate, val valor: Double)

    /**
     * Busca uma série do SGS entre duas datas (inclusive).
     * Retorna lista vazia se [dataInicial] for depois de [dataFinal].
     */
    suspend fun buscarSerie(
        codigoSerie: Int,
        dataInicial: LocalDate,
        dataFinal: LocalDate = LocalDate.now()
    ): List<PontoSerie> = withContext(Dispatchers.IO) {
        if (dataInicial.isAfter(dataFinal)) return@withContext emptyList()

        val url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.$codigoSerie/dados" +
            "?formato=json" +
            "&dataInicial=${dataInicial.format(formatoBr)}" +
            "&dataFinal=${dataFinal.format(formatoBr)}"

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Falha ao consultar o Banco Central (série $codigoSerie): HTTP ${response.code}")
            }
            val corpo = response.body?.string() ?: "[]"
            parseResposta(corpo)
        }
    }

    private fun parseResposta(json: String): List<PontoSerie> {
        val array = JSONArray(json)
        val lista = mutableListOf<PontoSerie>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val data = LocalDate.parse(obj.getString("data"), formatoBr)
            val valor = obj.getString("valor").replace(",", ".").toDouble()
            lista.add(PontoSerie(data, valor))
        }
        return lista
    }
}
