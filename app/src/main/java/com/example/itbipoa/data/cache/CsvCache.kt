package com.example.itbipoa.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Guarda uma cópia local de cada CSV baixado para não precisar buscar de novo
 * a cada pesquisa. O cache expira depois de [validadeHoras] horas (a
 * prefeitura costuma atualizar a base periodicamente, não em tempo real).
 *
 * A [chave] identifica de forma única um CSV — como o app agora pode
 * consultar mais de uma cidade, ela deve incluir tanto a cidade quanto o ano
 * (ex.: "poa_2024"), para o cache de uma cidade nunca colidir com o de outra.
 */
class CsvCache(context: Context, private val validadeHoras: Long = 24) {

    private val diretorio: File = File(context.cacheDir, "itbi_csv").apply { mkdirs() }

    private fun arquivo(chave: String) = File(diretorio, "itbi_$chave.csv")

    fun estaValido(chave: String): Boolean {
        val f = arquivo(chave)
        if (!f.exists()) return false
        val idadeMs = System.currentTimeMillis() - f.lastModified()
        return idadeMs < TimeUnit.HOURS.toMillis(validadeHoras)
    }

    suspend fun ler(chave: String): String = withContext(Dispatchers.IO) {
        arquivo(chave).readText(Charsets.UTF_8)
    }

    suspend fun salvar(chave: String, conteudo: String) = withContext(Dispatchers.IO) {
        arquivo(chave).writeText(conteudo, Charsets.UTF_8)
    }
}

