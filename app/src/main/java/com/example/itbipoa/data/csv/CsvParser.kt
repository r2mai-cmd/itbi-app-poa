package com.example.itbipoa.data.csv

/**
 * Os CSVs da prefeitura usam ';' como delimitador e aspas SIMPLES (') como
 * caractere de citação (em vez do padrão "). Um apóstrofo dentro de um campo
 * citado aparece duplicado (''). Este parser trata esse formato campo a campo,
 * sem depender de nenhuma lib externa.
 */
object CsvParser {

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val n = line.length

        while (i <= n) {
            if (i == n) {
                // linha terminou logo após um ';' -> último campo vazio
                if (result.isEmpty() || line.isNotEmpty() && line.last() == ';') {
                    result.add("")
                }
                break
            }

            if (line[i] == '\'') {
                val sb = StringBuilder()
                i++ // pula aspa de abertura
                while (i < n) {
                    if (line[i] == '\'') {
                        if (i + 1 < n && line[i + 1] == '\'') {
                            sb.append('\'')
                            i += 2
                        } else {
                            i++ // pula aspa de fechamento
                            break
                        }
                    } else {
                        sb.append(line[i])
                        i++
                    }
                }
                result.add(sb.toString())
                // avança até o próximo ';' (ignora lixo entre aspas e delimitador, se houver)
                while (i < n && line[i] != ';') i++
                if (i < n) {
                    i++ // pula ';'
                    if (i == n) result.add("") // campo vazio no final
                } else {
                    break
                }
            } else {
                val start = i
                while (i < n && line[i] != ';') i++
                result.add(line.substring(start, i))
                if (i < n) {
                    i++
                    if (i == n) result.add("")
                } else {
                    break
                }
            }
        }
        return result
    }

    /**
     * Faz o parse de um CSV completo (com cabeçalho na primeira linha),
     * retornando apenas as linhas de dados já separadas em campos.
     */
    fun parseBody(csvText: String): List<List<String>> {
        val linhas = csvText.split('\n')
        if (linhas.isEmpty()) return emptyList()
        return linhas.drop(1) // remove o cabeçalho
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .map { parseLine(it) }
    }
}
