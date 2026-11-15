# SmartResolve React frontend

This Vite React application connects to the SmartResolve Spring Boot API.
It supports:

- Employee registration and login
- JWT persistence in browser storage
- Employee ticket creation and ticket listing
- AI ticket analysis and troubleshooting recommendations
- Normal AI chat with conversation memory
- RAG chat using verified knowledge articles
- Admin/support-engineer AI recommendation acceptance
- Admin/support-engineer ticket assignment

## Prerequisites

- Node.js 20 or newer
- npm 10 or newer
- SmartResolve Spring Boot API running on `http://localhost:8080`
- PostgreSQL with the backend schema available
- Ollama running with:

```powershell
ollama pull llama3.2
ollama pull mxbai-embed-large
```

The backend must be running before using the frontend:

```powershell
.\mvnw.cmd spring-boot:run
```

## Install and run

From this directory:

```powershell
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

The Vite development server proxies `/api` requests to
`http://localhost:8080`, so no CORS configuration is needed for local
development.

## Production API URL

For a deployed frontend, create `frontend/.env`:

```text
VITE_API_BASE_URL=https://your-api.example.com/api
```

The deployed Spring Boot API must allow the frontend origin through CORS.

Build and preview:

```powershell
npm run build
npm run preview
```

## Folder structure

```text
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vite.config.ts
├── README.md
└── src/
    ├── api.ts
    ├── App.tsx
    ├── main.tsx
    ├── styles.css
    └── types.ts
```

## Backend endpoints used

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/support/teams
GET  /api/support/engineers
GET  /api/support/teams/{teamId}/engineers
GET  /api/ticket-categories
GET  /api/tickets/search?page=0&size=50
POST /api/tickets
POST /api/ai/analyze/{ticketId}
GET  /api/ai/analyze/{ticketId}
POST /api/ai/recommendations/{analysisId}/accept
POST /api/tickets/{ticketId}/assign
PATCH /api/tickets/{ticketId}/status
POST /api/tickets/{ticketId}/rating
POST /api/tickets/{ticketId}/comments
GET  /api/tickets/{ticketId}/comments
GET  /api/ai/chat/conversations
POST /api/ai/chat
POST /api/ai/rag/chat
GET  /api/knowledge/articles
POST /api/knowledge/articles
```

Admins and support engineers can filter ticket searches by `status` and
`priority`. Admins can also provide `categoryId`:

```text
GET /api/tickets/search?page=0&size=50&status=OPEN&priority=HIGH
GET /api/tickets/search?page=0&size=50&categoryId=CATEGORY_UUID
```

The frontend sends the JWT as:

```text
Authorization: Bearer <token>
```

Employees can rate the assigned support team from a ticket after it reaches
`RESOLVED` or `CLOSED`. Ratings use a 1–5 scale and may include optional
feedback. Ratings are stored in the `support_team_ratings` database table and
can be updated by the employee who submitted them.

## Employee test flow

1. Register a new employee account.
2. Sign in.
3. Create a ticket from **My tickets**.
4. Select the ticket and click **Get AI analysis**.
5. Open **AI assistant** and send multiple messages to test memory.
6. Open **Knowledge chat** and ask a question related to a verified knowledge
   article.
7. Confirm that the RAG response contains a source article.

## Admin review and assignment flow

Sign in with an `ADMIN` or `SUPPORT_ENGINEER` account. The ticket workspace
will show all tickets permitted by the backend. After selecting a ticket:

1. Click **Get AI analysis**.
2. Add optional review remarks.
3. Click **Accept AI recommendation**.
4. Enter the active support engineer UUID.
5. Optionally enter the team UUID if the accepted recommendation did not
   already set the ticket team.
6. Click **Assign to support engineer**.

The assignment request sent by the frontend is:

```json
{
  "assignedEngineerId": "SUPPORT_ENGINEER_UUID",
  "teamId": "SUPPORT_TEAM_UUID",
  "remarks": "Assigned for investigation"
}
```

The backend validates that the engineer is an active support engineer and that
the engineer belongs to the selected team.

Support engineers cannot assign tickets. They can update ticket statuses, add
public comments or internal notes, and view comments on tickets assigned to
them. Employees can view the public conversation on their own tickets and add
public comments for the support engineer. The frontend also lists the
authenticated user's saved AI conversations.

## Registration role and team fields

The registration request now accepts:

```json
{
  "fullName": "Naveen Support",
  "email": "naveen.support@example.com",
  "password": "password123",
  "role": "SUPPORT_ENGINEER",
  "teamId": "5d4bbcb8-8a96-4ee1-9bea-5732a0e03b2d"
}
```

Employees and admins must send `teamId: null`. Support engineers must select
an active team. The login response includes `teamId` and `teamName` when the
user belongs to a team.
