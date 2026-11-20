import { defineStore } from 'pinia'
import api from '@/services/api'

// Login simulado: ligado no "npm run dev" (a menos que VITE_MOCK_AUTH=false).
// No build de produção fica sempre desligado.
const MOCK = import.meta.env.DEV && import.meta.env.VITE_MOCK_AUTH !== 'false'
const espera = (ms) => new Promise((r) => setTimeout(r, ms))

// ATENÇÃO: confirmar estas rotas com os controllers do backend
const ROTAS = {
  login: '/api/auth/login',
  cadastro: '/api/auth/register',
  recuperarSenha: '/api/auth/forgot-password' // ainda não existe no backend
}

const ler = (chave) => localStorage.getItem(chave) ?? sessionStorage.getItem(chave)

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: ler('token'),
    usuario: JSON.parse(ler('usuario') || 'null')
  }),
  getters: {
    logado: (state) => !!state.token,
    isAdmin: (state) => state.usuario?.perfil === 'ADMIN'
  },
  actions: {
    limparArmazenamento() {
      for (const s of [localStorage, sessionStorage]) {
        s.removeItem('token')
        s.removeItem('usuario')
      }
    },
    // lembrar = true  -> continua logado ao fechar o navegador (localStorage)
    // lembrar = false -> sessão termina ao fechar a aba/navegador (sessionStorage)
    guardarSessao(token, usuario, lembrar = true) {
      this.limparArmazenamento()
      this.token = token
      this.usuario = usuario
      const armazenamento = lembrar ? localStorage : sessionStorage
      armazenamento.setItem('token', token)
      armazenamento.setItem('usuario', JSON.stringify(usuario))
    },
    async login(email, senha, lembrar = true) {
      if (MOCK) {
        await espera(400)
        // no modo simulado, e-mails que começam com "admin" entram como ADMIN
        const perfil = email.toLowerCase().startsWith('admin') ? 'ADMIN' : 'USER'
        return this.guardarSessao('mock-token', { nome: perfil === 'ADMIN' ? 'Administrador' : 'Usuário', email, perfil }, lembrar)
      }
      const { data } = await api.post(ROTAS.login, { email, senha })
      this.guardarSessao(data.token, data.usuario, lembrar)
    },
    async cadastrar({ nome, email, senha }) {
      if (MOCK) return espera(400)
      await api.post(ROTAS.cadastro, { nome, email, senha })
    },
    async recuperarSenha(email) {
      if (MOCK) return espera(500)
      await api.post(ROTAS.recuperarSenha, { email })
    },
    logout() {
      this.token = null
      this.usuario = null
      this.limparArmazenamento()
    }
  }
})
