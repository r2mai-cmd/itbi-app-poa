package com.example.itbipoa.data.source

import com.example.itbipoa.data.model.Cidade
import com.example.itbipoa.data.model.ItbiRecord

/**
 * Contrato que cada cidade suportada pelo app precisa implementar para que o
 * [com.example.itbipoa.data.repository.ItbiRepository] (que é genérico, sem
 * nenhum conhecimento específico de cidade) consiga baixar e interpretar os
 * dados dela.
 *
 * Cada prefeitura publica o CSV com um layout diferente (delimitador,
 * colunas, formato de data etc.) — toda essa particularidade fica isolada
 * dentro da implementação de [processarCsv] de cada cidade.
 */
interface FonteDadosItbi {

    val cidade: Cidade

    /** Anos com dados disponíveis para esta cidade, do mais recente para o mais antigo. */
    val anosDisponiveis: List<Int>

    /** Baixa o CSV bruto (texto) de um determinado ano diretamente da prefeitura. */
    suspend fun baixarCsv(ano: Int): String

    /**
     * Chave usada para guardar o CSV desse ano em cache local. Por padrão,
     * uma chave por cidade+ano (o caso comum: um arquivo por ano). Cidades
     * que publicam um único arquivo com todos os anos juntos (ex.:
     * Fortaleza) devem sobrescrever isso para devolver sempre a MESMA
     * chave, independente do [ano] — assim o app baixa e guarda em cache
     * esse arquivo grande uma única vez, em vez de uma cópia duplicada por
     * ano pesquisado.
     */
    fun chaveCache(ano: Int): String = "${cidade.id}_$ano"

    /**
     * Converte o texto bruto do CSV em registros no formato comum do app,
     * já filtrados para o [ano] pedido (uma fonte cujo arquivo já vem
     * separado por ano pode ignorar esse parâmetro na filtragem, já que o
     * próprio arquivo baixado já é só daquele ano; uma fonte com tudo num
     * arquivo só, como Fortaleza, usa esse parâmetro para selecionar dentro
     * do arquivo somente as linhas daquele ano).
     *
     * Implementado como [Sequence] de propósito: permite que o repositório
     * processe e filtre linha a linha sem nunca precisar montar a lista
     * inteira (filtrada ou não) em memória — o que é essencial para não
     * travar o app ao pesquisar vários anos de uma vez.
     */
    fun processarCsv(csvTexto: String, ano: Int): Sequence<ItbiRecord>
}
