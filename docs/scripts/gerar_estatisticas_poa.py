#!/usr/bin/env python3
"""Rebuild Porto Alegre 2026 statistics without modifying source CSVs."""
import csv
import json
import re
from collections import defaultdict
from datetime import date, datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "docs" / "data"
CSV_PATH = DATA_DIR / "itbi-2026.csv"
JSON_PATH = DATA_DIR / "estatisticas-porto-alegre.json"
COLUNAS = [
    "DATA_ESTIMATIVA", "DATA_PAGAMENTO", "VALOR", "PERC", "USO",
    "LOGRADOURO", "NUMERO", "UNIDADE", "COMPL", "BAIRRO", "CEP",
    "AREA_TERRENO", "AREA_TOTAL", "AREA_PRIV", "ANO_CONST", "MATRICULA",
    "CARTORIO", "SITUACAO",
]
FAIXAS = ("0-50", "50-100", "100-200", "200-500", "500+")
METODOLOGIA = (
    "Consolidação conservadora de cadeias Retificada por unidade/ano em até "
    "120 dias; filtros de qualidade."
)


def normalizar(texto):
    return " ".join(str(texto or "").strip().upper().split())


def numero(valor):
    """Parse Brazilian or dot-decimal numbers safely."""
    s = str(valor or "").strip().replace("R$", "").replace(" ", "")
    if not s:
        return None
    if "," in s and "." in s:
        if s.rfind(",") > s.rfind("."):
            s = s.replace(".", "").replace(",", ".")
        else:
            s = s.replace(",", "")
    elif "," in s:
        s = s.replace(".", "").replace(",", ".")
    elif s.count(".") > 1:
        s = s.replace(".", "")
    try:
        return float(s)
    except ValueError:
        return None


def data_pagamento(valor):
    s = str(valor or "").strip()
    for fmt in ("%d/%m/%Y", "%Y-%m-%d", "%d-%m-%Y", "%d/%m/%y"):
        try:
            return datetime.strptime(s, fmt).date()
        except ValueError:
            pass
    return None


def chave_unidade(row, indice):
    """Build a cautious property key; do not merge unidentifiable units."""
    matricula = normalizar(row.get("MATRICULA"))
    unidade = normalizar(row.get("UNIDADE"))
    complemento = normalizar(row.get("COMPL"))
    logradouro = normalizar(row.get("LOGRADOURO"))
    numero_endereco = normalizar(row.get("NUMERO"))
    if (unidade or complemento) and logradouro and numero_endereco:
        return "END|{}|{}|{}|{}".format(
            logradouro, numero_endereco, unidade, complemento
        )
    if matricula:
        return "MAT|" + matricula
    return "UNICO|{}".format(indice)


def pontuacao(row):
    situacao = normalizar(row.get("SITUACAO"))
    score = 0
    if "RETIFIC" in situacao:
        score += 500
    elif "PAGA" in situacao or "IMPRESSA" in situacao:
        score += 400
    elif "RECALCUL" in situacao:
        score += 300
    elif "CALCUL" in situacao:
        score += 200
    elif "ESTIMATIVA" in situacao:
        score += 100
    if row.get("data"):
        score += 50
    if numero(row.get("VALOR")) is not None and numero(row.get("AREA_PRIV")) is not None:
        score += 5
    return score


def carregar_registros(caminho):
    registros = []
    with caminho.open("r", encoding="utf-8-sig", errors="replace", newline="") as f:
        reader = csv.reader(f, delimiter=";", quotechar="'")
        for indice, campos in enumerate(reader):
            if not campos or not any(str(x).strip() for x in campos):
                continue
            if indice == 0 and ("DATA" in campos[0].upper() or "ESTIMATIVA" in campos[0].upper()):
                continue
            campos = campos[:18] + [""] * max(0, 18 - len(campos))
            row = dict(zip(COLUNAS, campos[:18]))
            row["data"] = data_pagamento(row.get("DATA_PAGAMENTO"))
            if row["data"] is None or row["data"].year != 2026:
                continue
            row["_indice"] = indice
            row["_chave"] = chave_unidade(row, indice)
            row["_score"] = pontuacao(row)
            row["_valor"] = numero(row.get("VALOR"))
            row["_area"] = numero(row.get("AREA_PRIV"))
            registros.append(row)
    return registros


def consolidar_cadeias(registros):
    """Keep the best record for an identifiable unit/year within 120 days."""
    grupos = defaultdict(list)
    for row in registros:
        grupos[(row["_chave"], row["data"].year)].append(row)

    resultado = []
    janela = timedelta(days=120)
    for grupo in grupos.values():
        grupo.sort(key=lambda r: (r["data"], r["_indice"]))
        cadeia = []
        inicio = None
        for row in grupo:
            if not cadeia or row["data"] - inicio <= janela:
                if not cadeia:
                    inicio = row["data"]
                cadeia.append(row)
            else:
                resultado.append(max(cadeia, key=lambda r: (r["_score"], r["data"], r["_indice"])))
                cadeia = [row]
                inicio = row["data"]
        if cadeia:
            resultado.append(max(cadeia, key=lambda r: (r["_score"], r["data"], r["_indice"])))
    return resultado


def elegivel(row):
    valor, area = row["_valor"], row["_area"]
    if valor is None or area is None or valor < 30000 or area < 20:
        return False
    m2 = valor / area
    if not 1200 <= m2 <= 35000:
        return False
    uso_complemento = normalizar(row.get("USO")) + " " + normalizar(row.get("COMPL"))
    return not re.search(r"BOX|GARAGEM|VAGA|ESTACIONAMENTO|ESTAC", uso_complemento)


def agregar(registros):
    grupos = defaultdict(list)
    for row in registros:
        if not elegivel(row):
            continue
        bairro = normalizar(row.get("BAIRRO")) or "NÃO INFORMADO"
        grupos[(row["data"].year, row["data"].month, bairro)].append(row)

    historico = []
    for (ano, mes, bairro), linhas in sorted(grupos.items()):
        soma_valor = sum(r["_valor"] for r in linhas)
        soma_area = sum(r["_area"] for r in linhas)
        qtd_area = sum(1 for r in linhas if r["_area"] > 0)
        faixas = {f: 0 for f in FAIXAS}
        for row in linhas:
            area = row["_area"]
            if area < 50:
                faixas["0-50"] += 1
            elif area < 100:
                faixas["50-100"] += 1
            elif area < 200:
                faixas["100-200"] += 1
            elif area < 500:
                faixas["200-500"] += 1
            else:
                faixas["500+"] += 1
        transacoes = len(linhas)
        historico.append({
            "ano": int(ano), "mes": int(mes), "bairro": bairro,
            "transacoes": transacoes,
            "soma_valor": round(soma_valor, 2),
            "soma_area": round(soma_area, 2),
            "qtd_area": int(qtd_area),
            "ticket_medio": round(soma_valor / transacoes, 2) if transacoes else 0,
            "m2_medio": round(soma_valor / soma_area, 2) if soma_area else 0,
            "area_media": round(soma_area / qtd_area, 1) if qtd_area else 0,
            "faixas_metragem": faixas,
        })
    return historico


def main():
    if not CSV_PATH.exists() or CSV_PATH.stat().st_size < 1000:
        raise SystemExit(f"CSV ausente ou pequeno demais: {CSV_PATH}")
    if not JSON_PATH.exists():
        raise SystemExit(f"JSON base ausente: {JSON_PATH}")

    with JSON_PATH.open("r", encoding="utf-8") as f:
        anterior = json.load(f)
    historico_antigo = anterior.get("historico_mensal", [])
    periodos_antigos = [(int(x["ano"]), int(x["mes"])) for x in historico_antigo]
    ultimo_anterior = max(periodos_antigos, default=(0, 0))

    registros = carregar_registros(CSV_PATH)
    consolidados = consolidar_cadeias(registros)
    historico_2026 = agregar(consolidados)
    periodo_novo = max(((x["ano"], x["mes"]) for x in historico_2026), default=(0, 0))

    if periodo_novo <= ultimo_anterior:
        print(
            "Nenhum período novo encontrado no CSV de Porto Alegre. "
            f"Último período no JSON: {ultimo_anterior}; no CSV após filtros: {periodo_novo}. "
            "JSON preservado para evitar substituir dados bons por uma base incompleta."
        )
        return

    # Preserve 2020-2025; regenerate 2026 coherently using the updated CSV.
    historico = [x for x in historico_antigo if int(x.get("ano", 0)) != 2026]
    historico.extend(historico_2026)
    historico.sort(key=lambda x: (int(x["ano"]), int(x["mes"]), str(x["bairro"])))

    bairros = sorted(set(anterior.get("bairros", [])) | {x["bairro"] for x in historico_2026})
    anos = sorted({int(x["ano"]) for x in historico})
    saida = {
        "cidade": anterior.get("cidade", "Porto Alegre - RS"),
        "uf": anterior.get("uf", "RS"),
        "metodologia": METODOLOGIA,
        "bairros": bairros,
        "anos_disponiveis": anos,
        "historico_mensal": historico,
    }
    temporario = JSON_PATH.with_suffix(".json.tmp")
    with temporario.open("w", encoding="utf-8") as f:
        json.dump(saida, f, ensure_ascii=False, indent=2)
        f.write("\n")
    temporario.replace(JSON_PATH)
    print(
        f"Estatísticas de Porto Alegre atualizadas. Período mais recente: "
        f"{periodo_novo[1]:02d}/{periodo_novo[0]}; "
        f"registros agregados de 2026: {len(historico_2026)}; "
        f"transações consideradas: {sum(x['transacoes'] for x in historico_2026):,}."
    )


if __name__ == "__main__":
    main()
