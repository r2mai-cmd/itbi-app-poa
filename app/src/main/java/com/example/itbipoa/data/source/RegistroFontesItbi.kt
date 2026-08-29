package com.example.itbipoa.data.source

import com.example.itbipoa.data.model.Cidade

/**
 * Ponto único onde as cidades suportadas pelo app são "ligadas" às suas
 * respectivas fontes de dados. Para adicionar uma nova cidade, basta:
 *  1. Adicionar a entrada em [Cidade].
 *  2. Criar a implementação de [FonteDadosItbi] correspondente.
 *  3. Adicionar uma linha no mapa abaixo.
 * Nenhum outro arquivo do app precisa ser tocado.
 */
object RegistroFontesItbi {

    private val fontes: Map<Cidade, FonteDadosItbi> = mapOf(
        Cidade.PORTO_ALEGRE to FonteDadosPortoAlegre()
    )

    /** Cidades disponíveis para busca no app, na ordem em que devem aparecer no seletor. */
    val cidadesDisponiveis: List<Cidade> = listOf(Cidade.PORTO_ALEGRE)

    fun fonteParaCidade(cidade: Cidade): FonteDadosItbi =
        fontes[cidade] ?: error("Nenhuma fonte de dados de ITBI cadastrada para $cidade")
}
