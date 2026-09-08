import csv
import glob
import os
import pandas as pd

csv.field_size_limit(2147483647)

COLUNAS = [
    "DATA_ESTIMATIVA", "DATA_PAGAMENTO", "VALOR", "PERC", "USO", "LOGRADOURO",
    "NUMERO", "UNIDADE", "COMPL", "BAIRRO", "CEP", "AREA_TERRENO", "AREA_TOTAL",
    "AREA_PRIV", "ANO_CONST", "MATRICULA", "CARTORIO", "SITUACAO"
]

arquivos_poa = sorted(glob.glob("itbi-20*.csv"))

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

def pontuar_registro(row):
    s = str(row["SITUACAO"]).upper()
    pag = str(row["DATA_PAGAMENTO"]).strip()
    val = str(row["VALOR"]).strip().replace(",", ".")
    try:
        val_f = float(val)
    except:
        val_f = 0.0

    pts = 0
    # Maior valor numérico geralmente é a consolidação final (Ap + Box)
    pts += int(val_f / 10000) 
    
    if "IMPRESSA" in s or "PAGA" in s:
        pts += 1000
    if len(pag) >= 8: # Tem data de pagamento oficial
        pts += 500
    if "BOX" in str(row["COMPL"]).upper() or "VAGA" in str(row["COMPL"]).upper():
        pts += 200
    return pts

for caminho_csv in arquivos_poa:
    nome = os.path.basename(caminho_csv)
    if any(cid in nome for cid in ["-sp-", "-bh-", "-recife", "-fortaleza"]):
        continue

    print(f"\nDepurando rigorosamente {nome}...")
    df = carregar_csv_seguro(caminho_csv)
    total_antigo = len(df)

    # Chave estrita ignorando variações no complemento
    df["CHAVE_ESTRITA"] = (
        df["LOGRADOURO"].fillna("").str.strip().str.upper() + "_" +
        df["NUMERO"].fillna("").str.strip() + "_" +
        df["UNIDADE"].fillna("").str.strip()
    )

    # Converte data de pagamento para ordenar as mais recentes
    df["DT_PAG"] = pd.to_datetime(df["DATA_PAGAMENTO"], format="%d/%m/%Y", errors="coerce")
    df["SCORE"] = df.apply(pontuar_registro, axis=1)

    # Ordena para colocar a versão definitiva/paga no topo
    df = df.sort_values(by=["CHAVE_ESTRITA", "SCORE", "DT_PAG"], ascending=[True, False, False])

    # Remove duplicatas estritas da mesma unidade mantendo a melhor pontuação (a paga de 490k e 09/04)
    df_dedup = df.drop_duplicates(subset=["CHAVE_ESTRITA"], keep="first").copy()
    removidos = total_antigo - len(df_dedup)

    df_export = df_dedup[COLUNAS].copy()
    df_export.to_csv(caminho_csv, sep=";", index=False, header=True, encoding="utf-8")
    print(f"  [OK] {len(df_export):,} linhas mantidas ({removidos:,} guias preliminares removidas).")

print("\Limpeza profunda concluída!")
