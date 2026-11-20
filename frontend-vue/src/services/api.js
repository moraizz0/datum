import axios from 'axios'

// Instância única do Axios: toda chamada ao backend passa por aqui.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
})

// "Lembrar de mim" ligado => token no localStorage; desligado => sessionStorage
const lerToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')

// Envia o token JWT em toda requisição
api.interceptors.request.use((config) => {
  const token = lerToken()
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// Token expirado/inválido: limpa a sessão e volta ao login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !err.config.url.includes('/auth/')) {
      for (const s of [localStorage, sessionStorage]) {
        s.removeItem('token')
        s.removeItem('usuario')
      }
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// Texto de erro para mostrar ao usuário (formato do backend: {"erro": "...", "campos": {...}})
export function mensagemErro(e, padrao) {
  if (!e.response) return 'Não foi possível conectar ao servidor. Tente novamente.'
  const dados = e.response.data
  if (dados?.campos) return Object.values(dados.campos)[0] || dados.erro || padrao
  return dados?.erro || padrao
}

export default api
