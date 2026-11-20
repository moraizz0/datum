import { defineStore } from 'pinia'
import api, { mensagemErro } from '@/services/api'
import { lerPlanilha, normalizar } from '@/utils/planilha'

const TAMANHO_MAX_MB = 5
const MAX_LINHAS = 5000 // mesmo valor de UPLOAD_MAX_LINHAS no backend

// Mesmo dicionário do backend (Padronizacao.java) e do Python (limpeza.py).
const SEGMENTOS = new Set([
  'ind', 'industria', 'industrial', 'com', 'comercio', 'serv', 'servico', 'servicos',
  'saude', 'edu', 'educacao', 'tec', 'ti', 'tecnologia'
])

function numero(txt) {
  let t = String(txt).replace(/R\$|\s/g, '')
  if (t === '') return NaN
  if (t.includes(',')) t = t.replace(/\./g, '').replace(',', '.')
  else if (/^\d{1,3}(\.\d{3})+$/.test(t)) t = t.replace(/\./g, '')
  return /^-?\d+(\.\d+)?$/.test(t) ? Number(t) : NaN
}

function dataValida(txt) {
  let d, m, a
  let r = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(txt)
  if (r) [, d, m, a] = r
  else if ((r = /^(\d{4})-(\d{1,2})-(\d{1,2})/.exec(txt))) [, a, m, d] = r
  else return false
  d = +d; m = +m; a = +a
  const dt = new Date(a, m - 1, d)
  return a >= 1990 && dt.getFullYear() === a && dt.getMonth() === m - 1 && dt.getDate() === d
}

// Confere cada linha. Devolve as mensagens de erro (vazio = linha válida).
function erros(l, codigosVistos) {
  const e = []
  const obrig = [
    ['codigoCliente', 'código'], ['nomeCliente', 'nome'], ['consultor', 'consultor'],
    ['segmento', 'segmento'], ['nivelCliente', 'nível'], ['faturamentoAnual', 'faturamento'],
    ['servicosContratados', 'serviços'], ['dataContratacao', 'data de contratação']
  ]
  for (const [campo, nome] of obrig) if (!l[campo]) e.push(`${nome} vazio`)

  if (l.codigoCliente) {
    if (!/^[A-Za-z0-9_-]+$/.test(l.codigoCliente)) e.push('código com caracteres inválidos')
    const chave = l.codigoCliente.toLowerCase()
    if (codigosVistos.has(chave)) e.push('código repetido na planilha')
    codigosVistos.add(chave)
  }
  if (l.segmento && !SEGMENTOS.has(normalizar(l.segmento).replace(/_/g, ' ').trim()))
    e.push(`segmento "${l.segmento}" não reconhecido`)
  if (l.nivelCliente && !['A', 'B', 'C'].includes(l.nivelCliente.toUpperCase())) e.push('nível deve ser A, B ou C')
  if (l.faturamentoAnual) {
    const n = numero(l.faturamentoAnual)
    if (Number.isNaN(n)) e.push('faturamento não é um número')
    else if (n < 0) e.push('faturamento negativo')
  }
  if (l.dataContratacao && !dataValida(l.dataContratacao)) e.push('data inválida (use dd/mm/aaaa)')
  if (l.uf && l.uf.length !== 2) e.push('UF deve ter 2 letras')
  return e
}

export const useUploadStore = defineStore('upload', {
  state: () => ({
    etapa: 'inicio',       // inicio | validado | enviando | concluido
    arquivoNome: '',
    validas: [],           // linhas aprovadas (vão para o backend)
    rejeitadas: [],        // [{numeroLinha, motivos: []}]
    lendo: false,
    erro: '',
    resultado: null        // resposta do backend
  }),
  getters: {
    total: (s) => s.validas.length + s.rejeitadas.length
  },
  actions: {
    reiniciar() {
      this.$reset()
    },
    async carregar(arquivo) {
      this.$reset()
      if (!/\.(xlsx|xls|csv)$/i.test(arquivo.name)) {
        this.erro = 'Formato não aceito. Envie um arquivo .xlsx, .xls ou .csv.'
        return
      }
      if (arquivo.size > TAMANHO_MAX_MB * 1024 * 1024) {
        this.erro = `O arquivo passa de ${TAMANHO_MAX_MB} MB.`
        return
      }
      this.lendo = true
      try {
        const { linhas, faltando } = await lerPlanilha(arquivo)
        if (faltando.length) {
          this.erro = 'Faltam colunas obrigatórias: ' + faltando.join(', ') + '. Baixe o modelo para conferir.'
          return
        }
        if (!linhas.length) { this.erro = 'A planilha não tem linhas de dados.'; return }
        if (linhas.length > MAX_LINHAS) { this.erro = `A planilha passou do limite de ${MAX_LINHAS} linhas.`; return }

        const vistos = new Set()
        for (const l of linhas) {
          const e = erros(l, vistos)
          if (e.length) this.rejeitadas.push({ numeroLinha: l.numeroLinha, motivos: e })
          else this.validas.push(l)
        }
        this.arquivoNome = arquivo.name
        this.etapa = 'validado'
      } catch (e) {
        this.erro = 'Não foi possível ler o arquivo. Confira se ele não está corrompido.'
      } finally {
        this.lendo = false
      }
    },
    async enviar() {
      if (!this.validas.length) return
      this.etapa = 'enviando'
      this.erro = ''
      try {
        const { data } = await api.post('/api/uploads/validadas', {
          arquivoNome: this.arquivoNome,
          descartadas: this.rejeitadas.length,
          linhas: this.validas
        }, { timeout: 10 * 60 * 1000 })
        this.resultado = data
        this.etapa = 'concluido'
      } catch (e) {
        this.erro = mensagemErro(e, 'Não foi possível enviar a planilha.')
        this.etapa = 'validado'
      }
    }
  }
})
