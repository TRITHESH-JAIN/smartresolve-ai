# SmartResolve AI

AI-powered IT support ticket management built with Java 25, Spring Boot,
Spring Security, PostgreSQL, pgvector, Spring AI, Ollama, and Docker.

## Features

- JWT authentication with employee, support engineer, and admin roles.
- Ticket creation, assignment, comments, audit history, filtering, and pagination.
- Strict ticket status transitions and role-based ticket access.
- Llama 3.2 ticket analysis with human accept/modify/reject review.
- JDBC-backed AI conversations with per-user ownership.
- Verified knowledge articles indexed with pgvector embeddings.
- RAG answers grounded in verified knowledge sources.
- Read-only AI tools for similar tickets, team lookup, and ticket status.
- Role-scoped dashboards and Swagger/OpenAPI documentation.

## Requirements

- JDK 25
- Maven (or the included `mvnw.cmd`)
- Docker Desktop
- Ollama

Install the AI models:

```powershell
ollama pull llama3.2
ollama pull mxbai-embed-large
```

## Configuration

The application reads these environment variables when supplied:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
OLLAMA_BASE_URL
JWT_SECRET
```

Create an untracked `.env` file for Docker Compose:

```text
DB_NAME=smartresolve_db
DB_USERNAME=smartresolve
DB_PASSWORD=replace-with-a-local-password
JWT_SECRET=replace-with-a-long-random-secret
```

Never commit production credentials or JWT secrets.

## Run locally

Start PostgreSQL with pgvector:

```powershell
docker compose up -d postgres
```

Compile:

```powershell
.\mvnw.cmd clean compile
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Application URLs:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health: `http://localhost:8080/actuator/health`

## API groups

| Area | Base endpoints |
|---|---|
| Authentication | `/api/auth/register`, `/api/auth/login` |
| Tickets | `/api/tickets` |
| Ticket search | `/api/tickets/search` |
| AI analysis | `/api/ai/analyze/{ticketId}` |
| AI review | `/api/ai/recommendations/{analysisId}` |
| AI chat | `/api/ai/chat` |
| RAG chat | `/api/ai/rag/chat` |
| Knowledge base | `/api/knowledge/articles` |
| Dashboard | `/api/dashboard` |

Authentication endpoints are public. Other endpoints require a JWT bearer
token and apply role/ownership checks.

## Testing

Run the focused unit tests and the application tests:

```powershell
.\mvnw.cmd test
```

The application context test requires a reachable PostgreSQL database with
credentials matching the active environment configuration.

## Docker deployment

Build the application image:

```powershell
.\mvnw.cmd clean package -DskipTests
docker compose up -d --build
```

Ollama remains on the host and is reached by the application container through
`host.docker.internal`. Keep Ollama private and do not expose it publicly.

Do not run `docker compose down -v` unless you intentionally want to delete the
PostgreSQL data volume.
