(function () {
  "use strict";

  var cacheCsv = {};
  var dadosAtuais = [];
  var graficosInstanciados = {};

  var configCidades = {
    "porto-alegre": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-" + ano + ".csv"; },
      encoding: "utf-8"
    },
    "sao-paulo": {
      anos: [2026, 2025, 2024, 2023, 2022, 2020],
      getArquivo: function(ano) { return "data/itbi-sp-" + ano + ".csv"; },
      encoding: "utf-8",
      bairrosEmBreve: true // Desativa o mapeamento de bairros de SP e exibe "Em breve"
    },
    "belo-horizonte": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-bh-" + ano + ".csv"; },
      encoding: "utf-8"
    },
    "fortaleza": {
      anos: [2024],
      getArquivo: function() { return "data/itbi-fortaleza.csv"; },
      encoding: "iso-8859-1"
    },
    "recife": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-recife-" + ano + ".csv"; },
      encoding: "utf-8"
    }
  };

  function parseLinhaCsv(linha) {
    var resultado = [];
    var i = 0;
    var n = linha.length;
    while (i <= n) {
      if (i === n) {
        if (resultado.length === 0 || linha.charAt(linha.length - 1) === ";") {
          resultado.push("");
        }
        break;
      }
      if (linha.charAt(i) === "'") {
        var sb = "";
        i++;
        while (i < n) {
          if (linha.charAt(i) === "'") {
            if (i + 1 < n && linha.charAt(i + 1) === "'") {
              sb += "'";
              i += 2;
            } else {
              i++;
              break;
            }
          } else {
            sb += linha.charAt(i);
            i++;
          }
        }
        resultado.push(sb);
        while (i < n && linha.charAt(i) !== ";") i++;
        if (i < n) { i++; if (i === n) resultado.push(""); } else { break; }
      } else {
        var inicio = i;
        while (i < n && linha.charAt(i) !== ";") i++;
        resultado.push(linha.substring(inicio, i));
        if (i < n) { i++; if (i === n) resultado.push(""); } else { break; }
      }
    }
    return resultado;
  }

  function parseDataPoa(texto) {
    if (!texto) return null;
    var m = texto.trim().match(/^(\d{4})\/(\d{2})\/(\d{2})/);
    if (!m) return null;
    return { y: parseInt(m[1], 10), mo: parseInt(m[2], 10), d: parseInt(m[3], 10) };
  }

  function parseDataFortaleza(texto) {
    if (!texto) return null;
    var m = texto.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (!m) return null;
    return { d: parseInt(m[1], 10), mo: parseInt(m[2], 10), y: parseInt(m[3], 10) };
  }

  function processarCsvGeral(texto, anoPadrao, ehFortaleza, ehSaoPaulo) {
    var linhas = texto.split("\n");
    var registros = [];
    for (var i = 1; i < linhas.length; i++) {
      var linha = linhas[i].replace(/\r$/, "");
      if (!linha.trim()) continue;
      try {
        var campos = ehFortaleza ? linha.split(";") : parseLinhaCsv(linha);
        if (campos.length < (ehFortaleza ? 31 : 18)) continue;

        var dataEst, dataPag, base, areaPriv, bairro;

        if (ehFortaleza) {
          var anoCsv = campos[4] ? parseInt(campos[4], 10) : 2024;
          dataEst = parseDataFortaleza(campos[5]);
          dataPag = parseDataFortaleza(campos[24]);
          base = parseFloat(campos[27].replace(",", "."));
          var areaConstr = parseFloat(campos[15].replace(",", "."));
          areaPriv = !isNaN(areaConstr) && areaConstr > 0 ? areaConstr : null;
          bairro = campos[7] ? campos[7].trim().toUpperCase() : "N
