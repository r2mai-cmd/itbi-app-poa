const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2
});

const formatadorNumero = new Intl.NumberFormat('pt-BR');

let dadosJson = null;
let chartEvolucao = null;
let chartRanking = null;

const mesesAbrev = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

async function inicializar() {
  // Remova o "data/" da URL:
const resposta = await fetch('estatisticas.json');
  dadosJson = await resposta.json();

  const cidade = dadosJson.cidades['porto-alegre'];
  const selBairro = document.getElementById('filtro-bairro');
  const selAno = document.getElementById('filtro-ano');

  // Popula bairros
  selBairro.innerHTML = '';
  cidade.bairros.forEach(b => {
    const opt = document.createElement('option');
    opt.value = b;
    opt.textContent = b;
    selBairro.appendChild(opt);
  });

  // Popula anos
  selAno.innerHTML = '';
  cidade.anos_disponiveis.forEach(a => {
    const opt = document.createElement('option');
    opt.value = a;
    opt.textContent = a;
    selAno.appendChild(opt);
  });

  document.getElementById('filtro-bairro').addEventListener('change', atualizar);
  document.getElementById('filtro-ano').addEventListener('change', atualizar);
  document.getElementById('filtro-mes').addEventListener('change', atualizar);

  atualizar();
}

function atualizar() {
  const cidadeChave = document.getElementById('filtro-cidade').value;
  const bairroSel = document.getElementById('filtro-bairro').value;
  const anoSel = parseInt(document.getElementById('filtro-ano').value, 10);
  const mesSel = document.getElementById('filtro-mes').value;

  const historico = dadosJson.cidades[cidadeChave].historico_mensal;

  // Filtragem dos KPIs
  const filtrados = historico.filter(item => {
    const mBairro = item.bairro === bairroSel;
    const mAno = item.ano === anoSel;
    const mMes = mesSel === 'todos' ? true : item.mes === parseInt(mesSel, 10);
    return mBairro && mAno && mMes;
  });

  if (filtrados.length > 0) {
    const totalTransacoes = filtrados.reduce((acc, c) => acc + c.transacoes, 0);
    const somaPonderada = filtrados.reduce((acc, c) => acc + (c.m2_medio * c.transacoes), 0);
    const mediaM2 = somaPonderada / totalTransacoes;

    document.getElementById('kpi-m2').textContent = formatadorMoeda.format(mediaM2);
    document.getElementById('kpi-transacoes').textContent = formatadorNumero.format(totalTransacoes);
  } else {
    document.getElementById('kpi-m2').textContent = 'R$ 0,00';
    document.getElementById('kpi-transacoes').textContent = '0';
  }

  // Gráfico 1: Evolução Mensal do Bairro no Ano
  const dadosEvolucao = historico
    .filter(item => item.bairro === bairroSel && item.ano === anoSel)
    .sort((a, b) => a.mes - b.mes);

  desenharGraficoEvolucao(bairroSel, anoSel, dadosEvolucao);

  // Gráfico 2: Top 10 Bairros Mais Caros do Ano Selecionado
  desenharGraficoRanking(historico, anoSel, mesSel);
}

function desenharGraficoEvolucao(bairro, ano, dados) {
  const ctx = document.getElementById('graficoEvolucao').getContext('2d');
  document.getElementById('titulo-grafico-evolucao').textContent = `Evolução Mensal do m² — ${bairro} (${ano})`;

  const labels = dados.map(d => mesesAbrev[d.mes - 1]);
  const valores = dados.map(d => d.m2_medio);

  if (chartEvolucao) chartEvolucao.destroy();

  chartEvolucao = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [{
        label: 'Valor médio m²',
        data: valores,
        borderColor: '#3b929c',
        backgroundColor: 'rgba(59, 146, 156, 0.15)',
        borderWidth: 3,
        fill: true,
        tension: 0.35,
        pointBackgroundColor: '#eb8634',
        pointRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        tooltip: {
          callbacks: {
            label: c => `m²: ${formatadorMoeda.format(c.raw)}`
          }
        }
      },
      scales: {
        y: {
          ticks: { callback: v => formatadorMoeda.format(v) },
          grid: { color: 'rgba(0, 0, 0, 0.06)' }
        },
        x: { grid: { display: false } }
      }
    }
  });
}

function desenharGraficoRanking(historico, ano, mes) {
  const ctx = document.getElementById('graficoRanking').getContext('2d');

  // Filtra pelo ano e opcionalmente pelo mês
  const itensAno = historico.filter(item => {
    const mAno = item.ano === ano;
    const mMes = mes === 'todos' ? true : item.mes === parseInt(mes, 10);
    return mAno && mMes;
  });

  // Agrupa médias por bairro
  const agrupadoBairro = {};
  itensAno.forEach(i => {
    if (!agrupadoBairro[i.bairro]) {
      agrupadoBairro[i.bairro] = { somaPonderada: 0, totalTransacoes: 0 };
    }
    agrupadoBairro[i.bairro].somaPonderada += i.m2_medio * i.transacoes;
    agrupadoBairro[i.bairro].totalTransacoes += i.transacoes;
  });

  const ranking = Object.keys(agrupadoBairro)
    .map(b => ({
      bairro: b,
      mediaM2: agrupadoBairro[b].somaPonderada / agrupadoBairro[b].totalTransacoes
    }))
    .sort((a, b) => b.mediaM2 - a.mediaM2)
    .slice(0, 10);

  if (chartRanking) chartRanking.destroy();

  chartRanking = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ranking.map(r => r.bairro),
      datasets: [{
        label: 'Preço m²',
        data: ranking.map(r => r.mediaM2),
        backgroundColor: '#eb8634',
        borderRadius: 6
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: c => `m² médio: ${formatadorMoeda.format(c.raw)}`
          }
        }
      },
      scales: {
        x: {
          ticks: { callback: v => formatadorMoeda.format(v) },
          grid: { color: 'rgba(0, 0, 0, 0.06)' }
        },
        y: { grid: { display: false } }
      }
    }
  });
}

document.addEventListener('DOMContentLoaded', inicializar);
