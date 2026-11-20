<script setup>
import { computed, onMounted, ref } from 'vue'
import api, { mensagemErro } from '@/services/api'
import StatusBadge from '@/components/StatusBadge.vue'
import { montarInsights } from '@/utils/insights'

const lote = ref(null)
const estatisticas = ref(null)
const lotes = ref([])
const escolhido = ref('')
const carregando = ref(false)
const erro = ref('')
const filtroAtivo = ref('todos')

const dataHora = (t) => (t ? new Date(t).toLocaleString('pt-BR') : '—')

async function carregar() {
  carregando.value = true
  erro.value = ''
  try {
    const { data } = await api.get('/api/dashboard', { params: { loteId: escolhido.value || undefined } })
    lote.value = data.lote
    estatisticas.value = data.estatisticas
  } catch (e) {
    lote.value = null
    estatisticas.value = null
    erro.value = mensagemErro(e, 'Não foi possível carregar os insights.')
  } finally {
    carregando.value = false
  }
}

onMounted(async () => {
  try { lotes.value = (await api.get('/api/dashboard/lotes')).data } catch { lotes.value = [] }
  await carregar()
})

const filtros = [
  { id: 'todos', rotulo: 'Todos' },
  { id: 'destaque', rotulo: 'Destaques' },
  { id: 'oportunidade', rotulo: 'Oportunidades' },
  { id: 'atencao', rotulo: 'Atenção' }
]

// classes completas (o Tailwind só gera classes escritas por extenso)
const estilos = {
  destaque: { icone: '↗', rotulo: 'Destaque', cor: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300' },
  oportunidade: { icone: '★', rotulo: 'Oportunidade', cor: 'bg-brand-100 text-brand-700 dark:bg-brand-500/20 dark:text-brand-100' },
  atencao: { icone: '!', rotulo: 'Atenção', cor: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300' }
}

const insights = computed(() => montarInsights(estatisticas.value, lote.value))
const lista = computed(() =>
  filtroAtivo.value === 'todos' ? insights.value : insights.value.filter((i) => i.tipo === filtroAtivo.value)
)
const frases = computed(() => estatisticas.value?.insights ?? [])
</script>

<template>
  <div>
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-3xl font-bold text-ink-900 dark:text-white">Insights</h1>
        <p v-if="lote" class="mt-1 flex flex-wrap items-center gap-2 text-slate-500 dark:text-slate-400">
          Lote {{ lote.id }} · {{ lote.arquivoNome }} · enviado em {{ dataHora(lote.enviadoEm) }}
          <StatusBadge :status="lote.status" />
        </p>
        <p v-else class="mt-1 text-slate-500 dark:text-slate-400">Descobertas e oportunidades geradas a partir da sua planilha.</p>
      </div>
      <label v-if="lotes.length" class="flex flex-col gap-1 text-sm text-slate-500 dark:text-slate-400">
        Planilha (lote)
        <select v-model="escolhido" class="input !py-2" @change="carregar">
          <option value="">Mais recente processado</option>
          <option v-for="l in lotes" :key="l.id" :value="l.id">#{{ l.id }} · {{ l.arquivoNome }} · {{ l.status }}</option>
        </select>
      </label>
    </div>

    <p v-if="erro" class="mt-6 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-300">
      {{ erro }} <RouterLink to="/app/upload" class="font-semibold underline">Enviar planilha</RouterLink>
    </p>
    <p v-else-if="carregando && !estatisticas" class="mt-6 text-slate-500">Carregando...</p>
    <p v-else-if="lote?.status === 'ERRO'" class="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">
      O Python não conseguiu processar este lote: {{ lote.mensagem }}
    </p>
    <p v-else-if="lote && !estatisticas" class="mt-6 rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:bg-brand-500/10 dark:text-brand-100">
      O Python ainda está processando a planilha. Atualize a página em instantes.
    </p>

    <template v-if="estatisticas">
      <div class="mt-6 flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="Filtrar insights">
        <button
          v-for="f in filtros" :key="f.id" role="tab" :aria-selected="filtroAtivo === f.id"
          class="whitespace-nowrap rounded-lg px-4 py-2.5 text-sm font-medium transition sm:py-2"
          :class="filtroAtivo === f.id
            ? 'bg-brand-600 text-white'
            : 'bg-white text-slate-600 hover:text-slate-900 dark:bg-white/5 dark:text-slate-300 dark:hover:text-white'"
          @click="filtroAtivo = f.id"
        >{{ f.rotulo }}</button>
      </div>

      <div v-if="lista.length" class="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <article v-for="i in lista" :key="i.id" class="card flex flex-col">
          <div class="flex items-center gap-3">
            <span class="flex h-10 w-10 items-center justify-center rounded-full text-lg font-bold" :class="estilos[i.tipo].cor" aria-hidden="true">{{ estilos[i.tipo].icone }}</span>
            <span class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{{ estilos[i.tipo].rotulo }}</span>
          </div>
          <p class="mt-4 break-words text-3xl font-bold text-ink-900 dark:text-white">{{ i.valor }}</p>
          <h2 class="mt-1 font-semibold text-slate-800 dark:text-slate-100">{{ i.titulo }}</h2>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ i.texto }}</p>
          <p class="mt-auto border-t border-slate-100 pt-3 text-sm font-medium text-brand-600 dark:border-white/10 dark:text-brand-100">{{ i.acao }}</p>
        </article>
      </div>
      <p v-else class="card mt-4 text-center text-slate-400">Nenhum insight nesta categoria.</p>

      <div v-if="frases.length" class="card mt-6">
        <h2 class="mb-2 font-semibold text-ink-900 dark:text-white">Análise do Python</h2>
        <ul class="list-disc space-y-1 pl-5 text-sm text-slate-700 dark:text-slate-300"><li v-for="f in frases" :key="f">{{ f }}</li></ul>
      </div>
    </template>
  </div>
</template>
