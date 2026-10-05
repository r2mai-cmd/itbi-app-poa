(function () {
  "use strict";

  var catalog = null;
  var dadosAtuais = null;
  var graficosInstanciados = {};
  var elCidade = document.getElementById("filtro-cidade");
  var elBairro = document.getElementById("filtro-bairro");
  var elAno = document.getElementById("filtro-ano");
  var elMes = document.getElementById("filtro-mes");
  var cache = {};
  var moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  var meses = ["Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"];
  var bins = ["0-50","50-100","100-200","200-500","500+"];

  function destruir(id) {
    if (graficosInstanciados[id]) {
      graficosInstanciados[id].destroy();
      delete graficosInstanciados[id];
    }
  }

  function somar(linhas) {
    var r = { transacoes: 0, soma_valor: 0, soma_area: 0, qtd_area: 0, faixas_metragem: {} };
    bins.forEach(function (b) { r.faixas_metragem[b] = 0; });
    linhas.forEach(function (x) {
      r.transacoes += Number(x.transacoes) || 0;
      r.soma_valor += Number(x.soma_valor) || 0;
      r.soma_area += Number(x.soma_area) || 0;
      r.qtd_area += Number(x.qtd_area) || 0;
      bins.forEach(function (b) { r.faixas_metragem[b] += Number((x.faixas_metragem || {})[b]) || 0; });
    });
    r.ticket_medio = r.transacoes ? r.soma_valor / r.transacoes : 0;
    r.m2_medio = r.soma_area ? r.soma_valor / r.soma_area : 0;
    r.area_media = r.qtd_area ? r.soma_area / r.qtd_area : 0;
    return r;
  }

  function linhasFiltradas(ignorarBairro) {
    if (!dadosAtuais) return [];
    var b = elBairro.value, a = elAno.value, m = elMes.value;
    return dadosAtuais.historico_mensal.filter(function (x) {
      return (ignorarBairro || b === "todos" || x.bairro === b) &&
        (a === "todos" || String(x.ano) === a) &&
        (m === "todos" || String(x.mes) === m);
    });
  }

  function preencherFiltros() {
    var bairros = (dadosAtuais.bairros || []).slice().sort(function (a,b) { return a.localeCompare(b, "pt-BR"); });
    elBairro.innerHTML = '<option value="todos">Todos os Bairros</option>';
    bairros.forEach(function (b) {
      var o = document.createElement("option"); o.value = b; o.textContent = b; elBairro.appendChild(o);
    });
    elBairro.value = "todos";

    elAno.innerHTML = '<option value="todos">Todos os Anos</option>';
    (dadosAtuais.anos_disponiveis || []).slice().sort(function(a,b){return b-a;}).forEach(function(y){
      var o=document.createElement("option"); o.value=y; o.textContent=y; elAno.appendChild(o);
    });
    if (dadosAtuais.anos_disponiveis && dadosAtuais.anos_disponiveis.length) elAno.value=String(Math.max.apply(null,dadosAtuais.anos_disponiveis));
  }

  function atualizar() {
    if (!dadosAtuais) return;
    var filtrados = linhasFiltradas(false);
    var kpi = somar(filtrados);
    document.getElementById("kpi-transacoes").textContent = kpi.transacoes.toLocaleString("pt-BR");
    document.getElementById("kpi-ticket").textContent = moeda.format(kpi.ticket_medio);
    document.getElementById("kpi-m2").textContent = moeda.format(kpi.m2_medio);
    var nome = dadosAtuais.cidade || "";
    document.getElementById("titulo-grafico-evolucao").textContent = "Evolução Mensal do Valor do m² — " + nome;
    document.getElementById("titulo-grafico-historico").textContent = "Evolução Histórica Anual do m² — " + nome;
    desenharEvolucao(); desenharRanking(); desenharHistorico(); desenharMetragem(); desenharVolumeMes(); desenharRankingVolume();
  }

  function desenharEvolucao() {
    destruir("graficoEvolucao");
    var ano = elAno.value;
    if (ano === "todos") {
      var anos = (dadosAtuais.anos_disponiveis || []).slice().sort(function(a,b){return b-a;});
      ano = anos.length ? String(anos[0]) : "todos";
    }
    var linhas = dadosAtuais.historico_mensal.filter(function(x){ return String(x.ano)===ano && (elBairro.value==="todos" || x.bairro===elBairro.value); });
    var vals = [];
    for(var m=1;m<=12;m++) vals.push(somar(linhas.filter(function(x){return x.mes===m;})).m2_medio || null);
    var ctx=document.getElementById("graficoEvolucao").getContext("2d");
    graficosInstanciados.graficoEvolucao=new Chart(ctx,{type:"line",data:{labels:meses,datasets:[{label:"R$/m²",data:vals,borderColor:"#F0913A",backgroundColor:"rgba(240,145,58,.10)",tension:.25,spanGaps:true,fill:true}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true}},scales:{y:{beginAtZero:false,ticks:{callback:function(v){return "R$ "+Number(v).toLocaleString("pt-BR");}}}}}});
  }

  function desenharRanking() {
    destruir("graficoRanking");
    var linhas=linhasFiltradas(true), mapa={};
    linhas.forEach(function(x){ if(x.bairro!=="NÃO INFORMADO"){ if(!mapa[x.bairro]) mapa[x.bairro]=[]; mapa[x.bairro].push(x); }});
    var arr=Object.keys(mapa).map(function(b){var s=somar(mapa[b]);return {b:b,v:s.m2_medio,t:s.transacoes};}).filter(function(x){return x.t>0;}).sort(function(a,b){return b.v-a.v;}).slice(0,10).reverse();
    var ctx=document.getElementById("graficoRanking").getContext("2d"); graficosInstanciados.graficoRanking=new Chart(ctx,{type:"bar",data:{labels:arr.map(function(x){return x.b;}),datasets:[{label:"R$/m²",data:arr.map(function(x){return x.v;})}]},options:{indexAxis:"y",responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true}},scales:{x:{ticks:{callback:function(v){return "R$ "+Number(v).toLocaleString("pt-BR");}}}}}});
  }

  function desenharHistorico() {
    destruir("graficoHistorico");
    var linhas=dadosAtuais.historico_mensal.filter(function(x){return elBairro.value==="todos"||x.bairro===elBairro.value;}), mapa={}; linhas.forEach(function(x){(mapa[x.ano]||(mapa[x.ano]=[])).push(x);});
    var anos=Object.keys(mapa).sort(function(a,b){return a-b;}), vals=anos.map(function(y){return somar(mapa[y]).m2_medio||null;});
    var ctx=document.getElementById("graficoHistorico").getContext("2d"); graficosInstanciados.graficoHistorico=new Chart(ctx,{type:"line",data:{labels:anos,datasets:[{label:"R$/m²",data:vals,borderColor:"#4FBFB8",backgroundColor:"rgba(79,191,184,.10)",tension:.2,spanGaps:true,fill:true}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true}},scales:{y:{beginAtZero:false,ticks:{callback:function(v){return "R$ "+Number(v).toLocaleString("pt-BR");}}}}}});
  }

  function desenharMetragem() {
    destruir("graficoMetragem"); var s=somar(linhasFiltradas(false));
    var ctx=document.getElementById("graficoMetragem").getContext("2d"); graficosInstanciados.graficoMetragem=new Chart(ctx,{type:"doughnut",data:{labels:bins,datasets:[{data:bins.map(function(b){return s.faixas_metragem[b];})}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{position:"bottom"}}}});
  }

  function desenharVolumeMes() {
    destruir("graficoVolumeMes");
    var ano=elAno.value; if(ano==="todos"){var ys=dadosAtuais.anos_disponiveis||[];ano=ys.length?String(Math.max.apply(null,ys)):"todos";}
    var linhas=dadosAtuais.historico_mensal.filter(function(x){return String(x.ano)===ano && (elBairro.value==="todos"||x.bairro===elBairro.value);});
    var vals=[];for(var m=1;m<=12;m++) vals.push(somar(linhas.filter(function(x){return x.mes===m;})).transacoes);
    var ctx=document.getElementById("graficoVolumeMes").getContext("2d");graficosInstanciados.graficoVolumeMes=new Chart(ctx,{type:"bar",data:{labels:meses,datasets:[{label:"Transações",data:vals}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true}}}});
  }

  function desenharRankingVolume() {
    destruir("graficoRankingVolume"); var linhas=linhasFiltradas(true), mapa={};
    linhas.forEach(function(x){if(x.bairro!=="NÃO INFORMADO"){(mapa[x.bairro]||(mapa[x.bairro]=[])).push(x);}});
    var arr=Object.keys(mapa).map(function(b){return {b:b,v:somar(mapa[b]).transacoes};}).sort(function(a,b){return b.v-a.v;}).slice(0,10).reverse();
    var ctx=document.getElementById("graficoRankingVolume").getContext("2d");graficosInstanciados.graficoRankingVolume=new Chart(ctx,{type:"bar",data:{labels:arr.map(function(x){return x.b;}),datasets:[{label:"Transações",data:arr.map(function(x){return x.v;})}]},options:{indexAxis:"y",responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true}}}});
  }

  function formatarDataAtualizacao(valor) {
    if (valor === null || valor === undefined || valor === "") return null;

    if (typeof valor === "number") {
      var dNum = new Date(valor < 10000000000 ? valor * 1000 : valor);
      return isNaN(dNum.getTime()) ? null : dNum.toLocaleDateString("pt-BR");
    }

    var s = String(valor).trim();
    if (!s) return null;

    var m = s.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ].*)?$/);
    if (m) return m[3] + "/" + m[2] + "/" + m[1];

    m = s.match(/^(\d{2})[\/-](\d{2})[\/-](\d{4})$/);
    if (m) return m[1] + "/" + m[2] + "/" + m[3];

    var d = new Date(s);
    return isNaN(d.getTime()) ? null : d.toLocaleDateString("pt-BR");
  }

  function procurarDataAtualizacao(obj, cidade, nivel) {
    if (!obj || nivel > 8) return null;

    var chaves = [
      "ultima_atualizacao", "ultimaAtualizacao", "data_atualizacao",
      "dataAtualizacao", "atualizado_em", "atualizadoEm",
      "updated_at", "updatedAt", "update_date", "updateDate",
      "gerado_em", "geradoEm", "generated_at", "generatedAt",
      "data_fonte", "dataFonte", "data_publicacao", "dataPublicacao"
    ];

    if (Array.isArray(obj)) {
      for (var i = 0; i < obj.length; i++) {
        var a = procurarDataAtualizacao(obj[i], cidade, nivel + 1);
        if (a) return a;
      }
      return null;
    }

    if (typeof obj !== "object") return null;

    // Primeiro: chaves explícitas de atualização.
    for (var p = 0; p < chaves.length; p++) {
      if (Object.prototype.hasOwnProperty.call(obj, chaves[p])) {
        var d1 = formatarDataAtualizacao(obj[chaves[p]]);
        if (d1) return d1;
      }
    }

    var keys = Object.keys(obj);

    // Segundo: chaves cujo nome indica atualização/fonte.
    for (var k = 0; k < keys.length; k++) {
      if (/(atual|update|updated|gerad|generated|publica|fonte)/i.test(keys[k])) {
        var d2 = formatarDataAtualizacao(obj[keys[k]]);
        if (d2) return d2;
      }
    }

    // Terceiro: procura especificamente pelo identificador da cidade.
    if (cidade) {
      var alvo = String(cidade).toLowerCase();
      for (var c = 0; c < keys.length; c++) {
        if (String(keys[c]).toLowerCase() === alvo) {
          var d3 = procurarDataAtualizacao(obj[keys[c]], cidade, nivel + 1);
          if (d3) return d3;
        }
      }
    }

    // Quarto: percorre estruturas aninhadas.
    for (var j = 0; j < keys.length; j++) {
      var d4 = procurarDataAtualizacao(obj[keys[j]], cidade, nivel + 1);
      if (d4) return d4;
    }

    return null;
  }

  function atualizarTextoUltimaAtualizacao(cidade, dadosCidade) {
    var el = document.getElementById("ultima-atualizacao");
    if (!el) return;

    var data = procurarDataAtualizacao(catalog, cidade, 0) ||
               procurarDataAtualizacao(dadosCidade, cidade, 0);

    if (data) {
      el.innerHTML = "Última atualização da fonte: <strong>" + data + "</strong>";
      return;
    }

    // Se não houver metadado de atualização, não inventa uma data.
    // Usa o último período efetivamente existente na base como fallback.
    var ano = null, mes = null;
    if (dadosCidade && Array.isArray(dadosCidade.historico_mensal)) {
      dadosCidade.historico_mensal.forEach(function (x) {
        var y = Number(x.ano), m = Number(x.mes);
        if (!isFinite(y) || !isFinite(m)) return;
        if (ano === null || y > ano || (y === ano && m > mes)) {
          ano = y; mes = m;
        }
      });
    }

    if (ano !== null && mes !== null) {
      var nomesMeses = ["Janeiro","Fevereiro","Março","Abril","Maio","Junho",
        "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"];
      el.textContent = "Último período disponível na fonte: " +
        nomesMeses[mes - 1] + "/" + ano;
    } else {
      el.textContent = "Última atualização da fonte: informação não informada";
    }
  }

  function carregarCidade(key) {
    var url="data/estatisticas-"+key+".json";
    if(cache[url]) { dadosAtuais=cache[url]; atualizarTextoUltimaAtualizacao(key, dadosAtuais); preencherFiltros(); atualizar(); return; }
    document.getElementById("kpi-m2").textContent="Carregando...";
    fetch(url).then(function(r){if(!r.ok)throw new Error("Arquivo de estatísticas não encontrado");return r.json();}).then(function(d){cache[url]=d;dadosAtuais=d;atualizarTextoUltimaAtualizacao(key, dadosAtuais);preencherFiltros();atualizar();}).catch(function(e){console.error(e);document.getElementById("kpi-m2").textContent="Erro";document.getElementById("kpi-ticket").textContent="Erro";document.getElementById("kpi-transacoes").textContent="Erro";});
  }

  function iniciar() {
    fetch("estatisticas.json").then(function(r){if(!r.ok)throw new Error("Catálogo de estatísticas não encontrado");return r.json();}).then(function(c){catalog=c;carregarCidade(elCidade.value||"porto-alegre");}).catch(function(e){console.error(e);carregarCidade(elCidade.value||"porto-alegre");});
    elCidade.addEventListener("change",function(){carregarCidade(this.value);});
    elBairro.addEventListener("change",atualizar); elAno.addEventListener("change",atualizar); elMes.addEventListener("change",atualizar);
  }
  iniciar();
})();
