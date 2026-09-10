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
  var atualizacoes = null;

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
    var linhas=linhasFiltradas(false), mapa={}; linhas.forEach(function(x){(mapa[x.ano]||(mapa[x.ano]=[])).push(x);});
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

  function mostrarAtualizacao(key) {
    var el = document.getElementById("ultima-atualizacao");
    if (!el) return;
    var info = atualizacoes && atualizacoes[key];
    if (!info) {
      el.textContent = "Última atualização da fonte: informação indisponível";
      return;
    }
    el.innerHTML = "<strong>" + info.nome + "</strong> — fonte atualizada em " + info.fonte_atualizada_em;
  }

  function carregarCidade(key) {
    mostrarAtualizacao(key);
    var url="data/estatisticas-"+key+".json";
    if(cache[url]) { dadosAtuais=cache[url]; preencherFiltros(); atualizar(); return; }
    document.getElementById("kpi-m2").textContent="Carregando...";
    fetch(url).then(function(r){if(!r.ok)throw new Error("Arquivo de estatísticas não encontrado");return r.json();}).then(function(d){cache[url]=d;dadosAtuais=d;preencherFiltros();atualizar();}).catch(function(e){console.error(e);document.getElementById("kpi-m2").textContent="Erro";document.getElementById("kpi-ticket").textContent="Erro";document.getElementById("kpi-transacoes").textContent="Erro";});
  }

  function iniciar() {
    Promise.all([
      fetch("estatisticas.json").then(function(r){return r.json();}),
      fetch("data/atualizacoes.json").then(function(r){return r.json();})
    ]).then(function(resultados){
      catalog = resultados[0];
      atualizacoes = resultados[1];
      carregarCidade(elCidade.value||"porto-alegre");
    }).catch(function(e){
      console.error(e);
      fetch("estatisticas.json").then(function(r){return r.json();}).then(function(c){catalog=c;carregarCidade(elCidade.value||"porto-alegre");});
    });
    elCidade.addEventListener("change",function(){carregarCidade(this.value);});
    elBairro.addEventListener("change",atualizar); elAno.addEventListener("change",atualizar); elMes.addEventListener("change",atualizar);
  }
  iniciar();
})();
