# ITBI Porto Alegre — app Android nativo

App em Kotlin + Jetpack Compose para pesquisar as transações de ITBI publicadas
nos Dados Abertos da Prefeitura de Porto Alegre e corrigir o valor da
negociação pelo **CDI (com % configurável)** ou pelo **IPCA**.

![Build status](https://github.com/SEU_USUARIO/SEU_REPOSITORIO/actions/workflows/android-build.yml/badge.svg)

## Publicando no GitHub

Este projeto já vem com `.gitignore`, `.gitattributes`, `LICENSE` e um
workflow de CI (`.github/workflows/android-build.yml`) que compila um APK de
debug a cada push. Para subir para um repositório novo:

```bash
cd itbi-poa-app
git init
git add .
git commit -m "Primeiro commit: app de consulta e correção do ITBI POA"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git
git push -u origin main
```

Depois do push, vá na aba **Actions** do repositório: o workflow builda o
APK automaticamente e o deixa disponível para download em "Artifacts". Não
esqueça de trocar `SEU_USUARIO/SEU_REPOSITORIO` no badge acima pelo caminho
real do seu repositório.

> O `.gitignore` já bloqueia `local.properties`, `build/`, `.idea/` e
> keystores — nada sensível ou desnecessário vai junto no commit.

## Como abrir

1. Instale o **Android Studio** (versão Koala/2024.1 ou mais recente).
2. `File > Open` e selecione a pasta `itbi-poa-app`.
3. Deixe o Gradle sincronizar (ele vai baixar as dependências automaticamente).
   - Se aparecer aviso sobre o `gradle-wrapper.jar` faltando, clique em
     "Try Again" / permita que o Android Studio regenere o wrapper — ele faz
     isso sozinho na primeira sincronização.
4. Rode em um emulador ou celular físico (API 26+, Android 8.0+).

## Como funciona

- **Busca**: baixa o CSV do ano selecionado (ou de todos os anos, em
  paralelo) direto do portal `dadosabertos.poa.br`, faz o parse e filtra
  por nome da rua/avenida (sem acento, sem case-sensitivity) e,
  opcionalmente, por número do imóvel.
- **Cache local**: cada CSV baixado fica salvo no cache do app por 24h, para
  não precisar rebaixar tudo a cada busca. O botão de atualizar (ícone ⟳)
  força um novo download.
- **Correção monetária**: ao abrir o detalhe de uma negociação, informe o
  **% do CDI desejado** (ex.: 90, 100, 110...) e toque em "Calcular". O app
  busca as séries diárias do CDI (série 12) e mensais do IPCA (série 433) na
  API pública do Banco Central (SGS) entre a data da negociação e hoje, e
  aplica a composição de juros.

## Limitações conhecidas / pontos de atenção

- **URLs dos CSVs fixas no código** (`PoaDataSource.kt`): a prefeitura pode,
  em tese, trocar o ID do recurso no CKAN. Se algum ano parar de baixar,
  confira a URL atual em https://dadosabertos.poa.br/dataset/itbi e
  atualize o mapa `urlsPorAno`.
- **Parser de CSV próprio**: os arquivos usam `;` como delimitador e aspas
  simples (`'`) — formato incomum. O parser em `CsvParser.kt` foi testado
  contra uma amostra real do arquivo de 2024, mas arquivos muito grandes ou
  com caracteres inesperados podem exigir ajustes.
- **"Todos" os anos**: baixa ~7 arquivos CSV (2020–2026) em paralelo — pode
  levar alguns segundos na primeira vez, dependendo da conexão.
- **API do Banco Central**: é gratuita e sem necessidade de chave, mas tem
  limites de uso justo. Para períodos muito longos (ex.: desde 2020), a
  consulta de CDI diário pode retornar milhares de pontos — funciona, mas é
  mais lenta.
- Nenhuma chave de API é necessária em nenhum dos serviços usados.

## Estrutura do projeto

```
app/src/main/java/com/example/itbipoa/
├── MainActivity.kt              # navegação simples entre telas
├── data/
│   ├── model/ItbiRecord.kt      # modelo de uma transação de ITBI
│   ├── csv/CsvParser.kt         # parser do CSV (delimitador ; e aspas simples)
│   ├── network/
│   │   ├── PoaDataSource.kt     # URLs e download dos CSVs da prefeitura
│   │   └── BcbApi.kt            # séries de CDI e IPCA (Banco Central)
│   ├── cache/CsvCache.kt        # cache em disco dos CSVs
│   └── repository/
│       ├── ItbiRepository.kt    # busca/filtra os registros
│       └── IndiceRepository.kt  # taxas de CDI/IPCA
├── util/
│   ├── TextoUtil.kt             # normalização de texto p/ busca
│   └── CorrecaoMonetaria.kt     # cálculo de correção por CDI% e IPCA
└── ui/
    ├── search/                  # tela de busca
    └── detail/                  # tela de detalhe + calculadora
```

## Próximos passos possíveis

- Persistir os resultados em Room para busca 100% offline após o primeiro download.
- Exportar resultado (CSV/PDF) de uma pesquisa.
- Filtro adicional por bairro, tipo de imóvel ou faixa de valor.
