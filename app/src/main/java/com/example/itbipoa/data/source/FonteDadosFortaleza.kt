package com.example.itbipoa.data.source

import com.example.itbipoa.data.model.Cidade
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.network.FortalezaDataSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Fonte de dados de ITBI de Fortaleza.
 *
 * Diferenças importantes em relação a Porto Alegre:
 *  - Um único CSV com TODOS os anos juntos (não um arquivo por ano) — por
 *    isso [chaveCache] ignora o ano e sempre devolve a mesma chave, e
 *    [processarCsv] é quem filtra pelo ano pedido, usando a coluna
 *    EXERCICIO de cada linha.
 *  - Delimitador `;`, mas SEM aspas nos campos (não precisa do parser
 *    especial usado em Porto Alegre).
 *  - Números usam vírgula como separador decimal (ex.: "33081,92").
 *  - Não tem os campos de unidade/complemento nem de matrícula de cartório
 *    que Porto Alegre tem — esses ficam null aqui.
 */
class FonteDadosFortaleza : FonteDadosItbi {

    override val cidade = Cidade.FORTALEZA

    // O dataset é um arquivo só; esta é uma estimativa razoável do período
    // coberto (mesma faixa observada em Porto Alegre). Anos sem dados reais
    // simplesmente não trazem nenhum resultado — não é um erro.
    override val anosDisponiveis: List<Int> = (2020..2026).toList().sortedDescending()

    override suspend fun baixarCsv(ano: Int): String = FortalezaDataSource.baixarCsv()

    // Mesma chave sempre: é o mesmo arquivo de ~25 MB para qualquer ano
    // pedido. Sem isso, pesquisar "Todos" baixaria e guardaria em cache 7
    // cópias idênticas desse arquivo.
    override fun chaveCache(ano: Int): String = "${cidade.id}_completo"

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    override fun processarCsv(csvTexto: String, ano: Int): Sequence<ItbiRecord> =
        csvTexto.lineSequence()
            .drop(1) // cabeçalho
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .mapNotNull { linha ->
                try {
                    converter(linha.split(';'))
                } catch (e: Exception) {
                    null // ignora silenciosamente uma linha malformada
                }
            }
            .filter { it.ano == ano } // o arquivo tem todos os anos; filtra aqui pro ano pedido

    private fun converter(campos: List<String>): ItbiRecord? {
        // Ordem das colunas no CSV oficial de Fortaleza (31 campos):
        // VERSAO;INSCRICAO_IMOVEL;DIGITO;NUM_DTI;EXERCICIO;DATA_CADASTRAMENTO_GI_IMOVEL;
        // NUMERO_CEP;BAIRRO;LOGRADOURO;NUMERO;XSIRGAS2000;YSIRGAS2000;QTD_FRENTES;
        // FRACAO_IDEAL;AREA_TERRENO;AREA_EDIFICADA;DATA_CONSTRUCAO;NUMERO_PAVIMENTOS;
        // TIPO_USO_IMOVEL;PADRAO_CONSTRUCAO;TIPO_TERRENO;ANO_MES_DEBITO;NOME_ZONEAMENTO;
        // ZONA_CARTORIO;DATA_DA_TRANSACAO_ITBI;ID_IMOVEL;CARTOGRAFIA;VL_BASE_CALCULO;
        // VL_VENAL;VL_LANCAMENTO_IPTU;IND_COMPRA_VIA_PROGRAMA_HABITACIONAL
        if (campos.size < 31) return null

        fun campo(i: Int): String? = campos.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }
        fun numero(i: Int): Double? = campo(i)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull()

        val ano = campo(4)?.toIntOrNull() ?: return null
        val fracaoIdeal = numero(13)

        return ItbiRecord(
            cidade = cidade,
            ano = ano,
            dataEstimativa = parseData(campo(5)),
            dataPagamento = parseData(campo(24)),
            baseCalculo = numero(27),
            percTransmitido = fracaoIdeal?.times(100),
            finalidadeConstrucao = campo(18),
            logradouro = campo(8),
            numeroEndereco = campo(9),
            numeroUnidade = null,
            complementoEndereco = null,
            bairro = campo(7),
            cep = campo(6),
            areaTotalTerreno = numero(14),
            areaConstrTotal = numero(15),
            areaConstrPrivativa = null,
            anoConstrucao = parseData(campo(16))?.year,
            numeroMatricula = null,
            numeroZona = campo(23),
            situacao = campo(19) // padrão construtivo (ex.: "Normal 2", "Alto nível 1")
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
