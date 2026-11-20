<script setup>
// Gráficos com o que o Python calculou e gravou no banco (estatistica_lote).
// Caminho: banco -> Spring GET /api/dashboard -> esta tela -> Chart.js.
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import api, { mensagemErro } from '@/services/api'
import BaseChart from '@/components/BaseChart.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const moeda = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }))
const numero = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 }))
const dataHora = (t) => (t ? new Date(t).toLocaleString('pt-BR') : '—')

const dash = reactive({
  lote: null, geradoEm: null, estatisticas: null, lotes: [], erro: '', carregando: false,
  async carregar(loteId) {
    this.carregando = true
    this.erro = ''
    try {
      const { data } = await api.get('/api/dashboard', { params: { loteId } })
      this.lote = data.lote
      this.geradoEm = data.geradoEm
      this.estatisticas = data.estatisticas
    } catch (e) {
      this.lote = null
      this.estatisticas = null
      this.erro = mensagemErro(e, 'Não foi possível carregar o dashboard.')
    } finally {
      this.carregando = false
    }
  },
  async carregarLotes() {
    try { this.lotes = (await api.get('/api/dashboard/lotes')).data } catch { this.lotes = [] }
  }
})

const escolhido = ref('')
const d = computed(() => dash.estatisticas)
let relogio = null

const seg = computed(() => d.value?.porSegmento ?? [])
const niv = computed(() => d.value?.porNivel ?? [])
const con = computed(() => d.value?.porConsultor ?? [])
const ser = computed(() => d.value?.porServico ?? [])
const mes = computed(() => d.value?.porMes ?? [])
const top = computed(() => d.value?.topClientes ?? [])

const kpis = computed(() => !d.value ? [] : [
  { rotulo: 'Clientes', valor: String(d.value.resumo.totalClientes) },
  { rotulo: 'Faturamento total', valor: moeda(d.value.resumo.faturamentoTotal) },
  { rotulo: 'Média', valor: moeda(d.value.resumo.media) },
  { rotulo: 'Mediana', valor: moeda(d.value.resumo.mediana) },
  { rotulo: 'Desvio padrão', valor: moeda(d.value.resumo.desvioPadrao) }
])

async function buscar() {
  await dash.carregar(escolhido.value || undefined)
  const processando = dash.lote?.status === 'PROCESSANDO'
  if (processando && !relogio) relogio = setInterval(buscar, 5000) // atualiza sozinho enquanto o Python trabalha
  if (!processando && relogio) { clearInterval(relogio); relogio = null }
}
async function trocarLote() {
  if (relogio) { clearInterval(relogio); relogio = null }
  await buscar()
}

onMounted(async () => { await dash.carregarLotes(); await buscar() })
onBeforeUnmount(() => relogio && clearInterval(relogio))
</script>

<template>
  <div>
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-3xl font-bold text-ink-900 dark:text-white">Dashboard</h1>
        <p v-if="dash.lote" class="mt-1 flex flex-wrap items-center gap-2 text-slate-500 dark:text-slate-400">
          Lote {{ dash.lote.id }} · {{ dash.lote.arquivoNome }} · enviado em {{ dataHora(dash.lote.enviadoEm) }}
          <StatusBadge :status="dash.lote.status" />
        </p>
        <p v-else class="mt-1 text-slate-500 dark:text-slate-400">Visão geral dos indicadores e insights da sua planilha.</p>
      </div>
      <label v-if="dash.lotes.length" class="flex flex-col gap-1 text-sm text-slate-500 dark:text-slate-400">
        Planilha (lote)
        <select v-model="escolhido" class="input !py-2" @change="trocarLote">
          <option value="">Mais recente processado</option>
          <option v-for="l in dash.lotes" :key="l.id" :value="l.id">#{{ l.id }} · {{ l.arquivoNome }} · {{ l.status }}</option>
        </select>
      </label>
    </div>

    <p v-if="dash.erro" class="mt-6 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-300">
      {{ dash.erro }} <RouterLink to="/app/upload" class="font-semibold underline">Enviar planilha</RouterLink>
    </p>
    <p v-else-if="dash.carregando && !d" class="mt-6 text-slate-500">Carregando...</p>
    <p v-else-if="dash.lote?.status === 'ERRO'" class="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">
      O Python não conseguiu processar este lote: {{ dash.lote.mensagem }}
    </p>
    <p v-else-if="dash.lote && !d" class="mt-6 rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-500/10 dark:text-brand-100">
      O Python está processando a planilha. Esta tela atualiza sozinha.
    </p>

    <template v-if="d">
      <div class="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-5">
        <div v-for="k in kpis" :key="k.rotulo" class="card !p-4">
          <p class="text-sm text-slate-500 dark:text-slate-400">{{ k.rotulo }}</p>
          <p class="mt-1 text-xl font-bold text-ink-900 dark:text-white sm:text-2xl">{{ k.valor }}</p>
        </div>
      </div>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <div class="card"><h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Clientes por segmento</h2>
          <BaseChart tipo="bar" titulo="Clientes" :rotulos="seg.map((x) => x.segmento)" :valores="seg.map((x) => x.clientes)" /></div>
        <div class="card"><h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Clientes por nível</h2>
          <BaseChart tipo="doughnut" :rotulos="niv.map((x) => 'Nível ' + x.nivel)" :valores="niv.map((x) => x.clientes)" /></div>
        <div class="card"><h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Faturamento por consultor</h2>
          <BaseChart tipo="bar" horizontal moeda titulo="Faturamento" :rotulos="con.map((x) => x.consultor)" :valores="con.map((x) => x.faturamentoTotal)" /></div>
        <div class="card"><h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Serviços mais contratados</h2>
          <BaseChart tipo="bar" horizontal titulo="Contratos" :rotulos="ser.map((x) => x.servico)" :valores="ser.map((x) => x.contratos)" /></div>
        <div class="card lg:col-span-2"><h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Contratações por mês</h2>
          <BaseChart tipo="line" titulo="Clientes" :rotulos="mes.map((x) => x.mes)" :valores="mes.map((x) => x.clientes)" /></div>
      </div>

      <div v-if="top.length" class="card mt-4 overflow-x-auto">
        <h2 class="mb-3 font-semibold text-ink-900 dark:text-white">Maiores clientes</h2>
        <table class="w-full min-w-[32rem] text-left text-sm">
          <thead class="text-slate-500 dark:text-slate-400">
            <tr><th class="pb-2 font-medium">Código</th><th class="pb-2 font-medium">Cliente</th><th class="pb-2 font-medium">Segmento</th><th class="pb-2 font-medium">Nível</th><th class="pb-2 text-right font-medium">Faturamento anual</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in top" :key="c.codigo" class="border-t border-slate-100 dark:border-white/10">
              <td class="py-2">{{ c.codigo }}</td><td class="py-2">{{ c.nome }}</td><td class="py-2">{{ c.segmento }}</td><td class="py-2">{{ c.nivel }}</td><td class="py-2 text-right">{{ moeda(c.faturamento) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="d.insights?.length" class="card mt-4">
        <h2 class="mb-2 font-semibold text-ink-900 dark:text-white">O que os dados mostram</h2>
        <ul class="list-disc space-y-1 pl-5 text-sm text-slate-700 dark:text-slate-300"><li v-for="i in d.insights" :key="i">{{ i }}</li></ul>
      </div>

      <p class="mt-4 text-xs text-slate-500">
        Dados lidos do banco (estatistica_lote) · calculados pelo Python em {{ dataHora(dash.geradoEm) }} ·
        linhas recebidas: {{ numero(d.qualidade?.linhasRecebidas) }} · válidas: {{ numero(d.qualidade?.linhasValidas) }}
      </p>
    </template>
  </div>
</template>
