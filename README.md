# Datum

Sistema web que lê planilhas de clientes e gera indicadores e insights para apoiar a tomada de decisão da empresa.

Projeto Integrador individual do segundo semestre do curso de Análise e Desenvolvimento de Sistemas (SENAI).

## Como funciona

O usuário envia uma planilha de clientes. O sistema valida os dados, calcula estatísticas e mostra o resultado em um dashboard e em uma tela de insights.

```
Front-end (Vue)  ->  Back-end (Spring Boot)  ->  Python (FastAPI)  ->  PostgreSQL (Azure)
valida e envia       valida e repassa            valida, limpa,         guarda os dados
                                                 calcula e grava
```

- O front-end valida a planilha no Pinia e mostra o relatório de validação na tela de Upload.
- O back-end valida de novo, cuida de usuários e login (JWT) e chama o Python.
- O Python valida mais uma vez, limpa e padroniza os dados, calcula as estatísticas e grava no banco.
- O dashboard e os insights apenas leem o que o Python gravou.

A validação é repetida em cada camada de propósito: o front-end pode ser contornado, então o servidor não confia nele.

## Tecnologias

- Front-end: Vue 3, Vite, Pinia, Vue Router, Axios, Tailwind CSS, Chart.js, xlsx
- Back-end: Java 21, Spring Boot 3.5, Spring Security, JWT, JPA
- Análise de dados: Python, FastAPI, pandas, SQLAlchemy
- Banco de dados: PostgreSQL (Azure Database for PostgreSQL)

## Estrutura de pastas

```
datum/
  frontend/          telas, store do Pinia e chamadas à API
  backend-java/      API, autenticação e envio ao Python
  analytics-python/  validação, limpeza, estatísticas e gravação no banco
```

## Pré-requisitos

- Node.js 20 ou superior
- Java 21 e Maven
- Python 3.11 ou superior
- Um banco PostgreSQL acessível (no projeto, o Azure Database for PostgreSQL, com o IP do seu computador liberado no firewall)

## Configuração

Cada parte tem um arquivo `.env`, criado a partir do `.env.example` da própria pasta. Os arquivos `.env` nunca devem ir para o Git.

### Chave entre o back-end e o Python

É uma senha que você inventa para o Python aceitar apenas pedidos do seu back-end. Gere uma com:

```
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

O mesmo valor vai em `ETL_API_KEY` (Python) e em `ETL_KEY` (back-end).

### analytics-python/.env

```
DB_HOST=SEU-SERVIDOR.postgres.database.azure.com
DB_PORT=5432
DB_NAME=postgres
DB_USER=seu-usuario
DB_PASSWORD=sua-senha
DB_SSLMODE=require
ETL_API_KEY=a-chave-gerada
```

### backend-java/.env

```
DB_URL=jdbc:postgresql://SEU-SERVIDOR.postgres.database.azure.com:5432/postgres?sslmode=require
DB_USER=seu-usuario
DB_PASSWORD=sua-senha
JWT_SECRET=texto-aleatorio-com-pelo-menos-32-caracteres
JWT_EXPIRATION_MINUTES=480
CORS_ORIGINS=http://localhost:5173
ADMIN_NOME=Administrador
ADMIN_EMAIL=admin@datum.local
ADMIN_PASSWORD=senha-forte-do-administrador
UPLOAD_MAX_LINHAS=5000
ETL_ENABLED=true
ETL_URL=http://localhost:8000/etl
ETL_KEY=a-chave-gerada
```

O arquivo `.env` do back-end deve ficar na mesma pasta do `pom.xml`. Evite `\` e aspas nos valores. Se `ETL_ENABLED` ficar `false`, a planilha é apenas validada e nada é gravado.

### frontend/.env

```
VITE_API_URL=http://localhost:8080
VITE_MOCK_AUTH=false
```

Com `VITE_MOCK_AUTH=true`, o login é simulado e não usa o back-end. Para usar o sistema de verdade, deixe `false`.

## Como executar

Suba as três partes, nesta ordem. Na primeira vez, o back-end cria as tabelas no banco sozinho.

**1. Back-end** (porta 8080)

```
cd backend-java
mvn spring-boot:run
```

Teste: abra http://localhost:8080/api/health. Deve responder `{"status":"ok"}`.

**2. Python** (porta 8000)

```
cd analytics-python
python -m venv .venv
.venv\Scripts\Activate.ps1        (Windows)   |   source .venv/bin/activate   (Linux/Mac)
pip install -r requirements.txt
uvicorn api:app --port 8000
```

Teste: abra http://localhost:8000/saude. Para conferir a conexão com o banco, rode `python testar_conexao.py`.

**3. Front-end** (porta 5173)

```
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173.

## Primeiro acesso

1. Entre com o administrador: e-mail definido em `ADMIN_EMAIL` e senha definida em `ADMIN_PASSWORD`.
2. Novos usuários se cadastram na tela de cadastro e ficam como Pendente.
3. O administrador abre a tela Usuários e aprova ou bloqueia cada solicitação. Só usuários aprovados conseguem entrar.

## Telas

- Home, Login e Cadastro: acesso ao sistema.
- Upload: envio da planilha. Mostra, na mesma tela, o relatório de validação (total de registros, válidos, com erro, quantidade de cada tipo de erro, e a linha, o campo e a descrição de cada problema). Só as linhas válidas são enviadas.
- Dashboard: indicadores (total de clientes, faturamento, média, mediana, desvio padrão), gráficos por segmento, nível, consultor, serviço e mês, e a lista dos maiores clientes.
- Insights: destaques, oportunidades e pontos de atenção gerados a partir do último lote processado.
- Usuários (somente administrador): lista, aprova, bloqueia e reativa usuários.

O modo escuro pode ser ligado no botão do topo.

## Formato da planilha

Aceita `.xlsx`, `.xls` e `.csv`, com até 5 MB e 5.000 linhas. Use o botão "Baixar modelo de planilha" na tela de Upload. As colunas são:

| Coluna | Obrigatória | Observação |
|---|---|---|
| codigo_cliente | sim | Letras, números, "-" ou "_". Não pode repetir. |
| nome_cliente | sim | |
| consultor | sim | |
| segmento | sim | Indústria, Comércio, Serviços, Saúde, Educação ou Tecnologia |
| nivel_cliente | sim | A, B ou C |
| faturamento_anual | sim | Número maior ou igual a zero |
| servicos_contratados | sim | Separados por ponto e vírgula |
| data_contratacao | sim | dd/mm/aaaa, a partir de 1990 |
| cidade | não | |
| uf | não | 2 letras |
| email | não | Se existir, o formato é validado |

## Validações

- Erros (a linha não é enviada): campo obrigatório vazio, código duplicado, dado fora do padrão (segmento, nível, faturamento, data, UF, código) e e-mail inválido.
- Avisos (a linha segue, já corrigida): espaços sobrando, textos padronizados (por exemplo "IND." vira Indústria, "sp" vira SP) e possível duplicidade (mesmo nome, faturamento e data com outro código).

## Principais rotas

Back-end (porta 8080):

| Rota | Quem acessa | Função |
|---|---|---|
| POST /api/auth/register | público | Cadastro (fica pendente) |
| POST /api/auth/login | público | Login, devolve o token JWT |
| GET /api/auth/me | logado | Dados do usuário atual |
| GET /api/health | público | Verifica se a API está no ar |
| GET /api/admin/usuarios | administrador | Lista usuários |
| PATCH /api/admin/usuarios/{id}/status | administrador | Aprova, bloqueia ou reativa |
| POST /api/uploads/validadas | logado | Recebe as linhas validadas e chama o Python |
| GET /api/dashboard | logado | Estatísticas do último lote (ou de `?loteId=`) |
| GET /api/dashboard/lotes | logado | Últimos 20 lotes |

Python (porta 8000):

| Rota | Função |
|---|---|
| GET /saude | Verifica se o serviço está no ar |
| POST /validar | Só valida as linhas |
| POST /testar | Valida e calcula, sem gravar |
| POST /etl | Valida, calcula e grava no banco (exige o cabeçalho `X-ETL-Key`) |

A documentação interativa do Python fica em http://localhost:8000/docs.

## Banco de dados

O back-end cria as tabelas ao iniciar (arquivo `backend-java/src/main/resources/schema.sql`):

- usuario
- upload_lote: cada planilha enviada, com status PROCESSANDO, PROCESSADO ou ERRO
- cliente, consultor, servico e contrato: dados da planilha
- estatistica_lote: o JSON com as estatísticas que o dashboard lê

## Testes

Python (na pasta analytics-python, com o ambiente virtual ligado):

```
python -m pytest tests -q
```

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Não conecta ao banco (timeout) | O IP público da sua rede não está liberado no firewall do Azure |
| Volta para o login ao abrir Usuários ou Upload | `VITE_MOCK_AUTH` não está `false`, ou o front não foi reiniciado |
| O upload responde 502 | `ETL_KEY` diferente de `ETL_API_KEY` |
| O upload responde 503 | O Python está desligado |
| "Rota não encontrada" no Dashboard | O back-end não foi reiniciado depois de copiar os arquivos novos |
| Planilha "validada" mas dashboard vazio | `ETL_ENABLED` está `false` |

Depois de alterar um `.env`, reinicie o programa que o lê.

## Limitações

- Cada planilha enviada gera um novo lote. O dashboard mostra o último lote processado, e é possível escolher outro na lista.
