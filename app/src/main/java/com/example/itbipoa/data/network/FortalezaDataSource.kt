package com.example.itbipoa.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Baixa o CSV único (todos os anos juntos) do conjunto de dados de ITBI de
 * Fortaleza: https://dados.fortaleza.ce.gov.br/dataset/itbi
 *
 * Diferente de Porto Alegre, aqui não há um arquivo por ano — é um único CSV
 * grande (dezenas de MB) com todas as transações. Quem separa por ano na
 * hora de filtrar é [com.example.itbipoa.data.source.FonteDadosFortaleza].
 *
 * IMPORTANTE: o arquivo da prefeitura não vem em UTF-8, e sim em
 * ISO-8859-1 (Latin-1/Windows-1252) — formato comum em sistemas legados do
 * setor público. Se lido como UTF-8 (padrão), toda acentuação vira
 * caracteres quebrados. Por isso lemos os bytes brutos e decodificamos
 * explicitamente com o charset correto.
 */
object FortalezaDataSource {

    private const val URL_CSV =
        "https://dados.fortaleza.ce.gov.br/dataset/32608ed2-e4ad-4cd2-b7bc-deea35dd189f/" +
            "resource/46324d49-9809-4d32-8e5c-b4b570f067ce/download/" +
            "dados_abertos_itbi_transacoes_imobiliarias.csv"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // arquivo grande, dá mais tempo
        .build()

    suspend fun baixarCsv(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(URL_CSV).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Falha ao baixar dados de Fortaleza: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes()
                ?: throw java.io.IOException("Resposta vazia ao baixar dados de Fortaleza")
            bytes.toString(Charsets.ISO_8859_1)
        }
    }
}
