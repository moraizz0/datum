<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { mensagemErro } from '@/services/api'
import AuthLayout from '@/components/AuthLayout.vue'

const auth = useAuthStore()
const email = ref('')
const enviado = ref(false)
const erro = ref('')
const carregando = ref(false)

async function enviar() {
  erro.value = ''
  if (!/^\S+@\S+\.\S+$/.test(email.value.trim())) {
    erro.value = 'Informe um e-mail válido.'
    return
  }
  carregando.value = true
  try {
    await auth.recuperarSenha(email.value.trim())
    enviado.value = true
  } catch (e) {
    erro.value = mensagemErro(e, 'Não foi possível enviar agora. Tente novamente.')
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Esqueceu a senha?" subtitulo="Informe seu e-mail e enviaremos as instruções para redefinir">
    <!-- mensagem neutra: não revela se o e-mail existe ou não -->
    <div v-if="enviado" class="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300" role="status">
      Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha.
    </div>
    <form v-else class="space-y-4" novalidate @submit.prevent="enviar">
      <div>
        <label class="label" for="email">E-mail</label>
        <input id="email" v-model="email" type="email" class="input" placeholder="seu@email.com" autocomplete="email" />
      </div>
      <p v-if="erro" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300" role="alert">{{ erro }}</p>
      <button class="btn-primary w-full" :disabled="carregando">{{ carregando ? 'Enviando...' : 'Enviar instruções' }}</button>
    </form>
    <template #rodape>
      <RouterLink to="/login" class="font-semibold text-brand-600 hover:underline dark:text-brand-100">Voltar ao login</RouterLink>
    </template>
  </AuthLayout>
</template>
