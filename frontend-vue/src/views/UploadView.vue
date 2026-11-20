<script setup>
import { computed, ref } from 'vue'
import { useUploadStore, TIPOS_ERRO, TIPOS_AVISO, ROTULOS_CAMPOS } from '@/stores/upload'
import { baixarModelo } from '@/utils/planilha'

const store = useUploadStore()
const input = ref(null)
const arrastando = ref(false)
const erroModelo = ref('')

const passos = ['Upload', 'Validação', 'Relatório', 'Envio']
const passoAtual = computed(() => ({ inicio: 0, analisado: 2, enviando: 3, concluido: 3 })[store.etapa])

// Lê o arquivo e valida no Pinia; o relatório aparece logo abaixo, nesta mesma tela.
const processar = (arquivo) => { if (arquivo) store.carregarArquivo(arquivo) }
const escolher = (e) => { processar(e.target.files?.[0]); e.target.value = '' }
const soltar = (e) => { arrastando.value = false; processar(e.dataTransfer?.files?.[0]) }
async function modelo() {
  erroModelo.value = ''
  try { await baixarModelo() } catch { erroModelo.value = 'Não foi possível gerar o modelo.' }
}

// ----- relatório -----
const filtro = ref('erros')     // erros | avisos
const tipoSel = ref('')         // '' = todos os tipos
const campo = (c) => ROTULOS_CAMPOS[c] || c
const tipos = computed(() => (filtro.value === 'erros' ? TIPOS_ERRO : TIPOS_AVISO))
const itens = computed(() => {
  const base = filtro.value === 'erros' ? store.erros : store.avisos
  return (tipoSel.value ? base.filter((x) => x.tipo === tipoSel.value) : base)
    .slice().sort((a, b) => a.linha - b.linha)
})
function trocar(f) { filtro.value = f; tipoSel.value = '' }
const cor = {
  erro: 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300',
  aviso: 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300'
}
</script>

<template>
  <div class="mx-auto max-w-5xl">
    <h1 class="text-3xl font-bold text-ink-900 dark:text-white">Upload de planilha</h1>
    <p class="mt-1 text-slate-500 dark:text-slate-400">Envie a planilha de clientes. Ela é validada e o relatório aparece aqui mesmo, antes de qualquer envio.</p>

    <ol class="mt-6 flex flex-wrap items-center gap-4 text-sm">
      <li v-for="(p, i) in passos" :key="p" class="flex items-center gap-2">
        <span class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold" :class="i <= passoAtual ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-700 dark:text-slate-300'">{{ i + 1 }}</span>
        <span :class="i === passoAtual ? 'font-semibold text-slate-900 dark:text-white' : 'text-slate-500 dark:text-slate-400'">{{ p }}</span>
      </li>
    </ol>

    <p v-if="store.erroLeitura" role="alert" class="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{{ store.erroLeitura }}</p>

    <!-- seleção do arquivo -->
    <div class="card mt-6">
      <div
        class="flex flex-col items-center rounded-2xl border-2 border-dashed px-6 text-center transition"
        :class="[store.temRelatorio ? 'py-8' : 'py-14', arrastando ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10' : 'border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-ink-950/50']"
        @dragover.prevent="arrastando = true" @dragleave="arrastando = false" @drop.prevent="soltar"
      >
        <p class="font-semibold text-slate-900 dark:text-white">Arraste e solte sua planilha aqui</p>
        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Formatos aceitos: .xlsx, .xls, .csv (até 5 MB)</p>
        <input ref="input" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="escolher" />
        <button type="button" class="btn-primary mt-5" :disabled="store.lendo || store.etapa === 'enviando'" @click="input.click()">
          {{ store.lendo ? 'Lendo e validando...' : 'Selecionar arquivo' }}
        </button>
      </div>
      <div class="mt-4 flex flex-col items-center gap-2">
        <button type="button" class="btn-ghost" @click="modelo">Baixar modelo de planilha</button>
        <p v-if="erroModelo" class="text-sm text-red-600">{{ erroModelo }}</p>
      </div>
    </div>

    <!-- relatório de validação (lê o state e os getters do Pinia) -->
    <section v-if="store.temRelatorio" class="mt-8">
      <h2 class="text-xl font-bold text-ink-900 dark:text-white">Relatório de validação</h2>
      <p class="mt-1 text-slate-500 dark:text-slate-400">Arquivo analisado: <strong class="text-slate-800 dark:text-slate-100">{{ store.arquivoNome }}</strong></p>

      <!-- resumo -->
      <div class="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div class="card !p-4"><p class="text-sm text-slate-500">Total de registros</p><p class="mt-1 text-3xl font-bold">{{ store.totalRegistros }}</p></div>
        <div class="card !p-4"><p class="text-sm text-slate-500">Registros válidos</p><p class="mt-1 text-3xl font-bold text-emerald-600 dark:text-emerald-300">{{ store.registrosValidos }}</p><p class="text-xs text-slate-500">{{ store.percentualValidos.toLocaleString('pt-BR') }}% do total</p></div>
        <div class="card !p-4"><p class="text-sm text-slate-500">Registros com erro</p><p class="mt-1 text-3xl font-bold text-red-600 dark:text-red-300">{{ store.linhasComErro }}</p></div>
        <div class="card !p-4"><p class="text-sm text-slate-500">Válidos com aviso</p><p class="mt-1 text-3xl font-bold text-amber-600 dark:text-amber-300">{{ store.registrosComAviso }}</p></div>
      </div>

      <!-- quantidade de cada tipo -->
      <div class="mt-4 grid gap-4 md:grid-cols-2">
        <div class="card overflow-x-auto">
          <h2 class="font-semibold text-ink-900 dark:text-white">Erros por tipo</h2>
          <table class="mt-3 w-full text-left text-sm">
            <thead class="text-slate-500 dark:text-slate-400"><tr><th class="pb-2 font-medium">Validação realizada</th><th class="pb-2 text-right font-medium">Quantidade</th></tr></thead>
            <tbody>
              <tr v-for="t in store.errosPorTipo" :key="t.tipo" class="border-t border-slate-100 dark:border-white/10">
                <td class="py-2">{{ t.rotulo }}</td><td class="py-2 text-right font-semibold" :class="t.quantidade ? 'text-red-600 dark:text-red-300' : ''">{{ t.quantidade }}</td>
              </tr>
              <tr class="border-t border-slate-100 dark:border-white/10"><td class="py-2">Registros válidos</td><td class="py-2 text-right font-semibold text-emerald-600 dark:text-emerald-300">{{ store.registrosValidos }}</td></tr>
            </tbody>
          </table>
        </div>
        <div class="card overflow-x-auto">
          <h2 class="font-semibold text-ink-900 dark:text-white">Ajustes e avisos</h2>
          <table class="mt-3 w-full text-left text-sm">
            <thead class="text-slate-500 dark:text-slate-400"><tr><th class="pb-2 font-medium">Tipo</th><th class="pb-2 text-right font-medium">Quantidade</th></tr></thead>
            <tbody>
              <tr v-for="t in store.avisosPorTipo" :key="t.tipo" class="border-t border-slate-100 dark:border-white/10">
                <td class="py-2">{{ t.rotulo }}</td><td class="py-2 text-right font-semibold" :class="t.quantidade ? 'text-amber-600 dark:text-amber-300' : ''">{{ t.quantidade }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="store.errosPorCampo.length" class="mt-3 text-xs text-slate-500">Campo com mais erros: <strong>{{ campo(store.errosPorCampo[0].campo) }}</strong> ({{ store.errosPorCampo[0].quantidade }}).</p>
        </div>
      </div>

      <!-- detalhes: linha, campo, descrição -->
      <div class="card mt-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="font-semibold text-ink-900 dark:text-white">Detalhes</h2>
          <div class="flex flex-wrap gap-2">
            <button class="rounded-full px-3 py-1 text-sm font-medium" :class="filtro === 'erros' ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-300'" @click="trocar('erros')">Erros ({{ store.erros.length }})</button>
            <button class="rounded-full px-3 py-1 text-sm font-medium" :class="filtro === 'avisos' ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-300'" @click="trocar('avisos')">Avisos ({{ store.avisos.length }})</button>
            <select v-model="tipoSel" class="input !w-auto !py-1.5" aria-label="Filtrar por tipo">
              <option value="">Todos os tipos</option>
              <option v-for="(rotulo, chave) in tipos" :key="chave" :value="chave">{{ rotulo }}</option>
            </select>
          </div>
        </div>
        <div class="mt-3 max-h-96 overflow-auto">
          <table class="w-full min-w-[34rem] text-left text-sm">
            <thead class="sticky top-0 bg-white text-slate-500 dark:bg-ink-900 dark:text-slate-400"><tr><th class="pb-2 font-medium">Linha</th><th class="pb-2 font-medium">Campo</th><th class="pb-2 font-medium">Tipo</th><th class="pb-2 font-medium">Descrição</th></tr></thead>
            <tbody>
              <tr v-if="!itens.length"><td colspan="4" class="py-8 text-center text-slate-400">Nada a mostrar aqui.</td></tr>
              <tr v-for="(x, i) in itens" :key="i" class="border-t border-slate-100 dark:border-white/10">
                <td class="py-2 font-medium">{{ x.linha }}</td>
                <td class="py-2">{{ campo(x.campo) }}</td>
                <td class="py-2"><span class="rounded-full px-2 py-0.5 text-xs font-semibold" :class="filtro === 'erros' ? cor.erro : cor.aviso">{{ tipos[x.tipo] }}</span></td>
                <td class="py-2">{{ x.descricao }}<span v-if="x.valor" class="text-slate-400"> · valor: "{{ x.valor }}"</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- envio -->
      <div v-if="store.etapa === 'concluido'" class="card mt-4">
        <p class="text-lg font-semibold text-emerald-700 dark:text-emerald-300">{{ store.resultado?.status === 'PROCESSADO' ? 'Planilha processada com sucesso' : 'Planilha validada' }}</p>
        <p v-if="store.resultado?.status !== 'PROCESSADO'" class="mt-1 text-sm text-slate-500">O processamento em Python está desligado no backend (ETL_ENABLED=false); os dados não foram gravados.</p>
        <dl class="mt-3 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
          <div><dt class="text-slate-500">Linhas</dt><dd class="text-xl font-bold">{{ store.resultado?.totalLinhas }}</dd></div>
          <div><dt class="text-slate-500">Gravadas</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasValidas }}</dd></div>
          <div><dt class="text-slate-500">Recusadas</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasInvalidas }}</dd></div>
          <div><dt class="text-slate-500">Com aviso</dt><dd class="text-xl font-bold">{{ store.resultado?.linhasComAviso }}</dd></div>
        </dl>
        <ul v-if="store.resultado?.rejeitadas?.length" class="mt-3 max-h-40 space-y-1 overflow-y-auto text-sm text-slate-600 dark:text-slate-300"><li v-for="(r, i) in store.resultado.rejeitadas" :key="i">{{ r }}</li></ul>
        <div class="mt-4 flex flex-wrap gap-3">
          <RouterLink to="/app/dashboard" class="btn-primary">Ver dashboard</RouterLink>
          <button class="btn-ghost" @click="store.reiniciar()">Analisar outra planilha</button>
        </div>
      </div>

      <div v-else class="mt-4 flex flex-wrap gap-3">
        <button class="btn-primary" :disabled="!store.registrosValidos || store.etapa === 'enviando'" @click="store.enviar()">
          {{ store.etapa === 'enviando' ? 'Enviando e processando...' : `Enviar ${store.registrosValidos} registro(s) válido(s)` }}
        </button>
        <button class="btn-ghost" :disabled="store.etapa === 'enviando'" @click="input.click()">Escolher outro arquivo</button>
      </div>
    </section>
  </div>
</template>
