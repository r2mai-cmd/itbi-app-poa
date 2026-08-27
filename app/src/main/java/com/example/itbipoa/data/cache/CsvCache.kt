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
 */
class CsvCache(context: Context, private val validadeHoras: Long = 24) {

    private val diretorio: File = File(context.cacheDir, "itbi_csv").apply { mkdirs() }

    private fun arquivo(ano: Int) = File(diretorio, "itbi_$ano.csv")

    fun estaValido(ano: Int): Boolean {
        val f = arquivo(ano)
        if (!f.exists()) return false
        val idadeMs = System.currentTimeMillis() - f.lastModified()
        return idadeMs < TimeUnit.HOURS.toMillis(validadeHoras)
    }

    suspend fun ler(ano: Int): String = withContext(Dispatchers.IO) {
        arquivo(ano).readText(Charsets.UTF_8)
    }

    suspend fun salvar(ano: Int, conteudo: String) = withContext(Dispatchers.IO) {
        arquivo(ano).writeText(conteudo, Charsets.UTF_8)
    }
}
