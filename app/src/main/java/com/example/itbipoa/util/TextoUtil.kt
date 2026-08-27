package com.example.itbipoa.util

import java.text.Normalizer

/** Remove acentuação e normaliza para maiúsculas, para comparar endereços com segurança. */
fun String.normalizarParaBusca(): String {
    val semAcento = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    return semAcento.uppercase().trim()
}
