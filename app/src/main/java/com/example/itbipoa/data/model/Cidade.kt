package com.example.itbipoa.data.model

/**
 * Cidades cujos dados abertos de ITBI o app sabe consultar. Para adicionar
 * uma nova cidade:
 *  1. Adicione uma entrada aqui (id, nome de exibição e sigla do estado).
 *  2. Crie uma implementação de [com.example.itbipoa.data.source.FonteDadosItbi]
 *     para ela (ex.: FonteDadosFortaleza).
 *  3. Registre essa fonte em
 *     [com.example.itbipoa.data.source.RegistroFontesItbi].
 * Nenhuma outra parte do app (tela de busca, tela de detalhe, repositório)
 * precisa mudar.
 */
enum class Cidade(
    val id: String,
    val nomeExibicao: String,
    val estadoSigla: String
) {
    PORTO_ALEGRE(id = "poa", nomeExibicao = "Porto Alegre", estadoSigla = "RS"),
    FORTALEZA(id = "for", nomeExibicao = "Fortaleza", estadoSigla = "CE")
}
