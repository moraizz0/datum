# Datum — Backend (Spring Boot 3.5 · Java 21 · PostgreSQL · JWT)

## O que este backend faz
- **Autenticação**: cadastro (fica PENDENTE), login com JWT e BCrypt, `GET /api/auth/me`.
- **Administração**: listar usuários e aprovar/bloquear (só ADMIN).
- **Upload**: recebe as linhas que o front validou (Pinia), **valida de novo** e envia só as válidas ao Python (ETL).
- O Java grava **somente a tabela `usuario`**. As tabelas da planilha são gravadas pelo Python.

## Rodar
1. Java 21 e Maven instalados. Banco PostgreSQL (Neon) criado.
2. `cp .env.example .env` (Windows: `copy .env.example .env`) e preencha:
   `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mín. 32 caracteres), `ADMIN_PASSWORD`.
   Enquanto o Python não existir, deixe `ETL_ENABLED=false`.
3. `mvn spring-boot:run`
4. Teste: http://localhost:8080/api/health → `{"status":"ok"}`
5. Teste as rotas com `docs/requests.http` (extensão REST Client do VS Code).

O primeiro ADMIN é criado automaticamente com `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
Troque a senha padrão e **nunca** envie o `.env` ao GitHub (já está no `.gitignore`).

## Rotas
| Método | Rota | Quem | O que faz |
|---|---|---|---|
| POST | /api/auth/register | público | cadastro (PENDENTE) → 201 |
| POST | /api/auth/login | público | devolve token + usuário |
| GET | /api/auth/me | logado | usuário do token |
| GET | /api/health | público | saúde da API |
| GET | /api/admin/usuarios?status= | ADMIN | lista usuários |
| PATCH | /api/admin/usuarios/{id}/status | ADMIN | `{"status":"APROVADO"}` ou `"BLOQUEADO"` |
| POST | /api/uploads/validadas | logado | valida e envia ao Python → 201 |

Erros sempre no formato `{"erro": "mensagem", "campos": {...}}`.
Códigos: 400 dados inválidos · 401 sem login/credencial inválida · 403 pendente, bloqueado ou sem permissão ·
404 não encontrado · 409 e-mail já cadastrado · 422 limite de linhas / nenhuma linha válida ·
502 Python respondeu erro · 503 Python fora do ar.

## Estrutura (`src/main/java/br/com/datum`)
- `config/` AppProperties, JwtService, JwtAuthenticationFilter, SecurityConfig, UsuarioDetailsService, AdminInicial, ApiException
- `controller/` AuthController, AdminUsuarioController, UploadController, ErroHandler
- `service/` AuthService, UsuarioService, UploadService, ClienteValidacaoService, PythonEtlService, Colunas, Padronizacao, Datas
- `model/` Usuario, PerfilUsuario, StatusUsuario · `repository/` UsuarioRepository · `dto/` AuthDtos, UsuarioDto, UploadDtos, ClienteDTO

## Validação de cada linha (ClienteValidacaoService)
**Erro (bloqueia a linha):** campo obrigatório vazio · código com caracteres inválidos ou duplicado · nível fora de A/B/C ·
faturamento não numérico, negativo ou ≥ 1 trilhão · nenhum serviço · data inválida ou antes de 1990 · UF com formato errado.
**Aviso (só informa):** espaços extras · segmento/consultor/nível/UF padronizados · segmento desconhecido ·
faturamento zero · data no futuro · possível duplicidade (mesmo nome + valor + data com outro código).
Aceita faturamento como `1850000`, `1.850.000,50`, `R$ 1.850,00` e datas `dd/mm/aaaa`, `aaaa-mm-dd` ou número do Excel.

## Pendente
- **Python/ETL**: o contrato do `POST /etl` (`arquivoNome, recebidas, invalidas, comAviso, linhas[]`) segue o guia da professora; confirme os nomes dos campos com o script Python quando ele existir.
- **Tabelas da planilha** e **endpoint do dashboard/insights** (leitura do que o Python gravou).
- **Esqueci a senha**: não existe endpoint ainda (o guia não prevê). Opção simples: o ADMIN redefine a senha.
- Testes automatizados e limite de tentativas de login.
