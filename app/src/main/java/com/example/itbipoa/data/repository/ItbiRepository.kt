package com.example.itbipoa.data.repository

import com.example.itbipoa.data.cache.CsvCache
import com.example.itbipoa.data.csv.CsvParser
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.PoaDataSource
import com.example.itbipoa.util.normalizarParaBusca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Resultado de uma busca, incluindo eventuais anos que falharam ao baixar/processar. */
data class ResultadoBusca(
    val registros: List<ItbiRecord>,
    val anosComErro: List<Int> = emptyList()
)

class ItbiRepository(private val cache: CsvCache) {

    private val formatoData = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    /**
     * Busca registros de ITBI.
     *
     * @param ano ano específico, ou null para pesquisar em TODOS os anos disponíveis.
     * @param logradouro texto (livre) para filtrar pelo nome da rua/avenida. Vazio = não filtra.
     * @param numero número do endereço, opcional.
     * @param forcarAtualizacao ignora o cache local e baixa tudo de novo.
     * @param onProgresso callback opcional chamado a cada ano processado (útil para "Todos").
     *
     * IMPORTANTE sobre memória: os anos são processados um de cada vez, em
     * sequência (nunca em paralelo). Cada CSV é lido e filtrado linha a linha,
     * via Sequence — só os registros que batem com a busca ficam guardados na
     * memória; o restante é descartado imediatamente pelo coletor de lixo antes
     * de passar para o próximo ano. Isso é o que evita o travamento ao
     * pesquisar "Todos os anos": nunca existem vários CSVs inteiros (ou vários
     * anos inteiros já convertidos) na memória ao mesmo tempo.
     */
    suspend fun buscar(
        ano: Int?,
        logradouro: String,
        numero: String? = null,
        forcarAtualizacao: Boolean = false,
        onProgresso: ((anoProcessado: Int, total: Int) -> Unit)? = null
    ): ResultadoBusca {
        val anos = ano?.let { listOf(it) } ?: PoaDataSource.anosDisponiveis
        val total = anos.size
        val logradouroBusca = logradouro.normalizarParaBusca()
        val numeroBusca = numero?.trim()

        val registros = mutableListOf<ItbiRecord>()
        val anosComErro = mutableListOf<Int>()

        anos.forEachIndexed { indice, anoAtual ->
            onProgresso?.invoke(indice + 1, total)
            try {
                val filtrados = obterRegistrosFiltradosDoAno(
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
     * Baixa (ou lê do cache) o CSV de um ano e já devolve só os registros que
     * batem com o filtro — processado linha a linha via Sequence, sem nunca
     * materializar a lista inteira (filtrada ou não) do ano em memória.
     */
    private suspend fun obterRegistrosFiltradosDoAno(
        ano: Int,
        forcarAtualizacao: Boolean,
        logradouroBusca: String,
        numeroBusca: String?
    ): List<ItbiRecord> {
        val csvTexto = if (!forcarAtualizacao && cache.estaValido(ano)) {
            cache.ler(ano)
        } else {
            val baixado = PoaDataSource.baixarCsv(ano)
            cache.salvar(ano, baixado)
            baixado
        }

        return withContext(Dispatchers.Default) {
            csvTexto.lineSequence()
                .drop(1) // cabeçalho
                .map { it.trimEnd('\r') }
                .filter { it.isNotBlank() }
                .mapNotNull { linha ->
                    try {
                        converter(CsvParser.parseLine(linha), ano)
                    } catch (e: Exception) {
                        null // ignora silenciosamente uma linha malformada
                    }
                }
                .filter { registro -> coincide(registro, logradouroBusca, numeroBusca) }
                .toList()
        }
    }

    private fun coincide(registro: ItbiRecord, logradouroBusca: String, numeroBusca: String?): Boolean {
        val logradouroOk = logradouroBusca.isBlank() ||
            (registro.logradouro?.normalizarParaBusca()?.contains(logradouroBusca) == true)

        val numeroOk = numeroBusca.isNullOrBlank() ||
            registro.numeroEndereco?.trim() == numeroBusca

        return logradouroOk && numeroOk
    }

    private fun converter(campos: List<String>, ano: Int): ItbiRecord? {
        // Ordem das colunas no CSV oficial (18 campos):
        // data_estimativa;data_pagamento;base_de_calculo;perc_transmitido;finalidade_construcao;
        // logradouro;n_endereco;n_unidade;complemento_endereco;bairro;cep;area_total_terreno;
        // area_constr_total;area_constr_privativa;ano_construcao;n_matricula_reg_imoveis;
        // n_zona_reg_imoveis;situacao
        if (campos.size < 18) return null

        fun campo(i: Int): String? = campos.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }

        return ItbiRecord(
            ano = ano,
            dataEstimativa = parseData(campo(0)),
            dataPagamento = parseData(campo(1)),
            baseCalculo = campo(2)?.toDoubleOrNull(),
            percTransmitido = campo(3)?.toDoubleOrNull(),
            finalidadeConstrucao = campo(4),
            logradouro = campo(5),
            numeroEndereco = campo(6),
            numeroUnidade = campo(7),
            complementoEndereco = campo(8),
            bairro = campo(9),
            cep = campo(10),
            areaTotalTerreno = campo(11)?.toDoubleOrNull(),
            areaConstrTotal = campo(12)?.toDoubleOrNull(),
            areaConstrPrivativa = campo(13)?.toDoubleOrNull(),
            anoConstrucao = campo(14)?.toIntOrNull(),
            numeroMatricula = campo(15),
            numeroZona = campo(16),
            situacao = campo(17)
        )
    }

    private fun parseData(texto: String?): LocalDate? {
        if (texto.isNullOrBlank()) return null
        return try {
            LocalDate.parse(texto, formatoData)
        } catch (e: Exception) {
            null
        }
    }
}
