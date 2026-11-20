<script setup>
import ThemeToggle from '@/components/ThemeToggle.vue'

const barras = [40, 62, 48, 82, 58, 72, 45]
const dias = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']
const kpis = [['Clientes ativos', '1.250'], ['Faturamento médio', 'R$ 450 mil'], ['Serviços ativos', '3.420']]
const recursos = [
  { n: '01', titulo: 'Envie a planilha', texto: 'Arraste seu arquivo Excel ou CSV e deixe o Datum ler tudo por você.' },
  { n: '02', titulo: 'Dados validados', texto: 'Erros de digitação e inconsistências são encontrados e padronizados antes de salvar.' },
  { n: '03', titulo: 'Decida com insights', texto: 'Dashboards mostram segmentos, clientes e serviços que mais trazem retorno.' }
]
</script>

<template>
  <div class="min-h-dvh">
    <header class="border-b border-slate-200 bg-white dark:border-white/10 dark:bg-ink-900">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
        <span class="text-lg font-bold tracking-[0.2em] text-ink-900 dark:text-white">DATUM</span>
        <nav class="hidden gap-8 text-sm text-slate-600 md:flex dark:text-slate-300">
          <a href="#recursos" class="hover:text-slate-900 dark:hover:text-white">Recursos</a>
        </nav>
        <div class="flex items-center gap-2 sm:gap-3">
          <ThemeToggle />
          <RouterLink to="/login" class="btn-ghost !py-2.5 sm:!py-2">Entrar</RouterLink>
          <RouterLink to="/cadastro" class="btn-primary hidden !py-2 sm:inline-flex">Criar conta</RouterLink>
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-6xl px-4 sm:px-6">
      <section class="mx-auto max-w-3xl pt-14 text-center sm:pt-24">
        <span class="inline-block rounded-md bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700 dark:bg-white/10 dark:text-brand-100">
          Da planilha ao insight
        </span>
        <h1 class="mt-5 text-4xl font-bold leading-tight text-ink-900 sm:text-5xl dark:text-white">
          Transforme dados em <span class="text-brand-600 dark:text-brand-500">decisões</span>
        </h1>
        <p class="mx-auto mt-5 max-w-xl text-base text-slate-600 sm:text-lg dark:text-slate-400">
          O Datum lê suas planilhas, corrige inconsistências e entrega dashboards para você enxergar o desempenho da empresa.
        </p>
        <div class="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <RouterLink to="/cadastro" class="btn-primary w-full px-7 sm:w-auto">Comece agora</RouterLink>
          <RouterLink to="/login" class="btn-ghost w-full !py-3 sm:w-auto">Já tenho conta</RouterLink>
        </div>
      </section>

      <!-- prévia do dashboard (decorativa) -->
      <section class="card mx-auto mt-14 max-w-4xl !p-4 sm:!p-6" aria-hidden="true">
        <div class="grid gap-3 sm:grid-cols-3">
          <div v-for="k in kpis" :key="k[0]" class="rounded-xl bg-slate-50 p-4 dark:bg-white/5">
            <p class="text-xs text-slate-500 dark:text-slate-400">{{ k[0] }}</p>
            <p class="mt-1 text-2xl font-bold text-ink-900 dark:text-white">{{ k[1] }}</p>
          </div>
        </div>
        <div class="mt-3 rounded-xl bg-slate-50 p-4 dark:bg-white/5">
          <p class="text-sm font-semibold text-ink-900 dark:text-white">Faturamento por dia</p>
          <div class="mt-4 flex h-40 items-end justify-between gap-3">
            <div v-for="(b, i) in barras" :key="i" class="flex h-full flex-1 flex-col items-center justify-end gap-2">
              <div class="w-full rounded-t-md bg-brand-600" :style="{ height: b + '%' }"></div>
              <span class="text-[10px] text-slate-500 dark:text-slate-400">{{ dias[i] }}</span>
            </div>
          </div>
        </div>
      </section>

      <section id="recursos" class="grid gap-4 py-16 sm:py-20 md:grid-cols-3">
        <article v-for="r in recursos" :key="r.n" class="card">
          <span class="text-sm font-bold text-brand-600 dark:text-brand-500">{{ r.n }}</span>
          <h3 class="mt-3 text-lg font-semibold text-ink-900 dark:text-white">{{ r.titulo }}</h3>
          <p class="mt-2 text-sm text-slate-600 dark:text-slate-400">{{ r.texto }}</p>
        </article>
      </section>
    </main>

    <footer class="border-t border-slate-200 py-6 text-center text-xs text-slate-500 dark:border-white/10 dark:text-slate-400">
      Datum · Projeto Integrador SENAI
    </footer>
  </div>
</template>
