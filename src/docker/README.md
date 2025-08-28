# ⚽ App Game – Jakarta EE 10 (WildFly + Postgres)

Aplicação **Jakarta EE 10** rodando no **WildFly** com **Postgres** via Docker.  
Implementa uma **API REST para gerenciamento de jogos de futebol**, já com operações de **criação de jogo** e **consulta por ID**.

---

## 🚀 Como subir o ambiente

Na raiz do projeto, execute:

```bash
docker compose up -d --build
```

Isso vai iniciar:
- **Postgres** (porta `5432`)
- **WildFly** (porta `8080` para a aplicação e `9990` para o console admin)

---

## 🛠️ Build do projeto

Para compilar e gerar o WAR da aplicação:

```bash
mvn clean package
```

---

## 📦 Deploy no WildFly

### 🔹 Opção A – Deploy automático via volume `./deployments`

```bash
cp target/app-game-1.0-SNAPSHOT.war ./deployments/

# (opcional) remova marcadores de falha se existirem
rm -f ./deployments/app-game-1.0-SNAPSHOT.war.failed       ./deployments/app-game-1.0-SNAPSHOT.war.undeployed
```

### 🔹 Opção B – Deploy via plugin Maven (se configurado)

```bash
mvn wildfly:deploy
```

---

## 📡 Testando a API

> **Base URL:** `http://localhost:8080/app-game-1.0-SNAPSHOT/api`

### ➕ Criar jogo
**Request**
```bash
curl --location 'http://localhost:8080/app-game-1.0-SNAPSHOT/api/jogos'   --header 'Content-Type: application/json'   --data '{
    "timeA": "Time dos Desenvolvedores",
    "timeB": "Time dos Analistas",
    "dataHoraPartida": "2025-08-28T20:00:00"
}'
```

**Response (exemplo)**
```json
{
  "dataHoraPartida":"2025-08-28T20:00:00",
  "id":1,
  "placarA":0,
  "placarB":0,
  "statusJogo":"EM_ANDAMENTO",
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
  "statusJogo":"EM_ANDAMENTO",
  "timeA":"Time dos Desenvolvedores",
  "timeB":"Time dos Analistas"
}
```

---

## ✅ Status atual

- [x] Ambiente Docker configurado  
- [x] WildFly conectado ao Postgres  
- [x] Deploy automático funcionando  
- [x] API para **criar jogo** (`POST /api/jogos`)  
- [x] API para **buscar jogo por ID** (`GET /api/jogos/{id}`)  

---

## 🔑 Acessos úteis

- **Aplicação REST**: [http://localhost:8080/app-game-1.0-SNAPSHOT/api](http://localhost:8080/app-game-1.0-SNAPSHOT/api)  
- **Console Admin WildFly**: [http://localhost:9990](http://localhost:9990)  
  - Usuário: `admin`  
  - Senha: `admin123`  

---
