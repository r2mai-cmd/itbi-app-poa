package com.example.itbipoa.data.source

import com.example.itbipoa.data.csv.CsvParser
import com.example.itbipoa.data.model.Cidade
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.PoaDataSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Fonte de dados de ITBI de Porto Alegre: um CSV por ano, delimitador `;`,
 * aspas simples, publicado em https://dadosabertos.poa.br/dataset/itbi
 */
class FonteDadosPortoAlegre : FonteDadosItbi {

    override val cidade = Cidade.PORTO_ALEGRE

    override val anosDisponiveis: List<Int> = PoaDataSource.anosDisponiveis

    override suspend fun baixarCsv(ano: Int): String = PoaDataSource.baixarCsv(ano)

    private val formatoData = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    override fun processarCsv(csvTexto: String, ano: Int): Sequence<ItbiRecord> =
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

    private fun converter(campos: List<String>, ano: Int): ItbiRecord? {
        // Ordem das colunas no CSV oficial de Porto Alegre (18 campos):
        // data_estimativa;data_pagamento;base_de_calculo;perc_transmitido;finalidade_construcao;
        // logradouro;n_endereco;n_unidade;complemento_endereco;bairro;cep;area_total_terreno;
        // area_constr_total;area_constr_privativa;ano_construcao;n_matricula_reg_imoveis;
        // n_zona_reg_imoveis;situacao
        if (campos.size < 18) return null

        fun campo(i: Int): String? = campos.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }

        return ItbiRecord(
            cidade = cidade,
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
