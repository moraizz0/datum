<script setup>
import ThemeToggle from '@/components/ThemeToggle.vue'
defineProps({ titulo: String, subtitulo: String })
</script>

<template>
  <div class="relative flex min-h-dvh flex-col items-center justify-center px-4 py-10">
    <div class="absolute right-4 top-4"><ThemeToggle /></div>
    <RouterLink to="/" class="mb-8 text-xl font-bold tracking-[0.2em] text-ink-900 dark:text-white">DATUM</RouterLink>
    <div class="card w-full max-w-md p-6 sm:p-8">
      <h1 class="text-2xl font-bold text-slate-900 dark:text-white">{{ titulo }}</h1>
      <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ subtitulo }}</p>
      <div class="mt-6"><slot /></div>
    </div>
    <p class="mt-6 text-center text-sm text-slate-500 dark:text-slate-400"><slot name="rodape" /></p>
  </div>
</template>
