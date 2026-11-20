<script setup>
import { computed, ref } from 'vue'
import { useUploadStore } from '@/stores/upload'
import { baixarModelo } from '@/utils/planilha'

const store = useUploadStore()
const input = ref(null)
const arrastando = ref(false)
const erroModelo = ref('')

const passos = ['Upload', 'Validação', 'Tratamento', 'Insights']
const passoAtual = computed(() => ({ inicio: 0, validado: 1, enviando: 2, concluido: 3 })[store.etapa])

function escolher(e) {
  const arquivo = e.target.files?.[0]
  if (arquivo) store.carregar(arquivo)
  e.target.value = '' // permite escolher o mesmo arquivo de novo
}
function soltar(e) {
  arrastando.value = false
  const arquivo = e.dataTransfer?.files?.[0]
  if (arquivo) store.carregar(arquivo)
}
async function modelo() {
  erroModelo.value = ''
  try { await baixarModelo() } catch { erroModelo.value = 'Não foi possível gerar o modelo.' }
}
</script>

<template>
  <div class="mx-auto max-w-4xl">
    <h1 class="text-3xl font-bold text-ink-900 dark:text-white">Upload de planilha</h1>
    <p class="mt-1 text-slate-500 dark:text-slate-400">Envie a planilha de clientes para validação e análise.</p>

    <ol class="mt-6 flex flex-wrap items-center gap-4 text-sm">
      <li v-for="(p, i) in passos" :key="p" class="flex items-center gap-2">
        <span class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold" :class="i <= passoAtual ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-700 dark:text-slate-300'">{{ i + 1 }}</span>
        <span :class="i === passoAtual ? 'font-semibold text-slate-900 dark:text-white' : 'text-slate-500 dark:text-slate-400'">{{ p }}</span>
      </li>
    </ol>

    <p v-if="store.erro" role="alert" class="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{{ store.erro }}</p>

    <!-- 1) escolher o arquivo -->
    <div v-if="store.etapa === 'inicio'" class="card mt-6">
      <div
        class="flex flex-col items-center rounded-2xl border-2 border-dashed px-6 py-14 text-center transition"
        :class="arrastando ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10' : 'border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-ink-950/50'"
        @dragover.prevent="arrastando = true" @dragleave="arrastando = false" @drop.prevent="soltar"
      >
        <p class="font-semibold text-slate-900 dark:text-white">Arraste e solte sua planilha aqui</p>
        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Formatos aceitos: .xlsx, .xls, .csv (até 5 MB)</p>
        <input ref="input" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="escolher" />
        <button type="button" class="btn-primary mt-5" :disabled="store.lendo" @click="input.click()">
          {{ store.lendo ? 'Lendo planilha...' : 'Selecionar arquivo' }}
        </button>
      </div>
      <div class="mt-4 flex flex-col items-center gap-2">
        <button type="button" class="btn-ghost" @click="modelo">Baixar modelo de planilha</button>
        <p v-if="erroModelo" class="text-sm text-red-600">{{ erroModelo }}</p>
      </div>
    </div>

    <!-- 2) resultado da validação -->
    <div v-else-if="store.etapa === 'validado' || store.etapa === 'enviando'" class="card mt-6">
      <p class="text-sm text-slate-500 dark:text-slate-400">Arquivo</p>
      <p class="font-semibold text-slate-900 dark:text-white">{{ store.arquivoNome }}</p>

      <div class="mt-4 grid grid-cols-3 gap-3 text-center">
        <div class="rounded-xl bg-slate-50 p-3 dark:bg-white/5"><p class="text-2xl font-bold">{{ store.total }}</p><p class="text-xs text-slate-500">linhas lidas</p></div>
        <div class="rounded-xl bg-emerald-50 p-3 dark:bg-emerald-500/10"><p class="text-2xl font-bold text-emerald-700 dark:text-emerald-300">{{ store.validas.length }}</p><p class="text-xs text-slate-500">válidas</p></div>
        <div class="rounded-xl bg-red-50 p-3 dark:bg-red-500/10"><p class="text-2xl font-bold text-red-700 dark:text-red-300">{{ store.rejeitadas.length }}</p><p class="text-xs text-slate-500">com erro</p></div>
      </div>

      <div v-if="store.rejeitadas.length" class="mt-5">
        <p class="text-sm font-semibold text-slate-900 dark:text-white">Linhas que não serão enviadas</p>
        <ul class="mt-2 max-h-64 space-y-1 overflow-y-auto text-sm text-slate-600 dark:text-slate-300">
          <li v-for="r in store.rejeitadas" :key="r.numeroLinha"><span class="font-medium">Linha {{ r.numeroLinha }}:</span> {{ r.motivos.join(' | ') }}</li>
        </ul>
        <p class="mt-2 text-xs text-slate-500">Corrija na planilha e envie de novo, ou siga só com as linhas válidas.</p>
      </div>

      <div class="mt-6 flex flex-wrap gap-3">
        <button class="btn-primary" :disabled="!store.validas.length || store.etapa === 'enviando'" @click="store.enviar()">
          {{ store.etapa === 'enviando' ? 'Enviando e processando...' : `Enviar ${store.validas.length} linha(s) válida(s)` }}
        </button>
        <button class="btn-ghost" :disabled="store.etapa === 'enviando'" @click="store.reiniciar()">Escolher outro arquivo</button>
      </div>
    </div>

    <!-- 3) concluído -->
    <div v-else class="card mt-6">
      <p class="text-lg font-semibold text-emerald-700 dark:text-emerald-300">
        {{ store.resultado?.status === 'PROCESSADO' ? 'Planilha processada com sucesso' : 'Planilha validada' }}
      </p>
      <p v-if="store.resultado?.status !== 'PROCESSADO'" class="mt-1 text-sm text-slate-500">O processamento em Python está desligado no backend (ETL_ENABLED=false), então os dados não foram gravados.</p>
      <dl class="mt-4 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
        <div><dt class="text-slate-500">Linhas</dt><dd class="text-xl font-bold">{{ store.resultado?.totalLinhas }}</dd></div>
        <div><dt class="text-slate-500">Válidas</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasValidas }}</dd></div>
        <div><dt class="text-slate-500">Recusadas</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasInvalidas }}</dd></div>
        <div><dt class="text-slate-500">Com aviso</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasComAviso }}</dd></div>
      </dl>
      <ul v-if="store.resultado?.rejeitadas?.length" class="mt-4 max-h-48 space-y-1 overflow-y-auto text-sm text-slate-600 dark:text-slate-300">
        <li v-for="(r, i) in store.resultado.rejeitadas" :key="i">{{ r }}</li>
      </ul>
      <div class="mt-6 flex flex-wrap gap-3">
        <RouterLink to="/app/dashboard" class="btn-primary">Ver dashboard</RouterLink>
        <button class="btn-ghost" @click="store.reiniciar()">Enviar outra planilha</button>
      </div>
    </div>
  </div>
</template>
