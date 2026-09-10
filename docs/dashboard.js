(function () {
  "use strict";

  var cacheCsv = {};
  var dadosAtuais = [];
  var graficosInstanciados = {};

  var configCidades = {
    "poa": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-" + ano + ".csv"; },
      processar: processarCsvPoa
    },
    "sao-paulo": {
      anos: [2026, 2025, 2024, 2023, 2022, 2020],
      getArquivo: function(ano) { return "data/itbi-sp-" + ano + ".csv"; },
      processar: processarCsvSp
    },
    "belo-horizonte": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-bh-" + ano + ".csv"; },
      processar: processarCsvBh
    },
    "fortaleza": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-fortaleza.csv"; },
      processar: processarCsvFortalezaUnico
    },
    "recife": {
      anos: [2026, 2025, 2024, 2023, 2022, 2021, 2020],
      getArquivo: function(ano) { return "data/itbi-recife-" + ano + ".csv"; },
      processar: processarCsvRecife
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

  function processarCsvPoa(texto, anoPadrao) {
    var linhas = texto.split("\n");
    var registros = [];
    for (var i = 1; i < linhas.length; i++) {
      var linha = linhas[i].replace(/\r$/, "");
      if (!linha.trim()) continue;
      try {
        var campos = parseLinhaCsv(linha);
        if (campos.length < 18) continue;
        var dataEst = parseDataPoa(campos[0]);
        var dataPag = parseDataPoa(campos[1]);
        var dataRef = dataPag || dataEst;
        var base = parseFloat(campos[2]);
        var areaPriv = parseFloat(campos[13]);
        var bairro = campos[9] ? campos[9].trim().toUpperCase() : "NÃO INFORMADO";

        if (!isNaN(base) && base > 0 && dataRef) {
          registros.push({
            ano: dataRef.y || anoPadrao,
            mes: dataRef.mo,
            baseCalculo: base,
            areaPrivativa: !isNaN(areaPriv) && areaPriv > 0 ? areaPriv : null,
            bairro: bairro
          });
        }
      } catch (e) {}
    }
    return registros;
  }

  function processarCsvSp(texto, anoPadrao) { return processarCsvPoa(texto, anoPadrao); }
  function processarCsvBh(texto, anoPadrao) { return processarCsvPoa(texto, anoPadrao); }
  function processarCsvRecife(texto, anoPadrao) { return processarCsvPoa(texto, anoPadrao); }

  function processarCsvFortalezaUnico(texto) {
    var linhas = texto.split("\n");
    var registros = [];
    for (var i = 1; i < linhas.length; i++) {
      var linha = linhas[i].replace(/\r$/, "");
      if (!linha.trim()) continue;
      try {
        var campos = linha.split(";");
        if (campos.length < 31) continue;
        var anoCsv = campos[4] ? parseInt(campos[4], 10) : null;
        var dataEst = parseDataFortaleza(campos[5]);
        var dataPag = parseDataFortaleza(campos[24]);
        var dataRef = dataPag || dataEst;
        var base = parseFloat(campos[27].replace(",", "."));
        var bairro = campos[7] ? campos[7].trim().toUpperCase() : "NÃO INFORMADO";

        if (!isNaN(base) && base > 0 && dataRef) {
          registros.push({
            ano: dataRef.y || anoCsv || 2024,
            mes: dataRef.mo,
            baseCalculo: base,
            areaPrivativa: null,
            bairro: bairro
          });
        }
      } catch (e) {}
    }
    return registros;
  }

  var elCidade = document.getElementById("filtro-cidade");
  var elBairro = document.getElementById("filtro-bairro");
  var elAno = document.getElementById("filtro-ano");
  var elMes = document.getElementById("filtro-mes");

  var formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

  function carregarDadosCidade(cidadeKey) {
    var cfg = configCidades[cidadeKey];
    if (!cfg) return;

    var promessas = cfg.anos.map(function (ano) {
      var url = cfg.getArquivo(ano);
      if (cacheCsv[url]) return Promise.resolve(cacheCsv[url]);
      return fetch(url).then(function (resp) {
        if (!resp.ok) return "";
        return (cidadeKey === "fortaleza") ? resp.arrayBuffer().then(function(buf){ return new TextDecoder("iso-8859-1").decode(buf); }) : resp.text();
      }).then(function (texto) {
        cacheCsv[url] = texto;
        return texto;
      }).catch(function () { return ""; });
    });

    Promise.all(promessas).then(function (textos) {
      var todosRegs = [];
      textos.forEach(function (txt, idx) {
        if (txt) {
          var regs = cfg.processar(txt, cfg.anos[idx]);
          todosRegs = todosRegs.concat(regs);
        }
      });
      dadosAtuais = todosRegs;
      atualizarFiltrosAnosEBairros();
      processarDashboard();
    });
  }

  function atualizarFiltrosAnosEBairros() {
    var anosSet = {};
    var bairrosSet = {};

    dadosAtuais.forEach(function (r) {
      if (r.ano) anosSet[r.ano] = true;
      if (r.bairro) bairrosSet[r.bairro] = true;
    });

    var anosArr = Object.keys(anosSet).sort(function(a,b){ return b - a; });
    elAno.innerHTML = '<option value="todos">Todos os Anos</option>';
    anosArr.forEach(function (ano) {
      var opt = document.createElement("option");
      opt.value = ano;
      opt.textContent = ano;
      elAno.appendChild(opt);
    });
    if (anosArr.length > 0) {
      elAno.value = anosArr[0];
    }

    var bairrosArr = Object.keys(bairrosSet).sort();
    elBairro.innerHTML = '<option value="todos">Todos os Bairros</option>';
    bairrosArr.forEach(function (b) {
      var opt = document.createElement("option");
      opt.value = b;
      opt.textContent = b;
      elBairro.appendChild(opt);
    });
    elBairro.value = "todos";
  }

  function filtrarDados() {
    var bairroSel = elBairro.value;
    var anoSel = elAno.value;
    var mesSel = elMes.value;

    return dadosAtuais.filter(function (r) {
      var matchBairro = (bairroSel === "todos" || r.bairro === bairroSel);
      var matchAno = (anoSel === "todos" || String(r.ano) === anoSel);
      var matchMes = (mesSel === "todos" || String(r.mes) === mesSel);
      return matchBairro && matchAno && matchMes;
    });
  }

  function processarDashboard() {
    var filtrados = filtrarDados();

    var totalTransacoes = filtrados.length;
    var somaValor = 0;
    var somaM2 = 0;
    var qtdM2 = 0;

    filtrados.forEach(function (r) {
      somaValor += r.baseCalculo;
      if (r.areaPrivativa && r.areaPrivativa > 0) {
        somaM2 += (r.baseCalculo / r.areaPrivativa);
        qtdM2++;
      }
    });

    var ticketMedio = totalTransacoes > 0 ? somaValor / totalTransacoes : 0;
    var m2Medio = qtdM2 > 0 ? somaM2 / qtdM2 : 0;

    document.getElementById("kpi-transacoes").textContent = totalTransacoes.toLocaleString("pt-BR");
    document.getElementById("kpi-ticket").textContent = formatoMoeda.format(ticketMedio);
    document.getElementById("kpi-m2").textContent = formatoMoeda.format(m2Medio);

    atualizarGraficos(filtrados);
  }

  function destruirGrafico(id) {
    if (graficosInstanciados[id]) {
      graficosInstanciados[id].destroy();
      delete graficosInstanciados[id];
    }
  }

  function atualizarGraficos(filtrados) {
    destruirGrafico("graficoEvolucao");
    var mesesNomes = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];
    var m2PorMes = new Array(12).fill(0);
    var qtdPorMes = new Array(12).fill(0);

    filtrados.forEach(function (r) {
      if (r.mes >= 1 && r.mes <= 12 && r.areaPrivativa) {
        m2PorMes[r.mes - 1] += (r.baseCalculo / r.areaPrivativa);
        qtdPorMes[r.mes - 1]++;
      }
    });

    var dadosEvolucao = m2PorMes.map(function (val, idx) {
      return qtdPorMes[idx] > 0 ? val / qtdPorMes[idx] : 0;
    });

    var ctxEvolucao = document.getElementById("graficoEvolucao").getContext("2d");
    graficosInstanciados["graficoEvolucao"] = new Chart(ctxEvolucao, {
      type: 'line',
      data: {
        labels: mesesNomes,
        datasets: [{
          label: 'M² Médio (R$)',
          data: dadosEvolucao,
          borderColor: '#F0913A',
          backgroundColor: 'rgba(240, 145, 58, 0.1)',
          fill: true,
          tension: 0.3
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });

    destruirGrafico("graficoRanking");
    var bairrosM2 = {};
    filtrados.forEach(function (r) {
      if (r.areaPrivativa && r.bairro) {
        if (!bairrosM2[r.bairro]) bairrosM2[r.bairro] = { soma: 0, qtd: 0 };
        bairrosM2[r.bairro].soma += (r.baseCalculo / r.areaPrivativa);
        bairrosM2[r.bairro].qtd++;
      }
    });

    var rankingM2Arr = Object.keys(bairrosM2).map(function (b) {
      return { bairro: b, media: bairrosM2[b].soma / bairrosM2[b].qtd };
    }).sort(function(a,b){ return b.media - a.media; }).slice(0, 10);

    var ctxRanking = document.getElementById("graficoRanking").getContext("2d");
    graficosInstanciados["graficoRanking"] = new Chart(ctxRanking, {
      type: 'bar',
      data: {
        labels: rankingM2Arr.map(function(x){ return x.bairro; }),
        datasets: [{
          label: 'Preço Médio m² (R$)',
          data: rankingM2Arr.map(function(x){ return x.media; }),
          backgroundColor: '#4FBFB8'
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, indexAxis: 'y' }
    });

    destruirGrafico("graficoHistorico");
    var anosM2 = {};
    dadosAtuais.forEach(function (r) {
      if (r.ano && r.areaPrivativa) {
        if (!anosM2[r.ano]) anosM2[r.ano] = { soma: 0, qtd: 0 };
        anosM2[r.ano].soma += (r.baseCalculo / r.areaPrivativa);
        anosM2[r.ano].qtd++;
      }
    });
    var anosOrdenados = Object.keys(anosM2).sort(function(a,b){ return a - b; });
    var dadosHistorico = anosOrdenados.map(function(ano){
      return anosM2[ano].soma / anosM2[ano].qtd;
    });

    var ctxHistorico = document.getElementById("graficoHistorico").getContext("2d");
    graficosInstanciados["graficoHistorico"] = new Chart(ctxHistorico, {
      type: 'bar',
      data: {
        labels: anosOrdenados,
        datasets: [{
          label: 'M² Histórico (R$)',
          data: dadosHistorico,
          backgroundColor: '#388e9c'
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });

    destruirGrafico("graficoMetragem");
    var faixaEtaria = { "Até 50m²": 0, "50-80m²": 0, "80-120m²": 0, "Acima 120m²": 0 };
    filtrados.forEach(function (r) {
      if (r.areaPrivativa) {
        if (r.areaPrivativa <= 50) faixaEtaria["Até 50m²"]++;
        else if (r.areaPrivativa <= 80) faixaEtaria["50-80m²"]++;
        else if (r.areaPrivativa <= 120) faixaEtaria["80-120m²"]++;
        else faixaEtaria["Acima 120m²"]++;
      }
    });

    var ctxMetragem = document.getElementById("graficoMetragem").getContext("2d");
    graficosInstanciados["graficoMetragem"] = new Chart(ctxMetragem, {
      type: 'doughnut',
      data: {
        labels: Object.keys(faixaEtaria),
        datasets: [{
          data: Object.values(faixaEtaria),
          backgroundColor: ['#F0913A', '#4FBFB8', '#388e9c', '#41464A']
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });

    destruirGrafico("graficoVolumeMes");
    var volumeMes = new Array(12).fill(0);
    filtrados.forEach(function (r) {
      if (r.mes >= 1 && r.mes <= 12) volumeMes[r.mes - 1]++;
    });

    var ctxVolumeMes = document.getElementById("graficoVolumeMes").getContext("2d");
    graficosInstanciados["graficoVolumeMes"] = new Chart(ctxVolumeMes, {
      type: 'bar',
      data: {
        labels: mesesNomes,
        datasets: [{
          label: 'Quantidade de Transações',
          data: volumeMes,
          backgroundColor: '#D9762A'
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });

    destruirGrafico("graficoRankingVolume");
    var bairrosVol = {};
    filtrados.forEach(function (r) {
      if (r.bairro) {
        bairrosVol[r.bairro] = (bairrosVol[r.bairro] || 0) + 1;
      }
    });
    var rankingVolArr = Object.keys(bairrosVol).map(function (b) {
      return { bairro: b, total: bairrosVol[b] };
    }).sort(function(a,b){ return b.total - a.total; }).slice(0, 10);

    var ctxRankingVol = document.getElementById("graficoRankingVolume").getContext("2d");
    graficosInstanciados["graficoRankingVolume"] = new Chart(ctxRankingVol, {
      type: 'bar',
      data: {
        labels: rankingVolArr.map(function(x){ return x.bairro; }),
        datasets: [{
          label: 'Total de Vendas',
          data: rankingVolArr.map(function(x){ return x.total; }),
          backgroundColor: '#4FBFB8'
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, indexAxis: 'y' }
    });
  }

  if (elCidade) elCidade.addEventListener("change", function () { carregarDadosCidade(this.value); });
  if (elBairro) elBairro.addEventListener("change", processarDashboard);
  if (elAno) elAno.addEventListener("change", processarDashboard);
  if (elMes) elMes.addEventListener("change", processarDashboard);

  carregarDadosCidade(elCidade ? elCidade.value : "poa");

})();
