<script setup>
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { mensagemErro } from '@/services/api'
import AuthLayout from '@/components/AuthLayout.vue'

const auth = useAuthStore()
const form = ref({ nome: '', email: '', senha: '', confirmar: '' })
const tentou = ref(false)
const mensagem = ref('')
const erroApi = ref('')
const carregando = ref(false)

// Validações do formulário (lista de problemas encontrados)
const problemas = computed(() => {
  const f = form.value
  const lista = []
  if (f.nome.trim().length < 3) lista.push('Informe seu nome completo.')
  if (!/^\S+@\S+\.\S+$/.test(f.email)) lista.push('Informe um e-mail válido.')
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(f.senha)) lista.push('A senha deve ter de 8 a 72 caracteres, com letras e números.')
  if (f.senha !== f.confirmar) lista.push('As senhas não conferem.')
  return lista
})

async function cadastrar() {
  tentou.value = true
  erroApi.value = ''
  if (problemas.value.length) return
  carregando.value = true
  try {
    await auth.cadastrar({ nome: form.value.nome.trim(), email: form.value.email.trim(), senha: form.value.senha })
    mensagem.value = 'Solicitação enviada! Aguarde a aprovação do administrador.'
  } catch (e) {
    erroApi.value = mensagemErro(e, 'Não foi possível enviar a solicitação.')
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <AuthLayout titulo="Seja bem-vindo!" subtitulo="Solicite seu acesso ao Datum">
    <p v-if="mensagem" class="rounded-xl bg-emerald-50 dark:bg-emerald-950/40 px-4 py-3 text-sm text-emerald-700 dark:text-emerald-300" role="status">{{ mensagem }}</p>
    <form v-else class="space-y-4" novalidate @submit.prevent="cadastrar">
      <div>
        <label class="label" for="nome">Nome</label>
        <input id="nome" v-model="form.nome" class="input" placeholder="Maria Silva" autocomplete="name" />
      </div>
      <div>
        <label class="label" for="email">E-mail</label>
        <input id="email" v-model="form.email" type="email" class="input" placeholder="maria@email.com" autocomplete="email" />
      </div>
      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label class="label" for="senha">Senha</label>
          <input id="senha" v-model="form.senha" type="password" class="input" autocomplete="new-password" />
        </div>
        <div>
          <label class="label" for="confirmar">Confirmar senha</label>
          <input id="confirmar" v-model="form.confirmar" type="password" class="input" autocomplete="new-password" />
        </div>
      </div>
      <ul v-if="tentou && problemas.length" class="list-disc space-y-1 rounded-xl bg-red-50 dark:bg-red-950/40 py-3 pl-8 pr-4 text-sm text-red-700 dark:text-red-300" role="alert">
        <li v-for="p in problemas" :key="p">{{ p }}</li>
      </ul>
      <p v-if="erroApi" class="rounded-xl bg-red-50 dark:bg-red-950/40 px-4 py-3 text-sm text-red-700 dark:text-red-300" role="alert">{{ erroApi }}</p>
      <button class="btn-primary w-full" :disabled="carregando">{{ carregando ? 'Enviando...' : 'Enviar solicitação' }}</button>
    </form>
    <template #rodape>
      Já tem conta? <RouterLink to="/login" class="font-semibold text-brand-600 hover:underline dark:text-brand-100">Entrar</RouterLink>
    </template>
  </AuthLayout>
</template>
