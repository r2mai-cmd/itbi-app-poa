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
 *  - 189 = IGP-M, variação mensal (%) — calculado pela FGV, série mantida pelo BCB
 */
object BcbApi {

    const val SERIE_CDI_DIARIO = 12
    const val SERIE_IPCA_MENSAL = 433
    const val SERIE_IGPM_MENSAL = 189

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

    private fun parseResposta(corpoBruto: String): List<PontoSerie> {
        val corpo = corpoBruto.trim()
        // A API do Banco Central às vezes devolve uma resposta que não é o
        // JSON esperado (por exemplo, uma página de erro em XML/HTML quando
        // o serviço deles está instável). Detectar isso aqui evita que o app
        // quebre com um erro técnico feio — em vez disso, mostra uma
        // mensagem clara e o usuário pode simplesmente tentar de novo.
        if (!corpo.startsWith("[")) {
            throw java.io.IOException(
                "O Banco Central não respondeu como esperado agora. Toque em \"Recalcular\" para tentar de novo."
            )
        }

        return try {
            val array = JSONArray(corpo)
            val lista = mutableListOf<PontoSerie>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val data = LocalDate.parse(obj.getString("data"), formatoBr)
                val valor = obj.getString("valor").replace(",", ".").toDouble()
                lista.add(PontoSerie(data, valor))
            }
            lista
        } catch (e: org.json.JSONException) {
            throw java.io.IOException(
                "Não foi possível interpretar os dados do Banco Central. Toque em \"Recalcular\" para tentar de novo.",
                e
            )
        }
    }
}
