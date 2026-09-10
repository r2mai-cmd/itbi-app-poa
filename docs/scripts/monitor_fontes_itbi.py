#!/usr/bin/env python3
import hashlib
import json
import os
import re
import sys
import urllib.request
from datetime import datetime, timezone

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
CONFIG = os.path.join(os.path.dirname(ROOT), ".github", "itbi-fontes.json")
STATE = os.path.join(ROOT, "data", "monitor", "estado.json")
ALERT = os.path.join(ROOT, "data", "monitor", "alerta.md")

HEADERS = {"User-Agent": "itbidata-monitor/1.0 (+https://itbidata.com.br)"}


def baixar(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=45) as r:
        body = r.read()
        headers = {k.lower(): v for k, v in r.headers.items()}
        return body, headers


def sha256(body):
    return hashlib.sha256(body).hexdigest()


def fingerprint_ckan(body):
    data = json.loads(body.decode("utf-8"))
    if not data.get("success"):
        raise RuntimeError("API CKAN retornou success=false")
    result = data.get("result", {})
    recursos = []
    for r in result.get("resources", []):
        # Mantemos apenas metadados que representam a versão do recurso.
        recursos.append({
            "id": r.get("id"),
            "name": r.get("name"),
            "format": r.get("format"),
            "url": r.get("url"),
            "hash": r.get("hash"),
            "size": r.get("size"),
            "last_modified": r.get("last_modified"),
            "created": r.get("created")
        })
    recursos.sort(key=lambda x: (str(x.get("name")), str(x.get("id"))))
    relevante = json.dumps(recursos, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return {
        "tipo": "ckan",
        "fingerprint": sha256(relevante),
        "recursos": recursos,
        "package_name": result.get("title") or result.get("name")
    }


def fingerprint_pagina(body, headers):
    texto = body.decode("utf-8", errors="replace")
    # O hash da página é deliberadamente usado aqui: em São Paulo o arquivo do
    # exercício corrente é atualizado dentro desta própria página.
    return {
        "tipo": "pagina",
        "fingerprint": sha256(body),
        "content_length": len(body),
        "last_modified_http": headers.get("last-modified"),
        "titulo": re.sub(r"\s+", " ", texto[:200]).strip()
    }


def carregar_estado():
    if not os.path.exists(STATE):
        return {}
    with open(STATE, "r", encoding="utf-8") as f:
        return json.load(f)


def salvar_estado(data):
    os.makedirs(os.path.dirname(STATE), exist_ok=True)
    with open(STATE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write("\n")


def main():
    with open(CONFIG, "r", encoding="utf-8") as f:
        fontes = json.load(f)

    anterior = carregar_estado()
    agora = datetime.now(timezone.utc).isoformat()
    atual = {}
    mudancas = []
    erros = []

    for chave, fonte in fontes.items():
        try:
            body, headers = baixar(fonte["url"])
            if fonte["tipo"] == "ckan":
                fp = fingerprint_ckan(body)
            else:
                fp = fingerprint_pagina(body, headers)
            atual[chave] = {
                "nome": fonte["nome"],
                "url": fonte["url"],
                "verificado_em": agora,
                **fp
            }
            if chave in anterior and anterior[chave].get("fingerprint") != fp.get("fingerprint"):
                mudancas.append((chave, fonte["nome"], anterior[chave], atual[chave]))
        except Exception as e:
            erros.append((fonte["nome"], fonte["url"], str(e)))
            if chave in anterior:
                atual[chave] = anterior[chave]
            else:
                atual[chave] = {"nome": fonte["nome"], "url": fonte["url"], "erro": str(e), "verificado_em": agora}

    salvar_estado(atual)

    # Primeira execução: cria a linha de base, sem alerta.
    if not anterior:
        if os.path.exists(ALERT):
            os.remove(ALERT)
        print("Linha de base criada. Nenhum alerta na primeira execução.")
        if erros:
            print("Avisos:")
            for nome, url, err in erros:
                print(f"- {nome}: {err} ({url})")
        return 0

    if mudancas or erros:
        linhas = [
            "## Monitor de fontes do itbidata",
            "",
            f"Verificação: {agora}",
            "",
        ]
        if mudancas:
            linhas += ["### Mudanças detectadas", ""]
            for _, nome, antigo, novo in mudancas:
                linhas += [
                    f"- **{nome}** — a fonte mudou.",
                    f"  - Antes: `{antigo.get('fingerprint', 'sem fingerprint')}`",
                    f"  - Agora: `{novo.get('fingerprint', 'sem fingerprint')}`",
                    f"  - Fonte: {novo.get('url')}",
                    ""
                ]
        if erros:
            linhas += ["### Fontes que não puderam ser verificadas", ""]
            for nome, url, err in erros:
                linhas += [f"- **{nome}** — `{err}` — {url}"]
            linhas.append("")
        linhas += [
            "> O monitor é apenas fiscalizador. Ele não altera nem publica os dados do site automaticamente.",
            "> Verifique a fonte oficial antes de atualizar a base do itbidata."
        ]
        os.makedirs(os.path.dirname(ALERT), exist_ok=True)
        with open(ALERT, "w", encoding="utf-8") as f:
            f.write("\n".join(linhas) + "\n")
        print("Mudança/erro detectado; alerta preparado.")
        return 2

    if os.path.exists(ALERT):
        os.remove(ALERT)
    print("Nenhuma mudança detectada nas fontes monitoradas.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
