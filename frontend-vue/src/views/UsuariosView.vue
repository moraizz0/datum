<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUsuariosStore } from '@/stores/usuarios'

const auth = useAuthStore()
const store = useUsuariosStore()
const filtro = ref('TODOS')

onMounted(() => store.carregar())

const abas = [
  { valor: 'TODOS', rotulo: 'Todos' },
  { valor: 'PENDENTE', rotulo: 'Pendentes' },
  { valor: 'APROVADO', rotulo: 'Aprovados' },
  { valor: 'BLOQUEADO', rotulo: 'Bloqueados' }
]

const visiveis = computed(() =>
  filtro.value === 'TODOS' ? store.lista : store.lista.filter((u) => u.status === filtro.value)
)

const ehEu = (u) => u.email?.toLowerCase() === auth.usuario?.email?.toLowerCase()

const estilo = {
  PENDENTE: 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300',
  APROVADO: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300',
  BLOQUEADO: 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300'
}
const rotulo = { PENDENTE: 'Pendente', APROVADO: 'Aprovado', BLOQUEADO: 'Bloqueado' }

const data = (iso) => (iso ? new Date(iso).toLocaleDateString('pt-BR') : '')
</script>

<template>
  <div>
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-3xl font-bold text-ink-900 dark:text-white">Administração de usuários</h1>
        <p class="mt-1 text-slate-500 dark:text-slate-400">Aprove ou bloqueie as solicitações de acesso.</p>
      </div>
      <button class="btn-ghost" :disabled="store.carregando" @click="store.carregar()">Atualizar</button>
    </div>

    <div class="mt-5 flex flex-wrap gap-2">
      <button
        v-for="a in abas" :key="a.valor"
        class="rounded-full px-4 py-1.5 text-sm font-medium transition"
        :class="filtro === a.valor
          ? 'bg-brand-600 text-white'
          : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-white/10 dark:text-slate-300 dark:hover:bg-white/15'"
        @click="filtro = a.valor"
      >
        {{ a.rotulo }}
        <span v-if="a.valor === 'PENDENTE' && store.pendentes" class="ml-1 rounded-full bg-white/80 px-1.5 text-xs font-bold text-amber-700">{{ store.pendentes }}</span>
      </button>
    </div>

    <p v-if="store.erro" role="alert" class="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{{ store.erro }}</p>

    <div class="card mt-4 overflow-x-auto">
      <table class="w-full min-w-[40rem] text-left text-sm">
        <thead class="text-slate-500 dark:text-slate-400">
          <tr>
            <th class="pb-3 font-medium">Nome</th>
            <th class="pb-3 font-medium">E-mail</th>
            <th class="pb-3 font-medium">Perfil</th>
            <th class="pb-3 font-medium">Cadastro</th>
            <th class="pb-3 font-medium">Status</th>
            <th class="pb-3 font-medium">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.carregando && !store.lista.length"><td colspan="6" class="py-10 text-center text-slate-400">Carregando...</td></tr>
          <tr v-else-if="!visiveis.length"><td colspan="6" class="py-10 text-center text-slate-400">Nenhum usuário nesta lista.</td></tr>
          <tr v-for="u in visiveis" :key="u.id" class="border-t border-slate-100 dark:border-white/10">
            <td class="py-3 font-medium text-slate-900 dark:text-white">{{ u.nome }}<span v-if="ehEu(u)" class="ml-2 text-xs text-slate-400">(você)</span></td>
            <td class="py-3">{{ u.email }}</td>
            <td class="py-3">{{ u.perfil === 'ADMIN' ? 'Administrador' : 'Usuário' }}</td>
            <td class="py-3">{{ data(u.criadoEm) }}</td>
            <td class="py-3"><span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="estilo[u.status]">{{ rotulo[u.status] || u.status }}</span></td>
            <td class="py-3">
              <span v-if="ehEu(u)" class="text-slate-400">—</span>
              <div v-else class="flex gap-2">
                <button v-if="u.status !== 'APROVADO'" class="btn-primary !px-3 !py-1.5" :disabled="store.alterando === u.id" @click="store.alterarStatus(u.id, 'APROVADO')">
                  {{ u.status === 'BLOQUEADO' ? 'Reativar' : 'Aprovar' }}
                </button>
                <button v-if="u.status !== 'BLOQUEADO'" class="btn-ghost !px-3 !py-1.5" :disabled="store.alterando === u.id" @click="store.alterarStatus(u.id, 'BLOQUEADO')">
                  Bloquear
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
