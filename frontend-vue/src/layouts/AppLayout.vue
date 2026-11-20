<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ThemeToggle from '@/components/ThemeToggle.vue'

const auth = useAuthStore()
const router = useRouter()

const links = computed(() => [
  { to: '/app/dashboard', label: 'Dashboard' },
  { to: '/app/upload', label: 'Upload' },
  { to: '/app/insights', label: 'Insights' },
  ...(auth.isAdmin ? [{ to: '/app/usuarios', label: 'Usuários' }] : [])
])

function sair() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-dvh">
    <header class="border-b border-slate-200 bg-white dark:border-white/10 dark:bg-ink-900">
      <!-- celular: logo + ações na 1ª linha, abas na 2ª | a partir de sm: tudo em uma linha -->
      <div class="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-x-3 gap-y-2 px-4 py-3">
        <span class="text-lg font-bold tracking-[0.2em] text-ink-900 dark:text-white">DATUM</span>
        <nav class="order-last flex w-full gap-1 overflow-x-auto sm:order-none sm:w-auto" aria-label="Principal">
          <RouterLink v-for="l in links" :key="l.to" :to="l.to" class="nav-pill">{{ l.label }}</RouterLink>
        </nav>
        <div class="flex items-center gap-2 sm:gap-3">
          <div class="hidden text-right leading-tight lg:block">
            <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{ auth.usuario?.nome }}</p>
            <p class="text-xs text-slate-500 dark:text-slate-400">{{ auth.usuario?.email }}</p>
          </div>
          <ThemeToggle />
          <button class="btn-ghost !py-2.5 sm:!py-2" @click="sair">Sair</button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-7xl px-4 py-6 sm:py-8">
      <RouterView />
    </main>
  </div>
</template>
