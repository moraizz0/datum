import { defineStore } from 'pinia'

// Guarda se o modo escuro está ligado. O estado inicial vem da classe "dark"
// que o script do index.html já aplicou (tema salvo ou preferência do sistema).
export const useTemaStore = defineStore('tema', {
  state: () => ({ escuro: document.documentElement.classList.contains('dark') }),
  actions: {
    alternar() {
      this.escuro = !this.escuro
      document.documentElement.classList.toggle('dark', this.escuro)
      try { localStorage.setItem('tema', this.escuro ? 'escuro' : 'claro') } catch (e) { /* ignora */ }
    }
  }
})
