import { defineStore } from 'pinia'
import api, { mensagemErro } from '@/services/api'

// Tela do administrador: lista usuários (GET) e muda o status (PATCH).
export const useUsuariosStore = defineStore('usuarios', {
  state: () => ({ lista: [], carregando: false, erro: '', alterando: null }),
  getters: {
    pendentes: (s) => s.lista.filter((u) => u.status === 'PENDENTE').length
  },
  actions: {
    async carregar() {
      this.carregando = true
      this.erro = ''
      try {
        const { data } = await api.get('/api/admin/usuarios')
        this.lista = data
      } catch (e) {
        this.erro = mensagemErro(e, 'Não foi possível carregar os usuários.')
      } finally {
        this.carregando = false
      }
    },
    // status: 'APROVADO' | 'BLOQUEADO' | 'PENDENTE'
    async alterarStatus(id, status) {
      this.alterando = id
      this.erro = ''
      try {
        const { data } = await api.patch(`/api/admin/usuarios/${id}/status`, { status })
        const i = this.lista.findIndex((u) => u.id === id)
        if (i >= 0) this.lista[i] = data
      } catch (e) {
        this.erro = mensagemErro(e, 'Não foi possível alterar o usuário.')
      } finally {
        this.alterando = null
      }
    }
  }
})
