# JTime4Web

Applicazione web cloud-native per il **time tracking** di task e progetti personali.  
Costruita con **Spring Boot 4 + Vaadin 25 + PostgreSQL**, deployabile con Docker Compose.

---

## Funzionalità

| Funzione | Descrizione |
|---|---|
| Registrazione / Login | Ogni utente ha il proprio account; esiste un utente admin pre-caricato |
| Gestione Task | Creazione, completamento (con orario effettivo) ed eliminazione di task giornaliere |
| Gestione Progetti | Creazione, completamento ed eliminazione di progetti; ogni task può essere associata a un progetto |
| Visualizzazione per data | Le task sono filtrate per giorno tramite un date picker |
| Statistiche progetto | Data di inizio/fine e durata totale calcolate automaticamente dalle task associate |
| Validazioni | Controllo sovrapposizione oraria tra task, vincoli di stato (es. non si può completare un progetto con task ancora attive) |

---

## Architettura

Vedere [`docs/architecture.md`](docs/architecture.md) per i diagrammi completi.

```
Browser (Vaadin SPA)
        │  HTTP / WebSocket
        ▼
┌─────────────────────────────────┐
│  Spring Boot 4  (porta 8080)    │
│  ├── View layer  (Vaadin)       │
│  ├── Service layer              │
│  ├── Repository (Spring Data)   │
│  └── Security (Spring Security) │
└────────────┬────────────────────┘
             │ JDBC
             ▼
     PostgreSQL 16 (porta 5432)
```

### Package MVC

```
it.unicam.cs.awmc.webjtime
├── model/        ← Entità JPA  (User, Project, Task, Status)
├── repository/   ← Spring Data JPA repositories
├── service/      ← Business logic (UserService, ProjectService, TaskService)
├── security/     ← Spring Security config, UserDetailsService, DataInitializer
└── view/         ← Vaadin views  (Login, Register, Calendar, ProjectList, MainLayout)
```

---

## Stack tecnologico

| Livello | Tecnologia |
|---|---|
| Frontend / UI | Vaadin 25 (SPA, tema Lumo responsive) |
| Backend | Spring Boot 4, Spring Security 6, Spring Data JPA |
| Database (prod) | PostgreSQL 16 |
| Database (dev) | H2 in-memory |
| Build | Gradle 8 con Gradle Wrapper |
| Containerizzazione | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Java | 21 (Temurin) |

---

## Prerequisiti

- **Java 21** (solo per build/run locale senza Docker)
- **Docker** e **Docker Compose** (per avvio completo con PostgreSQL)
- **Git**

---

## Build & Run

### Opzione A — Sviluppo locale (H2 in-memory, no Docker)

```bash
# 1. Clona il repository
git clone <url-repo>
cd webjtime

# 2. Avvia in modalità dev (H2, nessuna configurazione necessaria)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

L'app sarà disponibile su [http://localhost:8080](http://localhost:8080).

### Opzione B — Produzione con Docker Compose (PostgreSQL)

```bash
# 1. Clona il repository
git clone <url-repo>
cd webjtime

# 2. Crea il file .env a partire dal template
cp .env.example .env
# Modifica .env con le credenziali desiderate

# 3. Avvia tutti i servizi
docker compose up --build
```

L'app sarà disponibile su [http://localhost:8080](http://localhost:8080).

Per fermare:
```bash
docker compose down
# Per eliminare anche i dati persistenti:
docker compose down -v
```

### Build standalone JAR (senza Docker)

```bash
./gradlew vaadinBuildFrontend bootJar -Pvaadin.productionMode=true
java -jar build/libs/webjtime-0.0.1-SNAPSHOT.jar
```
Richiede le variabili d'ambiente `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`.

---

## Variabili d'ambiente

Copiare `.env.example` in `.env` e personalizzare i valori.

| Variabile | Descrizione | Esempio |
|---|---|---|
| `ADMIN_USERNAME` | Username dell'utente admin pre-caricato | `admin` |
| `ADMIN_PASSWORD` | Password dell'utente admin | `changeme` |
| `DATABASE_URL` | JDBC URL del database PostgreSQL | `jdbc:postgresql://db:5432/webjtime` |
| `DATABASE_USERNAME` | Utente PostgreSQL | `webjtime` |
| `DATABASE_PASSWORD` | Password PostgreSQL | `changeme` |
| `POSTGRES_PASSWORD` | Password usata da Docker Compose per creare il DB | `changeme` |

> ⚠️ Il file `.env` è escluso dal repository via `.gitignore`. Non committare credenziali reali.

---

## Test

```bash
# Esegue tutti i test con profilo dev (H2 in-memory)
./gradlew test

# Report HTML disponibile in:
# build/reports/tests/test/index.html
```

I test coprono la business logic dei service (`ProjectService`, `TaskService`, `UserService`) usando H2 in-memory.

---

## Credenziali di prova

Dopo l'avvio (con qualsiasi profilo) viene creato automaticamente un utente admin con le credenziali configurate in `.env` / `application-dev.yaml`.

**Profilo dev (default locale):**

| Campo | Valore |
|---|---|
| Username | `dev_admin` |
| Password | `changeme_dev` |

È anche possibile **registrare liberamente nuovi utenti** dalla pagina `/register`.

---

## CI/CD

La pipeline GitHub Actions (`.github/workflows/ci.yml`) si attiva ad ogni push/PR su `main`/`master` ed esegue:

1. **Build & Test** — `./gradlew test` con profilo `dev` (H2)
2. **Build Docker image** — build multi-stage dell'immagine (solo su push, non pubblicata)

---

## Struttura del repository

```
webjtime/
├── .github/workflows/ci.yml     ← Pipeline CI/CD
├── src/
│   ├── main/
│   │   ├── java/                ← Codice sorgente (MVC)
│   │   └── resources/
│   │       ├── application.yaml           ← Config produzione
│   │       └── application-dev.yaml       ← Config sviluppo (H2)
│   └── test/java/               ← Test unitari
├── docs/
│   └── architecture.md          ← Diagrammi architettura/deploy
├── Dockerfile                   ← Build multi-stage
├── docker-compose.yml           ← Orchestrazione app + PostgreSQL
├── .env.example                 ← Template variabili d'ambiente
├── build.gradle                 ← Dipendenze e plugin
└── README.md                    ← Questo file
```

