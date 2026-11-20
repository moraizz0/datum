<script setup>

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
