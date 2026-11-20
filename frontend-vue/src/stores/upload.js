import { defineStore } from 'pinia'
import api, { mensagemErro } from '@/services/api'
import { lerPlanilha, normalizar } from '@/utils/planilha'

const TAMANHO_MAX_MB = 5
const MAX_LINHAS = 5000 // mesmo valor de UPLOAD_MAX_LINHAS no backend

// ---------- tipos de problema (o relatório conta cada um) ----------
export const TIPOS_ERRO = {
  campoVazio: 'Campos obrigatórios vazios',
  duplicado: 'Registros duplicados',
  foraDoPadrao: 'Dados fora do padrão',
  emailInvalido: 'E-mails inválidos'
}
export const TIPOS_AVISO = {
  espacos: 'Espaços desnecessários (removidos)',
  padronizacao: 'Textos padronizados',
  possivelDuplicado: 'Possível duplicidade'
}

const ROTULOS = {
  codigoCliente: 'Código do cliente', nomeCliente: 'Nome do cliente', consultor: 'Consultor',
  segmento: 'Segmento', nivelCliente: 'Nível', faturamentoAnual: 'Faturamento anual',
  servicosContratados: 'Serviços contratados', dataContratacao: 'Data de contratação',
  cidade: 'Cidade', uf: 'UF', email: 'E-mail'
}
const OBRIGATORIOS = ['codigoCliente', 'nomeCliente', 'consultor', 'segmento', 'nivelCliente',
  'faturamentoAnual', 'servicosContratados', 'dataContratacao']
const CAMPOS = [...OBRIGATORIOS, 'cidade', 'uf']

// Mesmo dicionário do backend (Padronizacao.java) e do Python (limpeza.py).
const SEGMENTOS = {
  ind: 'Indústria', industria: 'Indústria', industrial: 'Indústria',
  com: 'Comércio', comercio: 'Comércio',
  serv: 'Serviços', servico: 'Serviços', servicos: 'Serviços',
  saude: 'Saúde', edu: 'Educação', educacao: 'Educação',
  tec: 'Tecnologia', ti: 'Tecnologia', tecnologia: 'Tecnologia'
}
const PARTICULAS = new Set(['de', 'da', 'do', 'das', 'dos', 'e'])

// ---------- funções auxiliares (puras) ----------
const limpar = (t) => String(t ?? '').trim().replace(/\s+/g, ' ')
const chaveSegmento = (t) => normalizar(t).replace(/_/g, ' ').trim()

function titulo(t) {
  return limpar(t).split(' ').map((p, i) => {
    const m = p.toLowerCase()
    return i > 0 && PARTICULAS.has(m) ? m : m.charAt(0).toUpperCase() + m.slice(1)
  }).join(' ')
}

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

const emailValido = (t) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(t)

const contar = (lista, chave) => lista.reduce((acc, x) => { acc[x[chave]] = (acc[x[chave]] || 0) + 1; return acc }, {})
const linhasUnicas = (lista) => new Set(lista.map((x) => x.linha))

// Versão "limpa" de uma linha: sem espaços sobrando e com os textos padronizados.
function limparLinha(l) {
  const seg = SEGMENTOS[chaveSegmento(l.segmento)]
  return {
    numeroLinha: l.numeroLinha,
    codigoCliente: limpar(l.codigoCliente),
    nomeCliente: limpar(l.nomeCliente),
    consultor: titulo(l.consultor),
    segmento: seg || limpar(l.segmento),
    nivelCliente: limpar(l.nivelCliente).toUpperCase(),
    faturamentoAnual: limpar(l.faturamentoAnual),
    servicosContratados: limpar(l.servicosContratados),
    dataContratacao: limpar(l.dataContratacao),
    cidade: limpar(l.cidade),
    uf: limpar(l.uf).toUpperCase()
  }
}

export const useUploadStore = defineStore('upload', {
  // =====================================================================
  // STATE: o que fica guardado temporariamente enquanto a planilha é analisada
  // =====================================================================
  state: () => ({
    etapa: 'inicio',     // inicio | analisado | enviando | concluido
    arquivoNome: '',     // nome do arquivo analisado
    linhas: [],          // as linhas da planilha, como foram lidas (com numeroLinha)
    erros: [],           // [{linha, campo, tipo, descricao, valor}]  problemas que recusam a linha
    avisos: [],          // [{linha, campo, tipo, descricao, valor}]  ajustes e alertas (a linha segue)
    lendo: false,        // true enquanto o arquivo é lido
    erroLeitura: '',     // problema com o arquivo em si (formato, tamanho, colunas faltando)
    resultado: null      // resposta do backend depois do envio
  }),

  // =====================================================================
  // GETTERS: valores calculados a partir do state
  // =====================================================================
  getters: {
    temRelatorio: (s) => s.etapa !== 'inicio' && !!s.arquivoNome,
    totalRegistros: (s) => s.linhas.length,
    linhasComErro: (s) => linhasUnicas(s.erros).size,                               // linhas distintas com ao menos 1 erro
    registrosValidos: (s) => s.linhas.length - linhasUnicas(s.erros).size,          // total menos as linhas com erro
    registrosComAviso: (s) => {                                                     // válidas que tiveram ajuste/aviso
      const comErro = linhasUnicas(s.erros)
      return [...linhasUnicas(s.avisos)].filter((n) => !comErro.has(n)).length
    },
    percentualValidos: (s) =>
      s.linhas.length ? Math.round(((s.linhas.length - linhasUnicas(s.erros).size) / s.linhas.length) * 1000) / 10 : 0,
    errosPorTipo: (s) => {                                                          // [{tipo, rotulo, quantidade}] (inclui os zerados)
      const c = contar(s.erros, 'tipo')
      return Object.entries(TIPOS_ERRO).map(([tipo, rotulo]) => ({ tipo, rotulo, quantidade: c[tipo] || 0 }))
    },
    avisosPorTipo: (s) => {
      const c = contar(s.avisos, 'tipo')
      return Object.entries(TIPOS_AVISO).map(([tipo, rotulo]) => ({ tipo, rotulo, quantidade: c[tipo] || 0 }))
    },
    errosPorCampo: (s) => {                                                         // [{campo, quantidade}] do maior para o menor
      const c = contar(s.erros, 'campo')
      return Object.entries(c).map(([campo, quantidade]) => ({ campo, quantidade })).sort((a, b) => b.quantidade - a.quantidade)
    },
    linhasParaEnvio: (s) => {                                                       // só as linhas sem erro, já limpas e padronizadas
      const comErro = linhasUnicas(s.erros)
      return s.linhas.filter((l) => !comErro.has(l.numeroLinha)).map(limparLinha)
    }
  },

  // =====================================================================
  // ACTIONS: os processos (ler, validar, enviar, limpar)
  // =====================================================================
  actions: {
    reiniciar() { this.$reset() },

    registrarErro(linha, campo, tipo, descricao, valor = '') {
      this.erros.push({ linha, campo, tipo, descricao, valor })
    },
    registrarAviso(linha, campo, tipo, descricao, valor = '') {
      this.avisos.push({ linha, campo, tipo, descricao, valor })
    },

    // Lê o arquivo, guarda as linhas no state e já executa a validação.
    async carregarArquivo(arquivo) {
      this.$reset()
      if (!/\.(xlsx|xls|csv)$/i.test(arquivo.name)) {
        this.erroLeitura = 'Formato não aceito. Envie um arquivo .xlsx, .xls ou .csv.'
        return false
      }
      if (arquivo.size > TAMANHO_MAX_MB * 1024 * 1024) {
        this.erroLeitura = `O arquivo passa de ${TAMANHO_MAX_MB} MB.`
        return false
      }
      this.lendo = true
      try {
        const { linhas, faltando } = await lerPlanilha(arquivo)
        if (faltando.length) {
          this.erroLeitura = 'Faltam colunas obrigatórias: ' + faltando.join(', ') + '. Baixe o modelo para conferir.'
          return false
        }
        if (!linhas.length) { this.erroLeitura = 'A planilha não tem linhas de dados.'; return false }
        if (linhas.length > MAX_LINHAS) { this.erroLeitura = `A planilha passou do limite de ${MAX_LINHAS} linhas.`; return false }
        this.arquivoNome = arquivo.name
        this.linhas = linhas
        this.validar()
        this.etapa = 'analisado'
        return true
      } catch (e) {
        this.erroLeitura = 'Não foi possível ler o arquivo. Confira se ele não está corrompido.'
        return false
      } finally {
        this.lendo = false
      }
    },

    // Percorre as linhas guardadas e registra erros e avisos.
    validar() {
      this.erros = []
      this.avisos = []
      const codigos = new Map()   // código (minúsculo) -> linha da 1ª ocorrência
      const chaves = new Map()    // nome+faturamento+data -> {linha, codigo}

      for (const l of this.linhas) {
        const n = l.numeroLinha

        // 1) espaços desnecessários (no início, no fim ou duplicados no meio)
        for (const campo of [...CAMPOS, 'email']) {
          const v = l[campo]
          if (v && v !== limpar(v)) this.registrarAviso(n, campo, 'espacos', 'Espaços sobrando; serão removidos.', v)
        }

        // 2) campos obrigatórios vazios
        const vazios = new Set()
        for (const campo of OBRIGATORIOS) {
          if (!limpar(l[campo])) {
            vazios.add(campo)
            this.registrarErro(n, campo, 'campoVazio', 'Campo obrigatório vazio.')
          }
        }
        const v = (campo) => limpar(l[campo])

        // 3) código: formato e duplicidade
        if (!vazios.has('codigoCliente')) {
          const cod = v('codigoCliente')
          if (!/^[A-Za-z0-9_-]+$/.test(cod))
            this.registrarErro(n, 'codigoCliente', 'foraDoPadrao', 'Use só letras, números, "-" ou "_".', cod)
          const chave = cod.toLowerCase()
          if (codigos.has(chave))
            this.registrarErro(n, 'codigoCliente', 'duplicado', `Código já usado na linha ${codigos.get(chave)}.`, cod)
          else codigos.set(chave, n)
        }

        // 4) textos que precisam de padronização
        if (!vazios.has('segmento')) {
          const padrao = SEGMENTOS[chaveSegmento(l.segmento)]
          if (!padrao)
            this.registrarErro(n, 'segmento', 'foraDoPadrao', 'Segmento não reconhecido (use Indústria, Comércio, Serviços, Saúde, Educação ou Tecnologia).', v('segmento'))
          else if (padrao !== v('segmento'))
            this.registrarAviso(n, 'segmento', 'padronizacao', `Padronizado para "${padrao}".`, v('segmento'))
        }
        if (!vazios.has('consultor') && titulo(l.consultor) !== v('consultor'))
          this.registrarAviso(n, 'consultor', 'padronizacao', `Padronizado para "${titulo(l.consultor)}".`, v('consultor'))

        // 5) nível A/B/C
        if (!vazios.has('nivelCliente')) {
          const nivel = v('nivelCliente')
          if (!['A', 'B', 'C'].includes(nivel.toUpperCase()))
            this.registrarErro(n, 'nivelCliente', 'foraDoPadrao', 'O nível deve ser A, B ou C.', nivel)
          else if (nivel !== nivel.toUpperCase())
            this.registrarAviso(n, 'nivelCliente', 'padronizacao', `Padronizado para "${nivel.toUpperCase()}".`, nivel)
        }

        // 6) faturamento
        if (!vazios.has('faturamentoAnual')) {
          const f = numero(v('faturamentoAnual'))
          if (Number.isNaN(f)) this.registrarErro(n, 'faturamentoAnual', 'foraDoPadrao', 'Não é um número válido.', v('faturamentoAnual'))
          else if (f < 0) this.registrarErro(n, 'faturamentoAnual', 'foraDoPadrao', 'Faturamento negativo.', v('faturamentoAnual'))
        }

        // 7) data
        if (!vazios.has('dataContratacao') && !dataValida(v('dataContratacao')))
          this.registrarErro(n, 'dataContratacao', 'foraDoPadrao', 'Data inválida (use dd/mm/aaaa, a partir de 1990).', v('dataContratacao'))

        // 8) UF (opcional) e e-mail (coluna opcional)
        if (v('uf') && !/^[A-Za-z]{2}$/.test(v('uf')))
          this.registrarErro(n, 'uf', 'foraDoPadrao', 'A UF deve ter 2 letras.', v('uf'))
        else if (v('uf') && v('uf') !== v('uf').toUpperCase())
          this.registrarAviso(n, 'uf', 'padronizacao', `Padronizado para "${v('uf').toUpperCase()}".`, v('uf'))
        if (v('email') && !emailValido(v('email')))
          this.registrarErro(n, 'email', 'emailInvalido', 'E-mail em formato inválido.', v('email'))

        // 9) possível duplicidade: mesmo nome, faturamento e data com outro código
        if (v('nomeCliente') && v('faturamentoAnual') && v('dataContratacao')) {
          const k = [v('nomeCliente').toLowerCase(), v('faturamentoAnual'), v('dataContratacao')].join('|')
          const antes = chaves.get(k)
          if (antes && antes.codigo !== v('codigoCliente').toLowerCase())
            this.registrarAviso(n, 'nomeCliente', 'possivelDuplicado', `Mesmo nome, faturamento e data da linha ${antes.linha}.`, v('nomeCliente'))
          else if (!antes) chaves.set(k, { linha: n, codigo: v('codigoCliente').toLowerCase() })
        }
      }
    },

    // Envia ao backend só as linhas válidas (já limpas e padronizadas).
    async enviar() {
      if (!this.registrosValidos) return false
      this.etapa = 'enviando'
      this.erroLeitura = ''
      try {
        const { data } = await api.post('/api/uploads/validadas', {
          arquivoNome: this.arquivoNome,
          descartadas: this.linhasComErro,
          linhas: this.linhasParaEnvio
        }, { timeout: 10 * 60 * 1000 })
        this.resultado = data
        this.etapa = 'concluido'
        return true
      } catch (e) {
        this.erroLeitura = mensagemErro(e, 'Não foi possível enviar a planilha.')
        this.etapa = 'analisado'
        return false
      }
    }
  }
})

export { ROTULOS as ROTULOS_CAMPOS }
