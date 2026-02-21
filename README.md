# JTime4Web

Applicazione web per il **tracciamento del tempo** su task e progetti, sviluppata con **Spring Boot** e **Vaadin Flow**.

---

## Indice

- [Funzionalità](#funzionalità)
- [Architettura](#architettura)
- [Stack tecnologico](#stack-tecnologico)
- [Scelte progettuali](#scelte-progettuali)
- [Sicurezza](#sicurezza)
- [Prerequisiti](#prerequisiti)
- [Build & Run](#build--run)
  - [Modalità sviluppo (locale)](#modalità-sviluppo-locale)
  - [Modalità produzione con Docker Compose](#modalità-produzione-con-docker-compose)
  - [Solo build JAR](#solo-build-jar)
- [Test](#test)
- [Configurazione](#configurazione)
- [Credenziali di prova](#credenziali-di-prova)

---

## Funzionalità

### 📅 Calendar
- Visualizzazione delle task filtrate per data tramite un selettore calendario
- **Aggiunta task**: nome, progetto (opzionale), data, orario inizio/fine con slot da 15 minuti
- Validazione di sovrapposizioni temporali tra task nella stessa giornata
- **Completamento task**: possibilità di correggere gli orari effettivi a consuntivo, con tracciamento della differenza rispetto alla stima iniziale (`oldDuration`)
- **Eliminazione task**

### 📁 Projects
- Visualizzazione di tutti i progetti con data di inizio/fine calcolata automaticamente dalle task associate
- Calcolo della durata totale del progetto (somma delle durate delle task)
- **Creazione progetto** con nome
- **Completamento progetto**: abilitato solo se tutte le task associate sono completate
- **Eliminazione progetto** (con rimozione della FK sulle task associate)

### 📊 Reports
- Creazione di report con intervallo di date e progetto opzionale
- Visualizzazione delle task filtrate per intervallo di date e progetto del report selezionato
- Pannello informativo: progetto, data inizio/fine, conteggio task attive/completate
- **Eliminazione report**

### 🔐 Autenticazione
- Accesso protetto tramite **Spring Security** integrato con Vaadin Flow
- Pagina di login dedicata (`/login`) con `LoginForm` di Vaadin, accessibile senza autenticazione
- Tutte le altre view richiedono autenticazione (`@PermitAll` + `VaadinSecurityConfigurer`)
- **Logout** disponibile direttamente nella navbar del layout principale
- Utente amministratore creato automaticamente all'avvio tramite `DataInitializer` (se non già presente nel DB)
- Password cifrata con **BCrypt**
- Credenziali configurabili tramite variabili d'ambiente (`ADMIN_USERNAME`, `ADMIN_PASSWORD`)

---

## Architettura

```
┌──────────────────────────────────────────────┐
│                  Browser                     │
│          (Vaadin Flow / WebSocket)           │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│              Spring Boot App                 │
│  ┌──────────────────────────────────────┐   │
│  │       Security Layer (Spring)        │   │
│  │  SecurityConfig │ UserDetailsService │   │
│  │  Login View     │ DataInitializer    │   │
│  ├──────────────────────────────────────┤   │
│  │           View Layer (Vaadin)        │   │
│  │  Calendar │ ProjectList │ ReportList │   │
│  │           └── MainLayout ────────────│   │
│  ├──────────────────────────────────────┤   │
│  │         Repository Layer (JPA)       │   │
│  │  TaskRepository │ ProjectRepository  │   │
│  │  ReportRepository │ UserRepository   │   │
│  ├──────────────────────────────────────┤   │
│  │           Model Layer (JPA Entities) │   │
│  │   Task │ Project │ Report │ Status   │   │
│  │   User                               │   │
│  └──────────────────────────────────────┘   │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────▼────────────────────────┐
│         Database (PostgreSQL / H2)           │
└──────────────────────────────────────────────┘
```

### Deploy con Docker Compose

```
┌─────────────────────────────────────────┐
│           Docker Network                │
│                                         │
│  ┌─────────────────┐  ┌──────────────┐  │
│  │   app (8080)    │──│  db (5432)   │  │
│  │  Spring Boot +  │  │  PostgreSQL  │  │
│  │  Vaadin Flow    │  │  16-alpine   │  │
│  └─────────────────┘  └──────┬───────┘  │
│                              │          │
│                     postgres_data (vol) │
└─────────────────────────────────────────┘
         │
  localhost:8080
```

---

## Stack tecnologico

| Componente       | Tecnologia                        | Versione  |
|------------------|-----------------------------------|-----------|
| Framework web    | Spring Boot                       | 4.0.3     |
| UI Framework     | Vaadin Flow                       | 25.0.5    |
| Sicurezza        | Spring Security (+ Vaadin integration) | —    |
| Persistenza      | Spring Data JPA / Hibernate       | —         |
| Database (prod)  | PostgreSQL                        | 16        |
| Database (dev)   | H2 (in-memory)                    | 2.2.224   |
| Build tool       | Gradle                            | Wrapper   |
| Java             | JDK                               | 21        |
| Riduzione boilerplate | Lombok                       | —         |
| Containerizzazione | Docker / Docker Compose         | —         |

---

## Scelte progettuali

- **Vaadin Flow** è stato scelto come framework UI per permettere lo sviluppo full-stack in Java puro, evitando la necessità di scrivere JavaScript/TypeScript separato. La comunicazione avviene via WebSocket in modo trasparente.
- **Profilo `dev`** usa un database H2 in-memory per agevolare lo sviluppo e i test senza dipendenze esterne.
- **Profilo `prod`** (default) usa PostgreSQL, configurato tramite variabili d'ambiente (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`).
- **Lombok** riduce il boilerplate per getter/setter/costruttori nelle entità JPA.
- **`ddl-auto: update`** gestisce automaticamente le migrazioni dello schema del database a partire dalle entità JPA, adatto a un contesto accademico/prototipale.
- Le task mantengono `oldDuration` per confrontare la durata stimata con quella effettiva a consuntivo, utile nella vista Report.
- Il `Dockerfile` usa un build multi-stage: il primo stage (`eclipse-temurin:21-jdk`) compila e builda il frontend Vaadin, il secondo stage (`eclipse-temurin:21-jre`) contiene solo il JAR finale, mantenendo l'immagine leggera.
- **Spring Security** è integrato tramite `VaadinSecurityConfigurer` per proteggere tutte le route Vaadin. Il `DataInitializer` crea automaticamente l'utente admin all'avvio se non esiste, cifrando la password con BCrypt. Le credenziali sono iniettate tramite le proprietà `app.admin.username` e `app.admin.password`, configurabili via variabili d'ambiente.

---

## Sicurezza

Il flusso di autenticazione funziona come segue:

1. L'utente non autenticato viene reindirizzato a `/login`
2. Dopo il login corretto, viene reindirizzato alla pagina richiesta originariamente
3. Il pulsante **Logout** nella navbar invalida la sessione e reindirizza a `/login`

### Struttura dei file di sicurezza

| File | Responsabilità |
|------|----------------|
| `SecurityConfig.java` | Configura `SecurityFilterChain` con Vaadin integration e `BCryptPasswordEncoder` |
| `UserDetailsServiceImpl.java` | Implementa `UserDetailsService` per caricare l'utente dal DB |
| `DataInitializer.java` | Crea l'utente admin all'avvio se non presente (legge da `app.admin.*`) |
| `User.java` | Entità JPA per gli utenti (tabella `users`) |
| `UserRepository.java` | Repository JPA con `findByUsername` |
| `Login.java` | View Vaadin per il form di login (`@AnonymousAllowed`) |

---

## Prerequisiti

- **JDK 21** (es. [Eclipse Temurin](https://adoptium.net/))
- **Docker** e **Docker Compose** (per la modalità containerizzata)
- Connessione internet per il download delle dipendenze Gradle al primo build

---

## Build & Run

### Modalità sviluppo (locale)

Avvia l'applicazione con profilo `dev` (H2 in-memory, nessun database esterno necessario):

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Su Windows (PowerShell):

```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

L'applicazione sarà disponibile su: **http://localhost:8080**

> In modalità dev è disponibile anche la **H2 Console** su: http://localhost:8080/h2-console  
> JDBC URL: `jdbc:h2:mem:jtimedev` · User: `sa` · Password: *(vuota)*

---

### Modalità produzione con Docker Compose

Avvia l'intera stack (app + PostgreSQL) con un solo comando:

```bash
docker compose up --build
```

L'applicazione sarà disponibile su: **http://localhost:8080**

Per fermare e rimuovere i container:

```bash
docker compose down
```

Per rimuovere anche il volume del database:

```bash
docker compose down -v
```

---

### Solo build JAR

Per produrre il JAR di produzione (include il build del frontend Vaadin):

```bash
./gradlew vaadinBuildFrontend bootJar -Pvaadin.productionMode=true
```

Il JAR verrà generato in `build/libs/webjtime-0.0.1-SNAPSHOT.jar`.

Per eseguirlo manualmente (richiede un PostgreSQL raggiungibile):

```bash
java -jar build/libs/webjtime-0.0.1-SNAPSHOT.jar \
  --DATABASE_URL=jdbc:postgresql://localhost:5432/webjtime \
  --DATABASE_USERNAME=webjtime \
  --DATABASE_PASSWORD=webjtime
```

---

## Test

Esegui i test con:

```bash
./gradlew test
```

I test usano il profilo `dev` con database H2 in-memory (nessuna dipendenza esterna). I report vengono generati in `build/reports/tests/test/index.html`.

---

## Configurazione

| Variabile d'ambiente  | Descrizione                            | Valore default (Docker Compose) |
|-----------------------|----------------------------------------|---------------------------------|
| `DATABASE_URL`        | JDBC URL del database PostgreSQL       | `jdbc:postgresql://db:5432/webjtime` |
| `DATABASE_USERNAME`   | Username del database                  | `webjtime`                      |
| `DATABASE_PASSWORD`   | Password del database                  | `webjtime`                      |
| `ADMIN_USERNAME`      | Username dell'utente admin applicazione | `red_coral`                    |
| `ADMIN_PASSWORD`      | Password dell'utente admin applicazione | *(vedere docker-compose.yml)*  |

I file di configurazione si trovano in `src/main/resources/`:
- `application.yaml` — configurazione base (produzione)
- `application-dev.yaml` — override per il profilo `dev` (H2 in-memory)

---

## Credenziali di prova

### Accesso all'applicazione

| Campo    | Valore        |
|----------|---------------|
| Username | `red_coral`   |
| Password | `Fico@#6013`  |

> L'utente viene creato automaticamente all'avvio dal `DataInitializer` se non già presente nel database.

### Database PostgreSQL (Docker Compose)

| Campo    | Valore      |
|----------|-------------|
| Database | `webjtime`  |
| Username | `webjtime`  |
| Password | `webjtime`  |
| Host     | `localhost` |
| Porta    | `5432`      |

### H2 Console (profilo dev)

| Campo      | Valore                   |
|------------|--------------------------|
| JDBC URL   | `jdbc:h2:mem:jtimedev`   |
| Username   | `sa`                     |
| Password   | *(vuota)*                |

---

## Autore

Filippo Corallini (matricola 125587), filippo.corallini@studenti.unicam.it  
Università di Camerino — Corso di Applicazioni Web, Mobile e Cloud

