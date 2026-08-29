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
     * Converte o texto bruto do CSV em registros no formato comum do app.
     *
     * Implementado como [Sequence] de propósito: permite que o repositório
     * processe e filtre linha a linha sem nunca precisar montar a lista
     * inteira (filtrada ou não) do ano inteiro em memória — o que é
     * essencial para não travar o app ao pesquisar vários anos de uma vez.
     */
    fun processarCsv(csvTexto: String, ano: Int): Sequence<ItbiRecord>
}
