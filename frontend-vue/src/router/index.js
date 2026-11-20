import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import CadastroView from '@/views/CadastroView.vue'
import EsqueciSenhaView from '@/views/EsqueciSenhaView.vue'
import AppLayout from '@/layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/cadastro', name: 'cadastro', component: CadastroView },
    { path: '/esqueci-senha', name: 'esqueci-senha', component: EsqueciSenhaView },
    {
      path: '/app',
      component: AppLayout,
      redirect: '/app/dashboard',
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'upload', name: 'upload', component: () => import('@/views/UploadView.vue') },
        { path: 'insights', name: 'insights', component: () => import('@/views/InsightsView.vue') },
        { path: 'usuarios', name: 'usuarios', component: () => import('@/views/UsuariosView.vue'), meta: { admin: true } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// Guard: rotas privadas exigem login; rotas admin exigem perfil ADMIN.
// (o backend continua sendo quem realmente autoriza)
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.logado) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return '/app/dashboard'
  if (['login', 'cadastro', 'esqueci-senha'].includes(to.name) && auth.logado) return '/app/dashboard'
})

export default router
