// Monta os cards de Insights a partir do JSON que o Python gravou (estatistica_lote).
// Cada card: { id, tipo: 'destaque'|'oportunidade'|'atencao', valor, titulo, texto, acao }.

const fmt = (n, casas = 1) => Number(n).toLocaleString('pt-BR', { maximumFractionDigits: casas })
const pct = (n) => fmt(n) + '%'
const brl = (n) => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
const maior = (lista, campo) => lista.reduce((a, b) => (b[campo] > a[campo] ? b : a))
const menor = (lista, campo) => lista.reduce((a, b) => (b[campo] < a[campo] ? b : a))

export function montarInsights(d, lote) {
  if (!d) return []
  const cards = []
  const add = (tipo, valor, titulo, texto, acao) => cards.push({ id: cards.length + 1, tipo, valor, titulo, texto, acao })

  const seg = d.porSegmento ?? []
  const niv = d.porNivel ?? []
  const con = d.porConsultor ?? []
  const ser = d.porServico ?? []
  const r = d.resumo ?? {}

  // ---- Destaques ----
  const a = niv.find((n) => n.nivel === 'A')
  if (a && a.percentualFaturamento != null)
    add('destaque', pct(a.percentualFaturamento), 'do faturamento vem dos clientes nível A',
      `São ${a.clientes} cliente(s), ${pct(a.percentual)} da carteira.`, 'Priorizar o relacionamento com esses clientes.')
  if (seg.length) {
    const s = maior(seg, 'faturamentoTotal')
    add('destaque', s.segmento, 'é o segmento com maior faturamento',
      `Soma ${brl(s.faturamentoTotal)} em ${s.clientes} cliente(s).`, 'Avaliar ofertas específicas para esse segmento.')
  }
  if (con.length) {
    const c = maior(con, 'faturamentoTotal')
    add('destaque', c.consultor, 'é o consultor com maior faturamento sob gestão',
      `${brl(c.faturamentoTotal)} em ${c.clientes} cliente(s).`, 'Entender e replicar as práticas dessa carteira.')
  }
  if (ser.length) {
    const s = ser[0]
    add('destaque', s.servico, 'é o serviço mais contratado',
      `Presente em ${s.contratos} cliente(s) (${pct(s.percentual)}).`, 'Usar como porta de entrada para outros serviços.')
  }

  // ---- Oportunidades ----
  if (seg.length > 1) {
    const s = menor(seg, 'clientes')
    add('oportunidade', s.segmento, 'é o segmento com menor presença na carteira',
      `Apenas ${s.clientes} cliente(s) (${pct(s.percentual)}).`, 'Planejar uma ação comercial dirigida a esse público.')
  }
  if (ser.length > 1) {
    const s = ser[ser.length - 1]
    add('oportunidade', s.servico, 'é o serviço menos contratado',
      `Presente em ${s.contratos} cliente(s) (${pct(s.percentual)}).`, 'Oferecer aos clientes que ainda não contratam (upsell).')
  }

  // ---- Atenção ----
  const recebidas = d.qualidade?.linhasRecebidas
  const validas = d.qualidade?.linhasValidas
  const fora = recebidas != null && validas != null ? recebidas - validas : (lote?.linhasInvalidas ?? 0)
  if (fora > 0)
    add('atencao', String(fora), 'linha(s) da planilha ficaram de fora da análise',
      'Foram rejeitadas na validação por dados inconsistentes.', 'Corrigir a planilha e enviar novamente.')
  if (lote?.linhasComAviso > 0)
    add('atencao', String(lote.linhasComAviso), 'linha(s) com aviso de padronização',
      'Os dados foram ajustados automaticamente (nomes, segmento, nível).', 'Conferir se os ajustes estão corretos.')
  if (r.media && r.mediana && r.media > r.mediana * 1.1)
    add('atencao', brl(r.media), 'é a média, bem acima da mediana de ' + brl(r.mediana),
      'Poucos clientes muito grandes puxam a média para cima.', 'Olhar a mediana para entender o cliente típico.')
  return cards
}
