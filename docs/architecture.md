# Architettura e Diagrammi — JTime4Web

---

## 1. Diagramma Architetturale

```
┌─────────────────────────────────────────────────────────────────┐
│                          UTENTE                                 │
│              Browser desktop / mobile                           │
└────────────────────────┬────────────────────────────────────────┘
                         │  HTTP / WebSocket (porta 8080)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   APPLICAZIONE  (Spring Boot 4)                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  VIEW  (Vaadin 25 — Server-Side Rendering + SPA)         │   │
│  │  ├── Login.java          →  /login                       │   │
│  │  ├── Register.java       →  /register                    │   │
│  │  ├── Calendar.java       →  /tasks  (default)            │   │
│  │  ├── ProjectList.java    →  /projects                    │   │
│  │  ├── MainLayout.java     →  Navbar + Drawer              │   │
│  │  └── DialogBuilder.java  →  Dialoghi riusabili           │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ chiama                                     │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │  SERVICE  (Business Logic)                               │   │
│  │  ├── UserService    → registrazione, utente corrente     │   │
│  │  ├── ProjectService → CRUD progetti, statistiche         │   │
│  │  └── TaskService    → CRUD task, validazione overlap     │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ usa                                        │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │  REPOSITORY  (Spring Data JPA)                           │   │
│  │  ├── UserRepository                                      │   │
│  │  ├── ProjectRepository                                   │   │
│  │  └── TaskRepository                                      │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ JDBC                                       │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │  SECURITY  (Spring Security 6)                           │   │
│  │  ├── SecurityConfig         → filtri, login view         │   │
│  │  ├── UserDetailsServiceImpl → carica utente da DB        │   │
│  │  └── DataInitializer        → crea admin all'avvio       │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────────┘
                         │ JDBC / PostgreSQL Wire Protocol
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   DATABASE  (PostgreSQL 16)                      │
│                                                                  │
│   Tabelle:                                                       │
│   ├── users    (id, username, password)                          │
│   ├── projects (id, name, start_date, end_date, status, user_id) │
│   └── tasks    (id, name, date, start_time, end_time, duration,  │
│                 old_duration, status, project_id, user_id)       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Diagramma Deploy — Docker Compose

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Network                       │
│                                                                  │
│  ┌─────────────────────────┐    ┌──────────────────────────┐    │
│  │  service: app            │    │  service: db              │    │
│  │  image: build da .       │    │  image: postgres:16-alpine│    │
│  │  porta: 8080:8080        │───▶│  porta: 5432 (interna)    │    │
│  │  env_file: .env          │    │  volume: postgres_data    │    │
│  │  depends_on: db healthy  │    │  healthcheck: pg_isready  │    │
│  └─────────────────────────┘    └──────────────────────────┘    │
│                                                                  │
│  volume: postgres_data  (persistenza dati)                       │
└─────────────────────────────────────────────────────────────────┘
         │
         │ porta 8080 esposta all'host
         ▼
    http://localhost:8080
```

---

## 3. Diagramma Dockerfile (Multi-Stage Build)

```
┌──────────────────────────────────────┐
│  Stage 1: build                      │
│  FROM eclipse-temurin:21-jdk         │
│  ├── Copia gradle wrapper            │
│  ├── Scarica dipendenze (cache)      │
│  ├── Copia src/                      │
│  └── ./gradlew vaadinBuildFrontend   │
│         bootJar (production mode)    │
└──────────────────┬───────────────────┘
                   │ copia solo il JAR
                   ▼
┌──────────────────────────────────────┐
│  Stage 2: runtime                    │
│  FROM eclipse-temurin:21-jre         │
│  ├── COPY app.jar                    │
│  ├── EXPOSE 8080                     │
│  └── ENTRYPOINT java -jar app.jar    │
└──────────────────────────────────────┘
```

---

## 4. Diagramma Entità-Relazione (ER)

```
┌────────────┐       ┌───────────────┐       ┌──────────────┐
│   User     │       │    Project    │       │    Task      │
│────────────│       │───────────────│       │──────────────│
│ id (PK)    │1     N│ id (PK)       │1     N│ id (PK)      │
│ username   │◀──────│ user_id (FK)  │◀──────│ project_id   │
│ password   │       │ name          │  0..N │  (FK, null)  │
└────────────┘       │ start_date    │       │ user_id (FK) │
      │1             │ end_date      │       │ name         │
      │              │ status        │       │ date         │
      │N             └───────────────┘       │ start_time   │
      └──────────────────────────────────────│ end_time     │
                                             │ duration     │
                                             │ status       │
                                             └──────────────┘
```

---

## 5. Pipeline CI/CD — GitHub Actions

```
push / PR su main o master
           │
           ▼
┌──────────────────────┐
│  Job: build-and-test  │
│  ├── Checkout         │
│  ├── Setup JDK 21     │
│  ├── ./gradlew test   │  ← profilo dev (H2 in-memory)
│  └── Upload report    │
└──────────┬────────────┘
           │ (solo su push)
           ▼
┌──────────────────────┐
│  Job: build-docker   │
│  ├── Checkout        │
│  ├── Setup Buildx    │
│  └── Build image     │  ← non pubblicata (push: false)
└──────────────────────┘
```

---

## 6. Scelte progettuali

| Scelta | Motivazione |
|---|---|
| **Vaadin** come framework UI | Permette di scrivere il frontend interamente in Java, senza separare in un progetto JS dedicato; garantisce una SPA responsive out-of-the-box con tema Lumo |
| **Pattern MVC server-side** | Separazione netta tra model (entità JPA), view (Vaadin) e controller/service; facilita la testabilità |
| **H2 in-memory per dev/test** | Zero configurazione locale; i test girano in isolamento senza dipendenze esterne |
| **PostgreSQL in produzione** | Database relazionale robusto; gestito via Docker Compose con healthcheck e volume persistente |
| **Spring Security** | Autenticazione form-based integrata con Vaadin; ogni utente vede solo i propri dati |
| **Lombok** | Riduce il boilerplate delle entità (getter, setter, costruttori) |
| **Profili Spring** (`dev` / default) | Configurazione separata per sviluppo e produzione senza modifiche al codice |
| **DataInitializer** | Garantisce la presenza di un utente admin al primo avvio in qualsiasi ambiente |

