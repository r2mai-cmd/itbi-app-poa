import csv
import json
import glob
import os
import shutil
import pandas as pd

csv.field_size_limit(2147483647)

COLUNAS = [
    "DATA_ESTIMATIVA", "DATA_PAGAMENTO", "VALOR", "PERC", "USO", "LOGRADOURO",
    "NUMERO", "UNIDADE", "COMPL", "BAIRRO", "CEP", "AREA_TERRENO", "AREA_TOTAL",
    "AREA_PRIV", "ANO_CONST", "MATRICULA", "CARTORIO", "SITUACAO"
]

# Procura os CSVs em docs/data, data e na raiz
locais = ["docs/data", "data", "."]
todos_arquivos = set()
for pasta in locais:
    for arq in glob.glob(os.path.join(pasta, "itbi-20*.csv")):
        todos_arquivos.add(os.path.normpath(arq))

todos_arquivos = sorted(list(todos_arquivos))
print(f"Arquivos localizados: {len(todos_arquivos)}")

def carregar_csv_seguro(caminho):
    linhas_validas = []
    with open(caminho, mode='r', encoding='utf-8', errors='replace') as f:
        leitor = csv.reader(f, delimiter=';', quotechar="'")
        for num_linha, linha in enumerate(leitor):
            if not linha or not any(linha):
                continue
            if num_linha == 0 and ("DATA" in linha[0].upper() or "ESTIMATIVA" in linha[0].upper()):
                continue
            if len(linha) > 18:
                linha = linha[:18]
            elif len(linha) < 18:
                linha = linha + [""] * (18 - len(linha))
            linhas_validas.append(linha)
    return pd.DataFrame(linhas_validas, columns=COLUNAS)

def pontuar_situacao(s, tem_pagamento):
    pts = 0
    if "IMPRESSA" in s or "PAGA" in s:
        pts += 10
    elif "CALCULADA" in s or "RECALCULADA" in s:
        pts += 4
    elif "ESTIMATIVA" in s:
        pts += 2
    if tem_pagamento:
        pts += 5
    return pts

todos_registros_estatisticas = []

for caminho_csv in todos_arquivos:
    nome = os.path.basename(caminho_csv)
    if any(cid in nome for cid in ["sp", "bh", "recife", "fortaleza"]):
        continue

    print(f"\nDepurando {caminho_csv}...")
    try:
        df = carregar_csv_seguro(caminho_csv)
    except Exception as e:
        print(f"  [ERRO]: {e}")
        continue

    total_antigo = len(df)

    # Identificação única da unidade imobiliária
    df["SITUACAO_NORM"] = df["SITUACAO"].fillna("").str.upper()
    df["TEM_PAG"] = df["DATA_PAGAMENTO"].fillna("").str.strip().apply(lambda x: 1 if len(x) >= 8 else 0)
    df["SCORE"] = [pontuar_situacao(s, p) for s, p in zip(df["SITUACAO_NORM"], df["TEM_PAG"])]

    # Chave por unidade física (independente da data variar 2 ou 3 dias)
    df["CHAVE_UNIDADE"] = (
        df["LOGRADOURO"].fillna("").str.strip().str.upper() + "_" +
        df["NUMERO"].fillna("").str.strip() + "_" +
        df["UNIDADE"].fillna("").str.strip() + "_" +
        df["COMPL"].fillna("").str.strip().str.upper()
    )

    # Ordena para garantir que a Guia Impressa/Paga fique no topo
    df = df.sort_values(by=["CHAVE_UNIDADE", "SCORE"], ascending=[True, False])
    
    # Remove a duplicata da mesma unidade
    df_dedup = df.drop_duplicates(subset=["CHAVE_UNIDADE"], keep="first").copy()
    removidos = total_antigo - len(df_dedup)

    df_export = df_dedup[COLUNAS].copy()
    df_export.to_csv(caminho_csv, sep=";", index=False, header=True, encoding="utf-8")
    print(f"  [OK] Salvo com {len(df_export):,} linhas ({removidos:,} duplicidades de guias removidas).")

    # Copia automaticamente para docs/data caso o arquivo esteja na raiz
    pasta_docs_data = os.path.normpath("docs/data")
    if os.path.exists(pasta_docs_data) and os.path.dirname(caminho_csv) != pasta_docs_data:
        destino = os.path.join(pasta_docs_data, nome)
        df_export.to_csv(destino, sep=";", index=False, header=True, encoding="utf-8")
        print(f"  -> Sincronizado para {destino}")

    # Dados para painel estatístico
    df_dedup["VALOR_F"] = pd.to_numeric(df_dedup["VALOR"].astype(str).str.replace(",", "."), errors="coerce").fillna(0)
    df_dedup["AREA_F"] = pd.to_numeric(df_dedup["AREA_PRIV"].astype(str).str.replace(",", "."), errors="coerce").fillna(0)
    df_dedup["DATA_DT"] = pd.to_datetime(df_dedup["DATA_PAGAMENTO"], format="%d/%m/%Y", errors="coerce")
    
    texto_box = (df_dedup["USO"].fillna("") + " " + df_dedup["COMPL"].fillna("")).str.upper()
    eh_box = texto_box.str.contains(r"BOX|GARAGEM|VAGA|ESTACIONAMENTO|ESTAC", regex=True)

    filtro = df_dedup[
        (~eh_box) &
        (df_dedup["VALOR_F"] >= 30000) &
        (df_dedup["AREA_F"] >= 20) &
        (df_dedup["DATA_DT"].notna())
    ].copy()

    filtro["m2"] = filtro["VALOR_F"] / filtro["AREA_F"]
    filtro = filtro[(filtro["m2"] >= 1200) & (filtro["m2"] <= 35000)]
    filtro["ano"] = filtro["DATA_DT"].dt.year
    filtro["mes"] = filtro["DATA_DT"].dt.month
    filtro["bairro"] = filtro["BAIRRO"].astype(str).str.strip().str.upper()

    todos_registros_estatisticas.append(filtro)

# Atualiza estatísticas em todas as pastas
if todos_registros_estatisticas:
    df_p = pd.concat(todos_registros_estatisticas, ignore_index=True)
    agrupado = df_p.groupby(["bairro", "ano", "mes"]).agg(
        transacoes=("VALOR_F", "count"),
        m2_medio=("m2", "mean"),
        ticket_medio=("VALOR_F", "mean"),
        area_media=("AREA_F", "mean")
    ).reset_index()

    historico = []
    for _, r in agrupado.iterrows():
        historico.append({
            "bairro": r["bairro"],
            "ano": int(r["ano"]),
            "mes": int(r["mes"]),
            "m2_medio": round(r["m2_medio"], 2),
            "ticket_medio": round(r["ticket_medio"], 2),
            "transacoes": int(r["transacoes"]),
            "area_media": round(r["area_media"], 1)
        })

    json_saida = {
        "bairros": sorted(df_p["bairro"].unique().tolist()),
        "anos_disponiveis": sorted([int(a) for a in df_p["ano"].dropna().unique().tolist()]),
        "historico_mensal": historico
    }

    for pasta_json in [".", "docs", "data", "docs/data"]:
        if os.path.exists(pasta_json):
            c_json = os.path.join(pasta_json, "estatisticas-poa.json")
            with open(c_json, "w", encoding="utf-8") as f:
                json.dump(json_saida, f, ensure_ascii=False, indent=2)
            print(f"Atualizado: {c_json}")

print("\nProcesso concluído com sucesso!")
