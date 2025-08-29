# ⚽ App Game – Jakarta EE 10 (WildFly + Postgres + RabbitMQ)

Aplicação **Jakarta EE 10** rodando no **WildFly** com **Postgres** via Docker.
Front-end em Apache Wicket.
Implementa uma **API REST para gerenciamento de jogos de futebol**, com operações de **criação**, **consulta por ID** e **listagem com filtros**.  
O ambiente inclui **RabbitMQ** para eventos e atualização da lista em tempo (quase) real via long‑polling.

---

## 🚀 Como subir o ambiente

Na raiz do projeto, execute:

```bash
docker compose up -d --build
```

Isso vai iniciar:
- **Postgres** (porta `5432`)
- **WildFly** (porta `8080` para a aplicação e `9990` para o console admin)
- **RabbitMQ** (porta `5672` para AMQP e `15672` para o console web)
- **Adminer** (porta `8081` para o console web de gerenciamento de banco de dados)

> 🧩 **Observação:** O serviço **RabbitMQ** foi adicionado ao `docker-compose.yml`.

---

## 🛠️ Build do projeto

Para compilar e gerar o WAR da aplicação:

```bash
mvn clean package
```

O artefato será gerado em: `target/app-game-1.0-SNAPSHOT.war`.

---

## 📦 Deploy no WildFly (manual via console)

1. Acesse o console do WildFly: `http://localhost:9990`
2. Faça login (credenciais abaixo).
3. Vá em **Deployments** → **Add** → **Upload a new deployment**.
4. Selecione `target/app-game-1.0-SNAPSHOT.war` → **Next** → **Finish**.
5. Confirme que o deployment está **Enabled**.

> Se já houver uma versão anterior, remova/desabilite antes de subir o novo WAR.

---

## 📡 Testando a API

**Base URL:** `http://localhost:8080/app-game-1.0-SNAPSHOT/api`

### ➕ Criar jogo
**Request**
```bash
curl --location 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos'   --header 'Content-Type: application/json'   --data '{
    "timeA": "Time dos Desenvolvedores",
    "timeB": "Time dos Analistas",
    "dataHoraPartida": "2025-08-28T20:00:00",
    "statusJogo": "NAO_INICIADO",
    "placarA": 0,
    "placarB": 0
}'
```

**Response (exemplo)**
```json
{
  "dataHoraPartida":"2025-08-28T20:00:00",
  "id":1,
  "placarA":0,
  "placarB":0,
  "statusJogo":"NAO_INICIADO",
  "timeA":"Time dos Desenvolvedores",
  "timeB":"Time dos Analistas"
}
```

---

### 🔍 Buscar jogo por ID
**Request**
```bash
curl --location 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos/1'
```

**Response (exemplo)**
```json
{
  "dataHoraPartida":"2025-08-28T20:00:00",
  "id":1,
  "placarA":0,
  "placarB":0,
  "statusJogo":"NAO_INICIADO",
  "timeA":"Time dos Desenvolvedores",
  "timeB":"Time dos Analistas"
}
```

---

### 📃 Listar todos os jogos
```bash
curl -s 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos'
```

### 🔎 Listar com filtros (opcionais)

- Por **status** (`EM_ANDAMENTO`, `FINALIZADO`):
```bash
curl -s 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos?status=EM_ANDAMENTO'
```

- Por **intervalo de data/hora** (`de`, `ate` em ISO-8601):
```bash
curl -s 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos?de=2025-08-29T00:00:00&ate=2025-08-29T23:59:59'
```

- Combinando **status + intervalo**:
```bash
curl -s 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos?status=FINALIZADO&de=2025-08-01T00:00:00&ate=2025-08-31T23:59:59'
```

---

## ✅ Status atual

- [x] Ambiente Docker configurado (Postgres, WildFly **e RabbitMQ**)
- [x] WildFly conectado ao Postgres
- [x] Deploy manual via console do WildFly com WAR gerado por `mvn clean package`
- [x] API para **criar jogo** (`POST /api/jogos`)
- [x] API para **buscar jogo por ID** (`GET /api/jogos/{id}`)
- [x] API para **listar jogos** (`GET /api/jogos`) com **filtros** (`status`, `de`, `ate`)

### ⚠️ Pendências / Não concluído

- [ ] **Serviço com banco Redis** ainda **não** foi adicionado.
- [ ] **Long-poll** retornando **erro 400** na tela de listagem(investigação em andamento).
- [ ] Possível **erro no consumer do RabbitMQ** (monitorar logs/consumo).
- [ ] **Endpoint para atualização de placar** ainda não adicionado.

---

## 🔑 Acessos úteis

- **Aplicação REST (base):**  
  `http://localhost:8080/app-game-1.0-SNAPSHOT/api`

- **Console Admin WildFly:**  
  `http://localhost:9990`  
  **Usuário:** `leonardo`  
  **Senha:** `admin`

- **Banco de Dados (Postgres):**  
  **Host:** `db` · **Porta:** `5432`  
  **Usuário:** `${POSTGRES_USER}`  
  **Senha:** `${POSTGRES_PASSWORD}`  
  **Database:** `${POSTGRES_DB}`

- **RabbitMQ (Console Web):**  
  `http://localhost:15672`  
  **Usuário:** `${RABBITMQ_USER}` (padrão: `guest`)  
  **Senha:** `${RABBITMQ_PASS}` (padrão: `guest`)  
  **AMQP:** `amqp://rabbitmq:5672` (dentro da rede Docker)

---

> Dica: para alterar usuários/senhas do RabbitMQ em desenvolvimento, ajuste as variáveis
> `RABBITMQ_USER` e `RABBITMQ_PASS` no seu `.env` e recrie os serviços.