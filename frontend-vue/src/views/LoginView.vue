<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { mensagemErro } from '@/services/api'
import AuthLayout from '@/components/AuthLayout.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref(localStorage.getItem('emailLembrado') || '')
const senha = ref('')
const lembrar = ref(true)
const erro = ref('')
const carregando = ref(false)

async function entrar() {
  erro.value = ''
  carregando.value = true
  try {
    await auth.login(email.value.trim(), senha.value, lembrar.value)
    if (lembrar.value) localStorage.setItem('emailLembrado', email.value.trim())
    else localStorage.removeItem('emailLembrado')
    router.push(route.query.redirect || '/app/dashboard')
  } catch (e) {
    erro.value = mensagemErro(e, 'Não foi possível entrar. Verifique e-mail e senha.')
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Bem-vindo de volta!" subtitulo="Faça login para acessar sua conta">
    <form class="space-y-4" @submit.prevent="entrar">
      <div>
        <label class="label" for="email">E-mail</label>
        <input id="email" v-model="email" type="email" class="input" placeholder="seu@email.com" required autocomplete="email" />
      </div>
      <div>
        <label class="label" for="senha">Senha</label>
        <input id="senha" v-model="senha" type="password" class="input" placeholder="Digite sua senha" required autocomplete="current-password" />
      </div>
      <div class="flex items-center justify-between gap-3 text-sm">
        <label class="flex cursor-pointer items-center gap-2 text-slate-600 dark:text-slate-300">
          <input v-model="lembrar" type="checkbox" class="h-4 w-4 rounded border-slate-300 accent-brand-600" />
          Lembrar de mim
        </label>
        <RouterLink to="/esqueci-senha" class="font-medium text-brand-600 hover:underline dark:text-brand-100">Esqueci a senha</RouterLink>
      </div>
      <p v-if="erro" class="rounded-xl bg-red-50 dark:bg-red-950/40 px-4 py-3 text-sm text-red-700 dark:text-red-300" role="alert">{{ erro }}</p>
      <button class="btn-primary w-full" :disabled="carregando">{{ carregando ? 'Entrando...' : 'Entrar' }}</button>
    </form>
    <template #rodape>
      Não tem conta? <RouterLink to="/cadastro" class="font-semibold text-brand-600 hover:underline dark:text-brand-100">Solicitar acesso</RouterLink>
    </template>
  </AuthLayout>
</template>
