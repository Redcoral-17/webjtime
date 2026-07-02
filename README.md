# JTime4Web

Applicazione web cloud-native per il **time tracking** di task e progetti personali.  
Costruita con **Spring Boot 4.0.3 + Vaadin 25.0.5 + PostgreSQL 16**, deployabile con Docker Compose.

---

## Indice

1. [Funzionalità](#1-funzionalità)
2. [Stack tecnologico](#2-stack-tecnologico)
3. [Architettura](#3-architettura)
4. [Diagramma Deploy — Docker Compose](#4-diagramma-deploy--docker-compose)
5. [Dockerfile — Multi-Stage Build](#5-dockerfile--multi-stage-build)
6. [Modello Entità-Relazione (ER)](#6-modello-entità-relazione-er)
7. [Pipeline CI/CD — GitHub Actions](#7-pipeline-cicd--github-actions)
8. [Dipendenze esterne](#8-dipendenze-esterne)
9. [Prerequisiti](#9-prerequisiti)
10. [Build & Run](#10-build--run)
11. [Test](#12-test)
12. [Credenziali di prova](#13-credenziali-di-prova)
13. [Struttura del repository](#14-struttura-del-repository)
14. [Scelte progettuali](#15-scelte-progettuali)

---

## 1. Funzionalità

| Funzione | Descrizione |
|---|---|
| Registrazione / Login | Ogni utente ha il proprio account; esiste un utente admin pre-caricato |
| Gestione Task | Creazione, completamento (con orario effettivo) ed eliminazione di task giornaliere |
| Gestione Progetti | Creazione, completamento ed eliminazione di progetti; ogni task può essere associata a un progetto |
| Visualizzazione per data | Le task sono filtrate per giorno tramite un date picker |
| Statistiche progetto | Data di inizio/fine e durata totale calcolate automaticamente dalle task associate |
| Validazioni | Controllo sovrapposizione oraria tra task, vincoli di stato (es. non si può completare un progetto con task ancora attive) |

---

## 2. Stack tecnologico

| Livello | Tecnologia | Versione |
|---|---|---|
| Frontend / UI | Vaadin (SPA, tema Lumo responsive) | 25.0.5 |
| Backend | Spring Boot, Spring Security 6, Spring Data JPA | 4.0.3 |
| Database (prod) | PostgreSQL | 16 (driver 42.7.11) |
| Database (dev/test) | H2 in-memory | 2.2.224 |
| Build | Gradle con Gradle Wrapper | 9.3.1 |
| Containerizzazione | Docker + Docker Compose | — |
| CI/CD | GitHub Actions | — |
| Java | Temurin | 21 |

---

## 3. Architettura

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
│  │  ├── Calendar.java       →  /tasks                       │   │
│  │  ├── ProjectList.java    →  /projects                    │   │
│  │  ├── MainLayout.java     →  Navbar + Drawer              │   │
│  │  └── DialogBuilder.java  →  Dialog riusabili             │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ chiama                                    │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │  SERVICE  (Business Logic)                               │   │
│  │  ├── UserService    → registrazione, utente corrente     │   │
│  │  ├── ProjectService → CRUD progetti, statistiche         │   │
│  │  └── TaskService    → CRUD task, validazione overlap     │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ usa                                       │
│  ┌──────────────────▼───────────────────────────────────────┐   │
│  │  REPOSITORY  (Spring Data JPA)                           │   │
│  │  ├── UserRepository                                      │   │
│  │  ├── ProjectRepository                                   │   │
│  │  └── TaskRepository                                      │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ JDBC                                      │
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
│                   DATABASE  (PostgreSQL 16)                     │
│                                                                 │
│   Tabelle:                                                      │
│   ├── users (id, username, password)                            │
│   ├── projects (id, name, start_date, end_date, status, user_id)│
│   └── tasks (id, name, date, start_time, end_time, duration,    │
│                 old_duration, status, project_id, user_id)      │
└─────────────────────────────────────────────────────────────────┘
```

### Package MVC

```
it.unicam.cs.awmc.webjtime
├── model/        ← Entità JPA (User, Project, Task, Status)
├── repository/   ← Spring Data JPA repositories
├── service/      ← Business logic (UserService, ProjectService, TaskService)
├── security/     ← Spring Security config, UserDetailsService, DataInitializer
└── view/         ← Vaadin views (Login, Register, Calendar, ProjectList, MainLayout)
```

---

## 4. Diagramma Deploy — Docker Compose

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Network                      │
│                                                                 │
│  ┌─────────────────────────┐    ┌────────────────────────────┐  │
│  │  service: app           │    │  service: db               │  │
│  │  image: build da .      │    │  image: postgres:16-alpine │  │
│  │  porta: 8080:8080       │───▶│  porta: 5432 (interna)     │  │
│  │  env_file: .env         │    │  volume: postgres_data     │  │
│  │  depends_on: db healthy │    │  healthcheck: pg_isready   │  │
│  └─────────────────────────┘    └────────────────────────────┘  │
│                                                                 │
│  volume: postgres_data  (persistenza dati)                      │
└─────────────────────────────────────────────────────────────────┘
         │
         │ porta 8080 esposta all'host
         ▼
    http://localhost:8080
```

---

## 5. Dockerfile — Multi-Stage Build

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

## 6. Modello Entità-Relazione (ER)

```
┌────────────┐       ┌───────────────┐       ┌──────────────────────┐
│   User     │       │    Project    │       │    Task              │
│────────────│       │───────────────│       │──────────────────────│
│ id (PK)    │1     N│ id (PK)       │1     N│ id (PK)              │
│ username   │◀──────│ user_id (FK)  │◀──────│ project_id (FK, null)│
│ password   │       │ name          │  0..N │ user_id (FK)         │
└────────────┘       │ start_date    │       │ name                 │
      │1             │ end_date      │       │ date                 │
      │              │ status        │       │ start_time           │
      │N             └───────────────┘       │ end_time             │
      └──────────────────────────────────────│ duration             │
                                             │ status               │
                                             └──────────────────────┘
```

---

## 7. Pipeline CI/CD — GitHub Actions

```
push / PR su main o master
           │
           ▼
┌──────────────────────┐
│  Job: build-and-test │
│  ├── Checkout        │
│  ├── Setup JDK 21    │
│  ├── ./gradlew test  │  ← profilo dev (H2 in-memory)
│  └── Upload report   │
└──────────┬───────────┘
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

## 8. Dipendenze esterne

| Dipendenza | Scope | Versione | Ruolo |
|---|---|---|---|
| Spring Boot | `implementation` | 4.0.3 | Framework backend principale — IoC, auto-configurazione, `@Transactional`, profili |
| Spring Data JPA | `implementation` | (BOM) | ORM / accesso al DB tramite `JpaRepository`; query derivate dal nome del metodo |
| Spring Security | `implementation` | (BOM) | Autenticazione form-based, `BCryptPasswordEncoder`, `UserDetailsService`, filtri HTTP |
| Vaadin | `implementation` | 25.0.5 | Framework UI server-side; SPA in Java, tema Lumo responsive, routing `@Route` |
| Lombok | `compileOnly` + `annotationProcessor` | (BOM) | `@Getter`, `@Setter`, `@NoArgsConstructor` sulle entità JPA |
| PostgreSQL driver | `runtimeOnly` | 42.7.11 | Driver JDBC per produzione; configurato via variabili d'ambiente |
| H2 | `developmentOnly` + `testImplementation` | 2.2.224 | DB in-memory per sviluppo locale e test (`create-drop`) |
| Spring Boot Test | `testImplementation` | (BOM) | JUnit 5 + Mockito + AssertJ + `@SpringBootTest` |
| JUnit Platform Launcher | `testRuntimeOnly` | (BOM) | Esecuzione test Gradle |
| Vaadin Dev | `developmentOnly` | (BOM) | Live reload in sviluppo |
| `org.jspecify` (`@NonNull`) | transitiva | (BOM) | Null-safety statica su parametri di service e security |

### Dettaglio utilizzo nel codice

**Spring Boot**

| Componente | File |
|---|---|
| `@SpringBootApplication` | `WebjtimeApplication.java` |
| `@Service`, `@Component`, `@Configuration` | Service e `SecurityConfig` |
| `@Transactional` | Tutti i metodi di `TaskService`, `ProjectService`, `UserService` |
| `CommandLineRunner` + `@Value` | `DataInitializer.java` |

**Spring Data JPA**

| File | Repository / Query |
|---|---|
| `UserRepository` | `findByUsername(String)` |
| `ProjectRepository` | `findByUser(User)`, `findByUserAndStatus(User, Status)` |
| `TaskRepository` | `findByUserAndDateOrderByStartTimeAsc`, `findByProject`, `findByUserAndDateAndStatus` |

**Spring Security**

| File | Uso |
|---|---|
| `SecurityConfig.java` | `SecurityFilterChain`, `VaadinSecurityConfigurer`, `BCryptPasswordEncoder` |
| `UserDetailsServiceImpl.java` | Carica utente dal DB → `UserDetails` con `ROLE_USER` |
| `UserService.java` | `SecurityContextHolder` per utente corrente; `PasswordEncoder.encode()` |
| `Calendar.java`, `ProjectList.java` | `@RolesAllowed("USER")` |

**Vaadin**

| File | Componenti usati |
|---|---|
| `Calendar.java` | `Grid<Task>`, `DatePicker`, `ComboBox<LocalTime>`, `TextField`, `Button` |
| `ProjectList.java` | `Grid<Project>`, `TextField`, `Button` |
| `Login.java` | `LoginOverlay` |
| `Register.java` | `TextField`, `PasswordField`, `Button` |
| `MainLayout.java` | `AppLayout`, `Tabs`, `RouterLink`, `Notification` |
| `DialogBuilder.java` | `Dialog` — factory per dialoghi di conferma/input |

---

## 9. Prerequisiti

- **Java 21** (solo per build/run locale senza Docker)
- **Docker** e **Docker Compose** (per avvio completo con PostgreSQL)
- **Git**

---

## 10. Build & Run

### Opzione A — Sviluppo locale (H2 in-memory, no Docker)

```bash
# 1. Clona il repository
git clone https://github.com/Redcoral-17/webjtime.git
cd webjtime

# 2. Avvia in modalità dev (H2, nessuna configurazione necessaria)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

L'app sarà disponibile su [http://localhost:8080](http://localhost:8080).

### Opzione B — Produzione con Docker Compose (PostgreSQL)

```bash
# 1. Clona il repository
git clone https://github.com/Redcoral-17/webjtime.git
cd webjtime

# 2. Crea il file .env a partire dal template
# Linux/macOS:
cp .env.example .env

# Windows PowerShell:
Copy-Item .env.example .env

# 3. Modifica .env con le credenziali desiderate

# 4. Avvia tutti i servizi
docker compose up --build
```

L'app sarà disponibile su [http://localhost:8080](http://localhost:8080).

```bash
# Fermare i servizi
docker compose down

# Fermare e rimuovere anche i dati persistenti
docker compose down -v
```

### Opzione C — Build standalone JAR (senza Docker)

```bash
./gradlew vaadinBuildFrontend bootJar -Pvaadin.productionMode=true
java -jar build/libs/webjtime-0.0.1-SNAPSHOT.jar
```

Richiede le variabili d'ambiente `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`.

---

## 11. Test

```bash
# Esegue tutti i test con profilo dev (H2 in-memory)
./gradlew test

# Report HTML disponibile in:
# build/reports/tests/test/index.html
```

| File di test | Cosa testa |
|---|---|
| `UserServiceTest.java` | Registrazione utente, username duplicato, validazioni |
| `TaskServiceTest.java` | Creazione/completamento/eliminazione task, overlap orario |
| `ProjectServiceTest.java` | Creazione/completamento/eliminazione progetto, vincoli di stato |

Tutti i test usano `@SpringBootTest` (contesto Spring completo) + H2 in-memory con profilo `dev` (`create-drop`).

---

## 12. Credenziali di prova

Dopo l'avvio (con qualsiasi profilo) viene creato automaticamente un utente admin.

**Profilo dev (avvio con `--spring.profiles.active=dev`):**

| Campo | Valore         |
|---|----------------|
| Username | `dev_admin`    |
| Password | `dev_password` |

È anche possibile **registrare liberamente nuovi utenti** dalla pagina `/register`.

---

## 13. Struttura del repository

```
webjtime/
├── .github/workflows/ci.yml     ← Pipeline CI/CD
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── it.unicam.cs.awmc.webjtime/
│   │   │       ├── model/       ← Entità JPA (User, Project, Task, Status)
│   │   │       ├── repository/  ← Spring Data repositories
│   │   │       ├── service/     ← Business logic
│   │   │       ├── security/    ← Autenticazione e autorizzazione
│   │   │       └── view/        ← Vaadin views
│   │   └── resources/
│   │       ├── application.yaml           ← Config produzione (PostgreSQL)
│   │       └── application-dev.yaml       ← Config sviluppo (H2 in-memory)
│   └── test/java/               ← Test JUnit 5
├── Dockerfile                   ← Build multi-stage (JDK 21 → JRE 21)
├── docker-compose.yml           ← Orchestrazione app + PostgreSQL 16
├── .env.example                 ← Template variabili d'ambiente
├── build.gradle                 ← Dipendenze e plugin Gradle
└── README.md                    ← Questo file
```

---

## 14. Scelte progettuali

| Scelta | Motivazione |
|---|---|
| **Vaadin** come framework UI | Permette di scrivere il frontend interamente in Java senza un progetto JS separato; SPA responsive out-of-the-box con tema Lumo |
| **Pattern MVC server-side** | Separazione netta tra model (entità JPA), view (Vaadin) e service; facilita la testabilità |
| **H2 in-memory per dev/test** | Zero configurazione locale; i test girano in isolamento senza dipendenze esterne |
| **PostgreSQL in produzione** | Database relazionale robusto; gestito via Docker Compose con healthcheck e volume persistente |
| **Spring Security + BCrypt** | Autenticazione form-based integrata con Vaadin; BCrypt include salt automatico e fattore di costo configurabile |
| **Lombok** | Riduce il boilerplate delle entità (getter, setter, costruttori no-arg richiesti da JPA) |
| **Profili Spring** (`dev` / default) | Configurazione separata per sviluppo e produzione senza modifiche al codice |
| **DataInitializer** | Garantisce la presenza di un utente admin al primo avvio in qualsiasi ambiente |
| **Docker multi-stage build** | Stage 1 (JDK) compila e produce il JAR; Stage 2 (JRE) lo esegue, riducendo la dimensione dell'immagine finale |
