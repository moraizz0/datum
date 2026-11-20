<script setup>
import { computed } from 'vue'
const props = defineProps({ status: String })
const cor = computed(() => ({
  PROCESSADO: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300',
  PROCESSANDO: 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300',
  ERRO: 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300'
}[props.status] || 'bg-slate-100 text-slate-700 dark:bg-white/10 dark:text-slate-300'))
</script>

<template>
  <span class="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="cor">{{ status }}</span>
</template>
