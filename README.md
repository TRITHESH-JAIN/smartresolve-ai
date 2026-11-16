# SmartResolve AI

> **AI-powered IT support, from incident report to verified resolution.**

SmartResolve AI is a full-stack IT support platform that combines secure ticket
management, human-reviewed AI triage, role-based collaboration, persistent AI
chat, and retrieval-augmented generation (RAG). It helps employees report
problems, helps administrators route work, and helps support engineers resolve
incidents faster while turning successful solutions into reusable knowledge.

## Why SmartResolve AI?

Traditional support workflows scatter incident details across email, chat, and
documents. This causes slow triage, repeated troubleshooting, and poor
visibility into team workload. SmartResolve AI brings the complete workflow
into one platform:

```text
Employee reports issue
        ↓
AI summarizes and recommends priority/team
        ↓
Administrator reviews and assigns
        ↓
Support engineer investigates and communicates
        ↓
Resolution is recorded and reusable knowledge is created
```

AI assists the team; authorized people remain in control of prioritization,
assignment, status changes, and closure.

## Functional overview

### Employee experience

- Register and sign in securely.
- Create a ticket with a title, description, and category.
- Receive instant AI analysis and troubleshooting guidance.
- View ticket status, priority, assignment, comments, and history.
- Communicate with administrators and support engineers through ticket
  comments.
- Continue conversations with an AI assistant using saved chat history.
- Ask questions against verified support knowledge with RAG chat.
- Add remarks and rate the support team after resolution.

### Administrator experience

- View a role-scoped support queue.
- Filter and sort tickets by status, priority, category, keyword, and age.
- Review AI summaries and recommendations.
- Accept, modify, or reject AI recommendations.
- Assign tickets to eligible support engineers.
- Add public comments and internal notes.
- Monitor role-scoped dashboards and operational counts.
- Publish verified knowledge articles from resolved incidents.

### Support engineer experience

Support engineers are organized into three specialized teams:

| Team | Typical responsibility |
|---|---|
| Network Support | Connectivity, VPN, DNS, routing, and network access |
| Software Support | Applications, clients, configuration, and software failures |
| Database Support | Database access, queries, schemas, and data issues |

Engineers can:

- View tickets assigned to them.
- Read employee remarks, comments, history, and AI analysis.
- Communicate with employees and administrators.
- Add public replies and internal engineering notes.
- Move tickets through the investigation lifecycle.
- Record resolution notes and close completed work.
- Add verified solutions to the organization knowledge base.
- Use AI assistant and RAG chat during investigation.

## Core capabilities

| Capability | What it provides |
|---|---|
| Secure access | JWT authentication and role/team authorization |
| Ticket lifecycle | Creation, filtering, assignment, comments, history, ratings, and resolution |
| AI triage | Summary, suggested category, priority, team, steps, and response |
| Human review | Admin accept, modify, or reject workflow for AI recommendations |
| AI assistant | Persistent user-owned conversations with JDBC chat memory |
| RAG knowledge | Answers grounded in verified and active support articles |
| Dashboards | Role-scoped ticket counts by status and priority |
| Auditability | Ticket history, recommendation review data, and internal notes |
| API documentation | Swagger UI and OpenAPI JSON |

## Ticket lifecycle

The backend validates status transitions and access rules:

```text
NEW → AI_ANALYSIS → OPEN → ASSIGNED → IN_PROGRESS
                                      ↓
                                  ON_HOLD
                                      ↓
                                  RESOLVED → CLOSED
                                      ↑
                                  REOPENED
```

Available statuses are `NEW`, `AI_ANALYSIS`, `OPEN`, `ASSIGNED`,
`IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, and `REOPENED`.

## AI and RAG design

### Ticket analysis

Spring AI sends ticket context to the configured Ollama chat model. The
structured result contains:

- Problem summary
- Suggested category
- Suggested priority
- Suggested support team
- Troubleshooting steps
- Suggested response

The result is stored and reviewed by an administrator before it becomes an
operational decision.

### Assistant chat

The assistant supports follow-up troubleshooting and response drafting.
Conversation history is persisted through Spring AI JDBC chat memory and is
isolated by authenticated user.

### RAG chat

Administrators and support engineers can publish reusable solutions as
knowledge articles. Only verified and active articles are indexed for RAG.
Spring AI retrieves relevant pgvector content and uses it to ground responses
with internal support knowledge.

## Technical architecture

```text
┌──────────────────────────────┐
│ React 19 + TypeScript + Vite │
│ Authentication, tickets, UI  │
└──────────────┬───────────────┘
               │ REST / JSON / JWT
┌──────────────▼───────────────┐
│ Spring Boot REST API         │
│ Security, validation, rules  │
│ Tickets, AI, chat, knowledge │
└───────┬──────────┬───────────┘
        │          │
┌───────▼──────┐ ┌─▼────────────────┐
│ PostgreSQL   │ │ Spring AI +      │
│ JPA business │ │ Ollama models    │
│ data         │ │ chat + embeddings│
└───────┬──────┘ └─┬────────────────┘
        │          │
        └──────┬───▼────────────────┐
               │ pgvector RAG store │
               └────────────────────┘
```

### Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript 5.7, Vite 6, Fetch API |
| Backend | Java 25, Spring Boot 3.5 |
| API | Spring Web, REST, Springdoc OpenAPI |
| Persistence | Spring Data JPA and PostgreSQL |
| Security | Spring Security, JWT, password hashing |
| AI orchestration | Spring AI 1.1.8 |
| Chat model | Ollama with `llama3.2` |
| Embedding model | Ollama with `mxbai-embed-large` |
| Vector search | PostgreSQL pgvector |
| Chat memory | Spring AI JDBC repository |
| Operations | Spring Boot Actuator and Docker Compose |

### Main backend modules

```text
src/main/java/com/smartresolve/
├── ai/             AI tools for controlled model access
├── config/         Security, chat memory, OpenAPI, and seed data
├── controller/     REST endpoints
├── dto/            Request and response contracts
├── entity/         JPA domain model
├── enums/          Roles, priorities, statuses, and review states
├── repository/     Database access and ticket specifications
├── security/       JWT authentication
└── service/        Business workflows and AI orchestration
```

The frontend is in [`frontend/`](frontend/):

```text
frontend/src/
├── App.tsx         Main role-aware application workspace
├── api.ts          Typed REST API client
├── types.ts        Shared TypeScript models
├── main.tsx        React entry point
└── styles.css      Application styling
```

## Quick start

### Prerequisites

- JDK 25
- Docker Desktop
- Node.js 20+
- npm 10+
- Ollama
- Git

### 1. Clone and configure

```powershell
git clone https://github.com/TRITHESH-JAIN/smartresolve-ai.git
Set-Location smartresolve-ai
Copy-Item .env.example .env
```

Update `.env` with local values:

```text
DB_NAME=smartresolve_db
DB_USERNAME=smartresolve
DB_PASSWORD=change-me
JWT_SECRET=your-base64-encoded-32-byte-secret
```

Generate a suitable JWT secret in PowerShell:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object {
  [byte](Get-Random -Minimum 0 -Maximum 256)
}))
```

Never commit `.env`, credentials, or production secrets.

### 2. Install AI models

Start Ollama, then run:

```powershell
ollama pull llama3.2
ollama pull mxbai-embed-large
```

### 3. Start PostgreSQL with pgvector

```powershell
docker compose up -d postgres
```

### 4. Start the Spring Boot backend

For a local Maven run, set the values required by the Java process in the same
PowerShell session:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/smartresolve_db"
$env:DB_USERNAME = "smartresolve"
$env:DB_PASSWORD = "change-me"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
$env:JWT_SECRET = "your-base64-encoded-32-byte-secret"

.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`.

### 5. Start the React frontend

Open another PowerShell window:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to the Spring Boot
backend during local development.

## Application URLs

| Resource | URL |
|---|---|
| React application | `http://localhost:5173` |
| Spring Boot API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |
| Health check | `http://localhost:8080/actuator/health` |

## API overview

| Area | Base endpoints |
|---|---|
| Authentication | `/api/auth/register`, `/api/auth/login` |
| Support directory | `/api/support/teams`, `/api/support/engineers` |
| Categories | `/api/ticket-categories` |
| Tickets | `/api/tickets`, `/api/tickets/search` |
| AI analysis | `/api/ai/analyze/{ticketId}` |
| AI review | `/api/ai/recommendations/{analysisId}` |
| AI assistant | `/api/ai/chat` |
| RAG chat | `/api/ai/rag/chat` |
| Knowledge base | `/api/knowledge/articles` |
| Dashboard | `/api/dashboard` |

For complete request, response, and database mappings, see
[API_DATABASE_SPECIFICATION.md](API_DATABASE_SPECIFICATION.md) and
[API_WORK_DOCUMENT.md](API_WORK_DOCUMENT.md).

## Default support teams

The application seeds these active teams at startup:

| Team | ID |
|---|---|
| Database Support | `55e9ad25-b8b6-45f5-a622-9ab381f1f823` |
| Network Support | `5d4bbcb8-8a96-4ee1-9bea-5732a0e03b2d` |
| Software Support | `be1b57ce-f8ae-493f-a516-90e8e7c1bbe4` |

Support engineer registration requires a `teamId`. Employee and administrator
registration must use `teamId: null`. The role field is available for local
development; production deployments should use administrator-controlled
provisioning.

## Testing and verification

Run the backend test suite:

```powershell
.\mvnw.cmd test
```

The application context test requires a reachable PostgreSQL instance with
matching credentials.

Build the frontend:

```powershell
Set-Location frontend
npm.cmd run build
```

The production frontend output is generated in `frontend/dist`.

## Docker deployment

Configure `.env`, make sure Ollama is running on the host, then build and start
the application and database:

```powershell
.\mvnw.cmd clean package -DskipTests
docker compose up -d --build
```

The application container reaches host Ollama through
`host.docker.internal:11434`. Keep Ollama private and do not expose it
publicly.

To stop containers without deleting the database volume:

```powershell
docker compose down
```

Do not run `docker compose down -v` unless you intentionally want to delete
the PostgreSQL data volume.

## Documentation

- [Functional requirements](FUNCTIONAL_REQUIREMENTS.md)
- [API and database specification](API_DATABASE_SPECIFICATION.md)
- [Full case study](CASE_STUDY.md)
- [Frontend documentation](frontend/FRONTEND_TECHNICAL_DOCUMENTATION.md)
- [Frontend README](frontend/README.md)

## Security notes

- Store secrets only in environment variables or a secret manager.
- Use a strong Base64 JWT secret containing at least 32 bytes.
- Do not expose Ollama or PostgreSQL directly to the public internet.
- Keep administrator registration restricted in production.
- Treat AI output as assistance, not as an authoritative operational action.

## License

This project is intended for educational and demonstration use. Add the
appropriate project license before distributing it publicly.
