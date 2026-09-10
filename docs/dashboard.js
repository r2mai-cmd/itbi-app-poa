(function () {
  "use strict";

  var dadosCidade = null;
  var graficos = {};
  var cidadeAtual = null;
  var moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  var meses = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

  var cidades = {
    "porto-alegre": { nome: "Porto Alegre - RS", arquivo: "data/estatisticas-porto-alegre.json" },
    "sao-paulo": { nome: "São Paulo - SP", arquivo: "data/estatisticas-sao-paulo.json" },
    "belo-horizonte": { nome: "Belo Horizonte - MG", arquivo: "data/estatisticas-belo-horizonte.json" },
    "fortaleza": { nome: "Fortaleza - CE", arquivo: "data/estatisticas-fortaleza.json" },
    "recife": { nome: "Recife - PE", arquivo: "data/estatisticas-recife.json" }
  };

  var elCidade = document.getElementById("filtro-cidade");
  var elBairro = document.getElementById("filtro-bairro");
  var elAno = document.getElementById("filtro-ano");
  var elMes = document.getElementById("filtro-mes");
  var elStatus = document.getElementById("status-estatisticas");

  function status(msg, erro) {
    if (!elStatus) return;
    elStatus.textContent = msg || "";
    elStatus.style.display = msg ? "block" : "none";
    elStatus.className = erro ? "status-estatisticas erro" : "status-estatisticas";
  }

  function destruir(id) {
    if (graficos[id]) {
      graficos[id].destroy();
      delete graficos[id];
    }
  }

  function destruirTodos() {
    Object.keys(graficos).forEach(destruir);
  }

  function somar(rows) {
    var r = { transacoes: 0, soma_valor: 0, soma_m2: 0, qtd_m2: 0, area_bins: [0,0,0,0] };
    rows.forEach(function (x) {
      r.transacoes += Number(x.transacoes) || 0;
      r.soma_valor += Number(x.soma_valor) || 0;
      r.soma_m2 += Number(x.soma_m2) || 0;
      r.qtd_m2 += Number(x.qtd_m2) || 0;
      for (var i=0;i<4;i++) r.area_bins[i] += Number((x.area_bins || [])[i]) || 0;
    });
    return r;
  }

  function linhasFiltradas(ignoreBairro) {
    if (!dadosCidade) return [];
    var bairro = elBairro.value;
    var ano = elAno.value;
    var mes = elMes.value;
    return dadosCidade.dados.filter(function (x) {
      return (ignoreBairro || bairro === "todos" || x.bairro === bairro) &&
             (ano === "todos" || String(x.ano) === ano) &&
             (mes === "todos" || String(x.mes) === mes);
    });
  }

  function preencherFiltros() {
    var anos = dadosCidade.anos_disponiveis || [];
    elAno.innerHTML = '<option value="todos">Todos os Anos</option>';
    anos.forEach(function (a) {
      var o=document.createElement("option"); o.value=a; o.textContent=a; elAno.appendChild(o);
    });
    elAno.value = anos.length ? String(anos[0]) : "todos";

    var counts = {};
    (dadosCidade.dados || []).forEach(function (x) {
      if (x.bairro && x.bairro !== "NÃO INFORMADO") counts[x.bairro] = (counts[x.bairro] || 0) + (Number(x.transacoes)||0);
    });
    var bairros = Object.keys(counts).sort(function(a,b){ return counts[b]-counts[a] || a.localeCompare(b); });
    // Nunca criar milhares de <option>. Em SP, o arquivo já limita aos 150 mais recorrentes.
    bairros = bairros.slice(0, 200);
    elBairro.innerHTML = '<option value="todos">Todos os Bairros</option>';
    bairros.forEach(function (b) {
      var o=document.createElement("option"); o.value=b; o.textContent=b; elBairro.appendChild(o);
    });
    elBairro.value="todos";
  }

  function atualizar() {
    var rows = linhasFiltradas(false);
    var total = somar(rows);
    var m2 = total.qtd_m2 ? total.soma_m2 / total.qtd_m2 : 0;
    var ticket = total.transacoes ? total.soma_valor / total.transacoes : 0;
    document.getElementById("kpi-transacoes").textContent = total.transacoes.toLocaleString("pt-BR");
    document.getElementById("kpi-ticket").textContent = moeda.format(ticket);
    document.getElementById("kpi-m2").textContent = moeda.format(m2);
    var titulo = cidadeAtual ? cidades[cidadeAtual].nome : "";
    document.getElementById("titulo-grafico-evolucao").textContent = "Evolução Mensal do Valor do m² — " + titulo;
    document.getElementById("titulo-grafico-historico").textContent = "Evolução Histórica Anual do m² — " + titulo;
    desenhar(rows);
  }

  function desenhar(rows) {
    var i;
    destruirTodos();

    // 1. Evolução mensal: se todos os anos estiverem selecionados, usa o ano mais recente.
    var anoSel = elAno.value;
    var anoMensal = anoSel === "todos" ? (dadosCidade.anos_disponiveis[0] || null) : Number(anoSel);
    var mensal = dadosCidade.dados.filter(function(x){
      return (!anoMensal || x.ano === anoMensal) &&
             (elBairro.value === "todos" || x.bairro === elBairro.value) &&
             (elMes.value === "todos" || x.mes === Number(elMes.value));
    });
    var sm=[0,0,0,0,0,0,0,0,0,0,0,0], qt=[0,0,0,0,0,0,0,0,0,0,0,0];
    mensal.forEach(function(x){ if(x.mes>=1&&x.mes<=12){sm[x.mes-1]+=Number(x.soma_m2)||0;qt[x.mes-1]+=Number(x.qtd_m2)||0;} });
    var mensalMed=sm.map(function(v,j){return qt[j]?v/qt[j]:null;});
    graficos.graficoEvolucao=new Chart(document.getElementById("graficoEvolucao"),{type:"line",data:{labels:meses,datasets:[{label:"R$/m²",data:mensalMed,borderColor:"#F0913A",backgroundColor:"rgba(240,145,58,.12)",fill:true,tension:.3,spanGaps:true}]},options:{responsive:true,maintainAspectRatio:false}});

    // 2. Ranking: sempre city-wide para continuar útil mesmo com um bairro selecionado.
    var rankingRows=linhasFiltradas(true), byB={};
    rankingRows.forEach(function(x){var b=x.bairro;if(b==="NÃO INFORMADO")return; if(!byB[b])byB[b]={s:0,q:0,t:0};byB[b].s+=Number(x.soma_m2)||0;byB[b].q+=Number(x.qtd_m2)||0;byB[b].t+=Number(x.transacoes)||0;});
    var rank=Object.keys(byB).map(function(b){return {b:b,m:byB[b].q?byB[b].s/byB[b].q:0,t:byB[b].t};}).filter(function(x){return x.m>0&&x.t>=3;}).sort(function(a,b){return b.m-a.m;}).slice(0,10);
    graficos.graficoRanking=new Chart(document.getElementById("graficoRanking"),{type:"bar",data:{labels:rank.map(function(x){return x.b;}),datasets:[{label:"R$/m²",data:rank.map(function(x){return x.m;}),backgroundColor:"#4FBFB8"}]},options:{responsive:true,maintainAspectRatio:false,indexAxis:"y"}});

    // 3. Histórico anual respeitando o bairro selecionado.
    var histRows=linhasFiltradas(false), byY={};
    histRows.forEach(function(x){if(!byY[x.ano])byY[x.ano]={s:0,q:0};byY[x.ano].s+=Number(x.soma_m2)||0;byY[x.ano].q+=Number(x.qtd_m2)||0;});
    var years=Object.keys(byY).sort(function(a,b){return a-b;});
    graficos.graficoHistorico=new Chart(document.getElementById("graficoHistorico"),{type:"bar",data:{labels:years,datasets:[{label:"R$/m²",data:years.map(function(y){return byY[y].q?byY[y].s/byY[y].q:0;}),backgroundColor:"#388e9c"}]},options:{responsive:true,maintainAspectRatio:false}});

    // 4. Distribuição por metragem.
    var total=rows.reduce(function(a,x){for(i=0;i<4;i++)a[i]+=Number((x.area_bins||[])[i])||0;return a;},[0,0,0,0]);
    graficos.graficoMetragem=new Chart(document.getElementById("graficoMetragem"),{type:"doughnut",data:{labels:["Até 50m²","50–80m²","80–120m²","Acima de 120m²"],datasets:[{data:total,backgroundColor:["#F0913A","#4FBFB8","#388e9c","#41464A"]}]},options:{responsive:true,maintainAspectRatio:false}});

    // 5. Volume mensal do período filtrado.
    var vm=[0,0,0,0,0,0,0,0,0,0,0,0];
    rows.forEach(function(x){if(x.mes>=1&&x.mes<=12)vm[x.mes-1]+=Number(x.transacoes)||0;});
    graficos.graficoVolumeMes=new Chart(document.getElementById("graficoVolumeMes"),{type:"bar",data:{labels:meses,datasets:[{label:"Transações",data:vm,backgroundColor:"#D9762A"}]},options:{responsive:true,maintainAspectRatio:false}});

    // 6. Ranking por volume.
    var bv={}; rankingRows.forEach(function(x){if(x.bairro!=="NÃO INFORMADO")bv[x.bairro]=(bv[x.bairro]||0)+(Number(x.transacoes)||0);});
    var rv=Object.keys(bv).map(function(b){return{b:b,t:bv[b]};}).sort(function(a,b){return b.t-a.t;}).slice(0,10);
    graficos.graficoRankingVolume=new Chart(document.getElementById("graficoRankingVolume"),{type:"bar",data:{labels:rv.map(function(x){return x.b;}),datasets:[{label:"Transações",data:rv.map(function(x){return x.t;}),backgroundColor:"#4FBFB8"}]},options:{responsive:true,maintainAspectRatio:false,indexAxis:"y"}});
  }

  function carregar(cidade) {
    if (!cidades[cidade]) cidade="porto-alegre";
    cidadeAtual=cidade;
    status("Carregando estatísticas de " + cidades[cidade].nome + "…",false);
    destruirTodos();
    fetch(cidades[cidade].arquivo,{cache:"no-store"})
      .then(function(resp){if(!resp.ok)throw new Error("HTTP "+resp.status);return resp.json();})
      .then(function(json){
        dadosCidade=json;
        preencherFiltros();
        atualizar();
        status(json.observacao_bairros || "",false);
      })
      .catch(function(err){
        dadosCidade=null;
        document.getElementById("kpi-m2").textContent="R$ 0,00";
        document.getElementById("kpi-ticket").textContent="R$ 0,00";
        document.getElementById("kpi-transacoes").textContent="0";
        status("Não foi possível carregar os dados estatísticos. Se estiver testando localmente, abra o site por HTTP (ex.: python -m http.server 8000), não por file://.",true);
        console.error(err);
      });
  }

  elCidade.addEventListener("change",function(){carregar(this.value);});
  elBairro.addEventListener("change",atualizar);
  elAno.addEventListener("change",atualizar);
  elMes.addEventListener("change",atualizar);

  var params=new URLSearchParams(window.location.search);
  var inicial=params.get("cidade");
  if(inicial && cidades[inicial]) elCidade.value=inicial;
  carregar(elCidade.value || "porto-alegre");
})();
