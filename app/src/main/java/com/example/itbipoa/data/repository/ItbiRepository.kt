package com.example.itbipoa.data.repository

import com.example.itbipoa.data.cache.CsvCache
import com.example.itbipoa.data.model.Cidade
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.source.FonteDadosItbi
import com.example.itbipoa.data.source.RegistroFontesItbi
import com.example.itbipoa.util.normalizarLogradouro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Resultado de uma busca, incluindo eventuais anos que falharam ao baixar/processar. */
data class ResultadoBusca(
    val registros: List<ItbiRecord>,
    val anosComErro: List<Int> = emptyList()
)

/**
 * Repositório genérico de busca de ITBI: não conhece nenhuma cidade
 * específica, apenas orquestra o download com cache, o processamento e a
 * filtragem, delegando as particularidades de cada cidade para a
 * [FonteDadosItbi] correspondente (ver [RegistroFontesItbi]).
 */
class ItbiRepository(private val cache: CsvCache) {

    /**
     * Busca registros de ITBI numa cidade.
     *
     * @param cidade cidade a pesquisar.
     * @param ano ano específico, ou null para pesquisar em TODOS os anos disponíveis dessa cidade.
     * @param logradouro texto (livre) para filtrar pelo nome da rua/avenida. Vazio = não filtra.
     * @param numero número do endereço, opcional.
     * @param forcarAtualizacao ignora o cache local e baixa tudo de novo.
     * @param onProgresso callback opcional chamado a cada ano processado (útil para "Todos").
     *
     * IMPORTANTE sobre memória: os anos são processados um de cada vez, em
     * sequência (nunca em paralelo). Cada CSV é lido e filtrado linha a
     * linha, via Sequence — só os registros que batem com a busca ficam
     * guardados na memória; o restante é descartado imediatamente antes de
     * passar para o próximo ano.
     */
    suspend fun buscar(
        cidade: Cidade,
        ano: Int?,
        logradouro: String,
        numero: String? = null,
        forcarAtualizacao: Boolean = false,
        onProgresso: ((anoProcessado: Int, total: Int) -> Unit)? = null
    ): ResultadoBusca {
        val fonte = RegistroFontesItbi.fonteParaCidade(cidade)
        val anos = ano?.let { listOf(it) } ?: fonte.anosDisponiveis
        val total = anos.size
        val logradouroBusca = logradouro.normalizarLogradouro()
        val numeroBusca = numero?.trim()

        val registros = mutableListOf<ItbiRecord>()
        val anosComErro = mutableListOf<Int>()

        anos.forEachIndexed { indice, anoAtual ->
            onProgresso?.invoke(indice + 1, total)
            try {
                val filtrados = obterRegistrosFiltradosDoAno(
                    fonte = fonte,
                    ano = anoAtual,
                    forcarAtualizacao = forcarAtualizacao,
                    logradouroBusca = logradouroBusca,
                    numeroBusca = numeroBusca
                )
                registros.addAll(filtrados)
            } catch (t: Throwable) {
                anosComErro.add(anoAtual)
            }
        }

        if (registros.isEmpty() && anosComErro.size == anos.size) {
            throw java.io.IOException("Não foi possível obter os dados de nenhum ano consultado.")
        }

        return ResultadoBusca(
            registros = registros.sortedByDescending { it.dataReferencia ?: LocalDate.MIN },
            anosComErro = anosComErro
        )
    }

    /**
     * Baixa (ou lê do cache) o CSV de um ano de uma fonte e já devolve só os
     * registros que batem com o filtro — processado linha a linha via
     * Sequence (dentro de [FonteDadosItbi.processarCsv]), sem nunca
     * materializar a lista inteira (filtrada ou não) do ano em memória.
     */
    private suspend fun obterRegistrosFiltradosDoAno(
        fonte: FonteDadosItbi,
        ano: Int,
        forcarAtualizacao: Boolean,
        logradouroBusca: String,
        numeroBusca: String?
    ): List<ItbiRecord> {
        val chaveCache = "${fonte.cidade.id}_$ano"
        val csvTexto = if (!forcarAtualizacao && cache.estaValido(chaveCache)) {
            cache.ler(chaveCache)
        } else {
            val baixado = fonte.baixarCsv(ano)
            cache.salvar(chaveCache, baixado)
            baixado
        }

        return withContext(Dispatchers.Default) {
            fonte.processarCsv(csvTexto, ano)
                .filter { registro -> coincide(registro, logradouroBusca, numeroBusca) }
                .toList()
        }
    }

    private fun coincide(registro: ItbiRecord, logradouroBusca: String, numeroBusca: String?): Boolean {
        val logradouroOk = logradouroBusca.isBlank() ||
            (registro.logradouro?.normalizarLogradouro()?.contains(logradouroBusca) == true)

        val numeroOk = numeroBusca.isNullOrBlank() ||
            registro.numeroEndereco?.trim() == numeroBusca

        return logradouroOk && numeroOk
    }
}
