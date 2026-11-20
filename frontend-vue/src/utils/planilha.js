// Leitura da planilha e modelo para download. A biblioteca "xlsx" só é carregada quando precisa.

export const COLUNAS = [
  'codigo_cliente', 'nome_cliente', 'consultor', 'segmento', 'nivel_cliente',
  'faturamento_anual', 'servicos_contratados', 'data_contratacao', 'cidade', 'uf'
]
export const OBRIGATORIAS = COLUNAS.slice(0, 8) // cidade e uf são opcionais

// "Código do Cliente " -> "codigo_do_cliente"
export const normalizar = (t) =>
  String(t ?? '').normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '')

const doisDigitos = (n) => String(n).padStart(2, '0')

// Data do Excel -> "aaaa-mm-dd" (soma 12h para não errar o dia por causa do fuso)
function dataParaTexto(d) {
  const x = new Date(d.getTime() + 12 * 3600 * 1000)
  return `${x.getUTCFullYear()}-${doisDigitos(x.getUTCMonth() + 1)}-${doisDigitos(x.getUTCDate())}`
}

function celulaParaTexto(v) {
  if (v instanceof Date) return dataParaTexto(v)
  if (v === null || v === undefined) return ''
  return String(v) // sem trim: o espaço sobrando é um dos problemas que a validação precisa enxergar
}

/**
 * Lê .xlsx, .xls ou .csv e devolve { linhas: [{numeroLinha, codigoCliente, ...}], faltando: [colunas] }.
 * numeroLinha é a linha da planilha (o cabeçalho é a linha 1).
 */
export async function lerPlanilha(arquivo) {
  const XLSX = await import('xlsx')
  const ehCsv = /\.csv$/i.test(arquivo.name)
  const livro = ehCsv
    ? XLSX.read(await arquivo.text(), { type: 'string', cellDates: true })
    : XLSX.read(await arquivo.arrayBuffer(), { type: 'array', cellDates: true })

  const folha = livro.Sheets[livro.SheetNames[0]]
  const matriz = XLSX.utils.sheet_to_json(folha, { header: 1, raw: true, defval: '' })
  if (!matriz.length) return { linhas: [], faltando: [...OBRIGATORIAS] }

  const posicao = {}
  matriz[0].forEach((titulo, i) => { posicao[normalizar(titulo)] = i })
  const faltando = OBRIGATORIAS.filter((c) => !(c in posicao))
  if (faltando.length) return { linhas: [], faltando }

  const pega = (linha, coluna) => (coluna in posicao ? celulaParaTexto(linha[posicao[coluna]]) : '')
  const linhas = []
  matriz.slice(1).forEach((l, i) => {
    if (l.every((c) => celulaParaTexto(c).trim() === '')) return // ignora linha totalmente vazia
    linhas.push({
      numeroLinha: i + 2,
      codigoCliente: pega(l, 'codigo_cliente'),
      nomeCliente: pega(l, 'nome_cliente'),
      consultor: pega(l, 'consultor'),
      segmento: pega(l, 'segmento'),
      nivelCliente: pega(l, 'nivel_cliente'),
      faturamentoAnual: pega(l, 'faturamento_anual'),
      servicosContratados: pega(l, 'servicos_contratados'),
      dataContratacao: pega(l, 'data_contratacao'),
      cidade: pega(l, 'cidade'),
      uf: pega(l, 'uf'),
      email: pega(l, 'email') // coluna opcional: só é validada se existir na planilha
    })
  })
  return { linhas, faltando: [] }
}

// Gera e baixa o modelo (.xlsx) com o cabeçalho certo e 2 linhas de exemplo.
export async function baixarModelo() {
  const XLSX = await import('xlsx')
  const dados = [
    COLUNAS,
    ['CTI001', 'Empresa Alfa Ltda', 'Ana Souza', 'Indústria', 'A', 1850000, 'Internet; Telefonia', '05/03/2024', 'Campinas', 'SP'],
    ['CTI002', 'Comercial Beta', 'Carlos Lima', 'Comércio', 'B', 420000, 'Suporte', '10/01/2023', 'Sumaré', 'SP']
  ]
  const folha = XLSX.utils.aoa_to_sheet(dados)
  folha['!cols'] = COLUNAS.map((c) => ({ wch: Math.max(14, c.length + 2) }))
  const livro = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(livro, folha, 'Clientes')
  XLSX.writeFile(livro, 'modelo_datum.xlsx')
}
