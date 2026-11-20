# Datum — Front-end (Vue 3 + Vite + Tailwind v4 + Pinia)

## Rodar
```
npm install
cp .env.example .env     # (Windows: copy .env.example .env)
npm run dev              # http://localhost:5173
```
Com `VITE_MOCK_AUTH=true` o login funciona sem backend:
e-mail começando com "admin" entra como ADMIN (vê o menu Usuários); qualquer outro entra como USER.

## Estrutura
- `src/style.css` → tokens de cor/fonte (@theme) e classes base (.card, .btn-*, .input)
- `src/router` → rotas públicas, privadas (/app/*) e guard por perfil
- `src/stores/auth.js` → Pinia: token, usuário, logado, isAdmin
- `src/services/api.js` → Axios + token JWT
- `src/views` → Home, Login, Cadastro, Dashboard, Upload, Usuarios
- `src/layouts/AppLayout.vue` → barra superior + área interna

## Próximas etapas
Upload com uploadStore + validações (xlsx) → Usuários (admin) → Dashboard (Chart.js) → integração com o backend.

## Recursos adicionais
- **Modo escuro**: botão (sol/lua) no topo; escolha salva no navegador (`stores/tema.js`). Sem escolha, segue o tema do sistema.
- **Lembrar de mim**: marcado = continua logado ao fechar o navegador; desmarcado = sessão só da aba.
- **Esqueci a senha**: tela `/esqueci-senha`. Falta o endpoint no backend (`POST /api/auth/forgot-password`).
- Login simulado fica ligado em `npm run dev`; para usar o backend real: `VITE_MOCK_AUTH=false`.
