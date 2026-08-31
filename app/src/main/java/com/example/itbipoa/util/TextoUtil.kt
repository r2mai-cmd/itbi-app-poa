package com.example.itbipoa.util

import java.text.Normalizer

/**
 * Preposições e artigos que às vezes aparecem no nome oficial de uma rua e
 * às vezes não (ex.: "Carvalho de Freitas" vs "Carvalho Freitas"), a
 * depender de como cada prefeitura cadastrou o logradouro. Removê-las antes
 * de comparar evita que essa diferença de grafia impeça a busca de achar
 * o endereço certo.
 */
private val CONECTIVOS = Regex("\\b(DE|DA|DO|DAS|DOS|E)\\b")

/** Remove acentuação e normaliza para maiúsculas, para comparar endereços com segurança. */
fun String.normalizarParaBusca(): String {
    val semAcento = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    return semAcento.uppercase().trim()
}

/**
 * Igual a [normalizarParaBusca], mas também remove preposições/artigos
 * comuns ("de", "da", "do"...) e espaços duplicados. Usado especificamente
 * na comparação de nomes de rua, onde essas palavras costumam divergir
 * entre o que o usuário digita e como a prefeitura cadastrou o logradouro.
 */
fun String.normalizarLogradouro(): String {
    return normalizarParaBusca()
        .replace(CONECTIVOS, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
