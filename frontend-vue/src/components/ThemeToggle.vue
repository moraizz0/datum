<script setup>
import { useTemaStore } from '@/stores/tema'

const tema = useTemaStore()
</script>

<template>
  <button
    type="button"
    class="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 transition hover:bg-slate-50 sm:h-10 sm:w-10 dark:border-white/15 dark:bg-transparent dark:text-slate-200 dark:hover:bg-white/5"
    :aria-label="tema.escuro ? 'Ativar modo claro' : 'Ativar modo escuro'"
    :aria-pressed="tema.escuro"
    :title="tema.escuro ? 'Modo claro' : 'Modo escuro'"
    @click="tema.alternar()"
  >
    <!-- sol (aparece no modo escuro) -->
    <svg v-if="tema.escuro" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
    <!-- lua (aparece no modo claro) -->
    <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  </button>
</template>
