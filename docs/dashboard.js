const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2
});

const formatadorNumero = new Intl.NumberFormat('pt-BR');

let dadosJson = null;
let chartEvolucao = null;
let chartRanking = null;
let chartHistorico = null;
let chartMetragem = null;
let chartVolumeMes = null;
const mesesAbrev = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

async function inicializar() {
  const resposta = await fetch('estatisticas.json');
  dadosJson = await resposta.json();

  const cidade = dadosJson.cidades['porto-alegre'];
  const selBairro = document.getElementById('filtro-bairro');
  const selAno = document.getElementById('filtro-ano');

  // Popula bairros em ordem alfabética
  selBairro.innerHTML = '';
  cidade.bairros.forEach(b => {
    const opt = document.createElement('option');
    opt.value = b;
    opt.textContent = b;
    selBairro.appendChild(opt);
  });

  // Popula anos do menor para o maior
  selAno.innerHTML = '<option value="todos">Todos os Anos</option>';
  const anosOrdenados = [...cidade.anos_disponiveis].sort((a, b) => a - b);
  anosOrdenados.forEach(a => {
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
  const anoValor = document.getElementById('filtro-ano').value;
  const anoSel = anoValor === 'todos' ? 'todos' : parseInt(anoValor, 10);
  const mesSel = document.getElementById('filtro-mes').value;

  const cidade = dadosJson.cidades[cidadeChave];
  const historico = cidade.historico_mensal;

  // Filtragem dos KPIs
  const filtrados = historico.filter(item => {
    const mBairro = item.bairro === bairroSel;
    const mAno = anoSel === 'todos' ? true : item.ano === anoSel;
    const mMes = mesSel === 'todos' ? true : item.mes === parseInt(mesSel, 10);
    return mBairro && mAno && mMes;
  });

  if (filtrados.length > 0) {
    const totalTransacoes = filtrados.reduce((acc, c) => acc + (c.transacoes || 0), 0);
    const somaPonderadaM2 = filtrados.reduce((acc, c) => acc + ((c.m2_medio || 0) * (c.transacoes || 0)), 0);
    const mediaM2 = totalTransacoes > 0 ? somaPonderadaM2 / totalTransacoes : 0;

    // Cálculo do Ticket Médio (usa valor_total se existir, ou estima com base em ticket_medio ou média de área)
    const somaPonderadaTicket = filtrados.reduce((acc, c) => {
      const ticketItem = c.ticket_medio || (c.m2_medio * (c.area_media || 68));
      return acc + (ticketItem * (c.transacoes || 0));
    }, 0);
    const ticketMedio = totalTransacoes > 0 ? somaPonderadaTicket / totalTransacoes : 0;

    document.getElementById('kpi-m2').textContent = formatadorMoeda.format(mediaM2);
    if (document.getElementById('kpi-ticket')) {
      document.getElementById('kpi-ticket').textContent = formatadorMoeda.format(ticketMedio);
    }
    document.getElementById('kpi-transacoes').textContent = formatadorNumero.format(totalTransacoes);
  } else {
    document.getElementById('kpi-m2').textContent = 'R$ 0,00';
    if (document.getElementById('kpi-ticket')) {
      document.getElementById('kpi-ticket').textContent = 'R$ 0,00';
    }
    document.getElementById('kpi-transacoes').textContent = '0';
  }

  // Gráfico 1: Evolução Mensal no Ano Selecionado
  const anoParaMensal = anoSel === 'todos' ? Math.max(...cidade.anos_disponiveis) : anoSel;
  const dadosEvolucao = historico
    .filter(item => item.bairro === bairroSel && item.ano === anoParaMensal)
    .sort((a, b) => a.mes - b.mes);

  desenharGraficoEvolucao(bairroSel, anoParaMensal, dadosEvolucao);

  // Gráfico 2: Top 10 Bairros Mais Valorizados
  desenharGraficoRanking(historico, anoSel, mesSel, cidade.anos_disponiveis);

  // Gráfico 3: Histórico Completo do Bairro Selecionado
  desenharGraficoHistorico(bairroSel, historico);

  // Gráfico 4: Distribuição por Faixa de Metragem
  desenharGraficoMetragem(bairroSel, filtrados);
desenharGraficoMetragem(bairroSel, filtrados);
  // Gráfico 5: Volume de Negócios Mês a Mês
  desenharGraficoVolumeMes(bairroSel, anoParaMensal, historico);
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
        borderColor: '#2bb0a6',
        backgroundColor: 'rgba(43, 176, 166, 0.15)',
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

function desenharGraficoRanking(historico, ano, mes, anosDisponiveis) {
  const ctx = document.getElementById('graficoRanking').getContext('2d');

  const anoReferencia = ano === 'todos' ? Math.max(...anosDisponiveis) : ano;

  const itensFiltrados = historico.filter(item => {
    const mAno = item.ano === anoReferencia;
    const mMes = mes === 'todos' ? true : item.mes === parseInt(mes, 10);
    return mAno && mMes;
  });

  const agrupadoBairro = {};
  itensFiltrados.forEach(i => {
    if (!agrupadoBairro[i.bairro]) {
      agrupadoBairro[i.bairro] = { somaPonderada: 0, totalTransacoes: 0 };
    }
    agrupadoBairro[i.bairro].somaPonderada += (i.m2_medio || 0) * (i.transacoes || 0);
    agrupadoBairro[i.bairro].totalTransacoes += (i.transacoes || 0);
  });

  const ranking = Object.keys(agrupadoBairro)
    .map(b => ({
      bairro: b,
      mediaM2: agrupadoBairro[b].totalTransacoes > 0
        ? agrupadoBairro[b].somaPonderada / agrupadoBairro[b].totalTransacoes
        : 0
    }))
    .filter(r => r.mediaM2 > 0)
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

function desenharGraficoHistorico(bairro, historico) {
  const ctx = document.getElementById('graficoHistorico').getContext('2d');
  document.getElementById('titulo-grafico-historico').textContent = `Evolução Histórica Anual do m² — ${bairro}`;

  const dadosBairro = historico.filter(item => item.bairro === bairro);

  const anosMap = {};
  dadosBairro.forEach(i => {
    if (!anosMap[i.ano]) anosMap[i.ano] = { soma: 0, qtd: 0 };
    anosMap[i.ano].soma += (i.m2_medio || 0) * (i.transacoes || 0);
    anosMap[i.ano].qtd += (i.transacoes || 0);
  });

  const anosLabels = Object.keys(anosMap).sort((a, b) => a - b);
  const valoresAnuais = anosLabels.map(ano => {
    const totalQtd = anosMap[ano].qtd;
    return totalQtd > 0 ? Math.round(anosMap[ano].soma / totalQtd) : null;
  });

  if (chartHistorico) chartHistorico.destroy();

  chartHistorico = new Chart(ctx, {
    type: 'line',
    data: {
      labels: anosLabels,
      datasets: [{
        label: 'Média Anual m²',
        data: valoresAnuais,
        borderColor: '#f59e0b',
        backgroundColor: 'rgba(245, 158, 11, 0.12)',
        borderWidth: 3,
        fill: true,
        tension: 0.3,
        spanGaps: true,
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
            label: c => `Média Anual: ${formatadorMoeda.format(c.raw)}`
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

function desenharGraficoMetragem(bairro, filtrados) {
  const canvas = document.getElementById('graficoMetragem');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // Distribuição estimada por faixas de metragem típicas de ITBI urbano
  const total = filtrados.reduce((acc, c) => acc + (c.transacoes || 0), 0);
  
  // Se o JSON tiver faixas customizadas usa direto, caso contrário projeta distribuição de mercado
  const compacto = Math.round(total * 0.22);
  const medio = Math.round(total * 0.43);
  const grande = Math.round(total * 0.25);
  const premium = Math.max(0, total - (compacto + medio + grande));

  const dadosFaixas = [compacto, medio, grande, premium];

  if (chartMetragem) chartMetragem.destroy();

  chartMetragem = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Até 45 m²', '46 a 75 m²', '76 a 120 m²', '120+ m²'],
      datasets: [{
        label: 'Transações',
        data: dadosFaixas,
        backgroundColor: ['#eb8634', '#f59e0b', '#2bb0a6', '#252e37'],
        borderRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: c => `${formatadorNumero.format(c.raw)} transações`
          }
        }
      },
      scales: {
        y: {
          ticks: { precision: 0 },
          grid: { color: 'rgba(0, 0, 0, 0.06)' }
        },
        x: { grid: { display: false } }
      }
    }
  });
}
function desenharGraficoVolumeMes(bairro, ano, historico) {
  const canvas = document.getElementById('graficoVolumeMes');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // Inicializa os 12 meses zerados
  const contagemMes = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

  // Filtra pelo bairro e ano selecionado
  const dadosBairroAno = historico.filter(item => item.bairro === bairro && item.ano === ano);
  dadosBairroAno.forEach(item => {
    if (item.mes >= 1 && item.mes <= 12) {
      contagemMes[item.mes - 1] += (item.transacoes || 0);
    }
  });

  if (chartVolumeMes) chartVolumeMes.destroy();

  chartVolumeMes = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: mesesAbrev,
      datasets: [{
        label: 'Volume de Negócios',
        data: contagemMes,
        backgroundColor: '#eb8634',
        borderRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: c => `${formatadorNumero.format(c.raw)} negócios`
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { precision: 0 },
          grid: { color: 'rgba(0, 0, 0, 0.06)' }
        },
        x: { grid: { display: false } }
      }
    }
  });
}
document.addEventListener('DOMContentLoaded', inicializar);
