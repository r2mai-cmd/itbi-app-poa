package com.example.itbipoa.data.repository

import com.example.itbipoa.data.cache.CsvCache
import com.example.itbipoa.data.csv.CsvParser
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.PoaDataSource
import com.example.itbipoa.util.normalizarParaBusca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
     * Importante: o filtro é aplicado ano a ano, logo após o parse de cada CSV,
     * em vez de acumular todos os registros de todos os anos na memória antes de
     * filtrar. Isso evita picos de memória (e travamentos) ao pesquisar "Todos".
     * Se um ano específico falhar ao baixar, os demais continuam normalmente e o
     * ano problemático é reportado em [ResultadoBusca.anosComErro].
     */
    suspend fun buscar(
        ano: Int?,
        logradouro: String,
        numero: String? = null,
        forcarAtualizacao: Boolean = false,
        onProgresso: ((anoProcessado: Int, total: Int) -> Unit)? = null
    ): ResultadoBusca = coroutineScope {
        val anos = ano?.let { listOf(it) } ?: PoaDataSource.anosDisponiveis
        val total = anos.size
        var processados = 0

        val deferreds = anos.map { anoAtual ->
            async {
                processados++
                val progressoAtual = processados
                onProgresso?.invoke(progressoAtual, total)
                try {
                    val registrosDoAno = obterRegistrosDoAno(anoAtual, forcarAtualizacao)
                    val filtrados = filtrar(registrosDoAno, logradouro, numero)
                    Result.success(filtrados)
                } catch (e: Throwable) {
                    Result.failure<List<ItbiRecord>>(e)
                }
            }
        }

        val resultadosPorAno = deferreds.awaitAll()
        val registros = resultadosPorAno.mapNotNull { it.getOrNull() }.flatten()
        val anosComErro = anos.filterIndexed { index, _ -> resultadosPorAno[index].isFailure }

        if (registros.isEmpty() && anosComErro.size == anos.size) {
            // Nenhum ano funcionou: melhor propagar um erro claro do que devolver silêncio.
            throw resultadosPorAno.first().exceptionOrNull()
                ?: java.io.IOException("Não foi possível obter os dados de nenhum ano.")
        }

        ResultadoBusca(
            registros = registros.sortedByDescending { it.dataReferencia ?: LocalDate.MIN },
            anosComErro = anosComErro
        )
    }

    private suspend fun obterRegistrosDoAno(ano: Int, forcarAtualizacao: Boolean): List<ItbiRecord> {
        val csvTexto = if (!forcarAtualizacao && cache.estaValido(ano)) {
            cache.ler(ano)
        } else {
            val baixado = PoaDataSource.baixarCsv(ano)
            cache.salvar(ano, baixado)
            baixado
        }
        return withContext(Dispatchers.Default) {
            CsvParser.parseBody(csvTexto).mapNotNull { campos ->
                try {
                    converter(campos, ano)
                } catch (e: Exception) {
                    null // ignora silenciosamente uma linha malformada, sem derrubar o restante
                }
            }
        }
    }

    private fun filtrar(registros: List<ItbiRecord>, logradouro: String, numero: String?): List<ItbiRecord> {
        val logradouroBusca = logradouro.normalizarParaBusca()
        val numeroBusca = numero?.trim()

        return registros.filter { registro ->
            val logradouroOk = logradouroBusca.isBlank() ||
                (registro.logradouro?.normalizarParaBusca()?.contains(logradouroBusca) == true)

            val numeroOk = numeroBusca.isNullOrBlank() ||
                registro.numeroEndereco?.trim() == numeroBusca

            logradouroOk && numeroOk
        }
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
