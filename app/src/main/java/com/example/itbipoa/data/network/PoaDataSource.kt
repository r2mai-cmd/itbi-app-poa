package com.example.itbipoa.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Baixa os arquivos CSV do conjunto de dados "ITBI" do portal de Dados
 * Abertos de Porto Alegre: https://dadosabertos.poa.br/dataset/itbi
 *
 * As URLs abaixo foram obtidas diretamente na página do conjunto de dados.
 * Se a prefeitura publicar um novo ano ou trocar o ID do recurso, basta
 * atualizar/adicionar uma entrada neste mapa.
 */
object PoaDataSource {

    private const val BASE =
        "https://dadosabertos.poa.br/dataset/dd8ee5be-06f4-4107-a3bf-5e15aebba6c1/resource"

    val urlsPorAno: Map<Int, String> = mapOf(
        2020 to "$BASE/60256fcf-9ae6-4adf-b0b4-b6b45fb4b34b/download/itbi-2020.csv",
        2021 to "$BASE/150848b4-0d01-4ffb-96e0-f5f2a276f5ef/download/itbi-2021.csv",
        2022 to "$BASE/102520c0-3edc-4f85-947f-6a98b96ed160/download/itbi-2022.csv",
        2023 to "$BASE/2a4b0aee-7126-4323-936d-5f82fd04aeef/download/itbi-2023.csv",
        2024 to "$BASE/4947bed6-6be6-40e6-a120-42957e745da5/download/itbi-2024.csv",
        2025 to "$BASE/e46f56f3-ac8a-4513-b155-5d3038a275b2/download/itbi-2025.csv",
        2026 to "$BASE/b4cf6e78-6cb9-41f7-8ef7-57dc67feda0c/download/itbi-2026.csv",
    )

    val anosDisponiveis: List<Int> = urlsPorAno.keys.sortedDescending()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Baixa o CSV bruto (texto) de um determinado ano. */
    suspend fun baixarCsv(ano: Int): String = withContext(Dispatchers.IO) {
        val url = urlsPorAno[ano]
            ?: throw IllegalArgumentException("Não há dados de ITBI cadastrados para o ano $ano")

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Falha ao baixar dados de $ano: HTTP ${response.code}")
            }
            response.body?.string() ?: throw java.io.IOException("Resposta vazia para o ano $ano")
        }
    }
}
