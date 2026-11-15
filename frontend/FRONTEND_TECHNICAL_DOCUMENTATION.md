# SmartResolve AI Frontend Technical Documentation

## 1. Executive summary

SmartResolve AI is a React-based web application for IT support management. The
frontend allows employees to create and track support tickets, use AI
assistance, and search verified internal support knowledge. Administrators and
support engineers receive additional ticket-management and knowledge-management
capabilities.

The frontend is a separate Vite application located in the `frontend/`
directory. It communicates with the Spring Boot backend through JSON REST APIs.
Authentication uses JWT bearer tokens.

## 2. Technology stack

| Area | Technology |
|---|---|
| UI framework | React 19 |
| Language | TypeScript 5.7 |
| Build tool and development server | Vite 6 |
| Styling | Plain CSS in `src/styles.css` |
| Browser communication | Fetch API |
| Authentication | JWT bearer token |
| Backend integration | Spring Boot REST API |
| Database used by backend | PostgreSQL |
| AI services used by backend | Ollama, Spring AI |
| RAG vector search used by backend | PostgreSQL vector store |

The frontend does not call the LLM directly. It calls backend endpoints, and
the backend handles authentication, authorization, database operations, vector
search, and LLM communication.

## 3. Frontend project structure

```text
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── README.md
└── src/
    ├── App.tsx       Main application, screens, forms, and role-based UI
    ├── api.ts        Typed REST API client and error handling
    ├── main.tsx      React application entry point
    ├── styles.css    Global layout and component styling
    └── types.ts      Shared TypeScript data models
```

### Main files

- `App.tsx` contains the authenticated workspace, authentication screen,
  ticket workspace, AI chat, Knowledge chat, and Manage knowledge screen.
- `api.ts` contains the common `fetch` wrapper and all frontend API methods.
- `types.ts` defines the expected request and response data shapes.
- `styles.css` contains the visual design, responsive layout, forms, cards,
  chat interface, and status styles.
- `vite.config.ts` configures the development server and backend proxy.

## 4. Running the application

### Requirements

- Node.js 20 or newer
- npm 10 or newer
- Java and Maven wrapper for the backend
- PostgreSQL configured for the backend
- Ollama running for AI features

### Start the backend

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

### Start the frontend

From the `frontend` directory:

```powershell
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

### Build the frontend

```powershell
npm run build
```

The production files are generated in `frontend/dist`.

To preview the production build locally:

```powershell
npm run preview
```

## 5. Development proxy and API configuration

During local development, Vite proxies requests beginning with `/api` to:

```text
http://localhost:8080
```

This is configured in `vite.config.ts`.

For a deployed frontend, create `frontend/.env`:

```text
VITE_API_BASE_URL=https://your-api.example.com/api
```

The deployed backend must allow the deployed frontend origin through CORS.

## 6. Authentication flow

1. A user registers or signs in from the authentication screen.
2. The frontend calls `POST /api/auth/register` or `POST /api/auth/login`.
3. The backend returns a JWT and user information.
4. The frontend stores the authentication response in browser
   `localStorage` using the key `smartresolve.auth`.
5. Every authenticated API request sends:

```http
Authorization: Bearer <JWT_TOKEN>
```

6. On sign out, the saved authentication data is removed.

If a user enters an incorrect password, the backend returns HTTP 401 with
`Incorrect password`, and the frontend displays that message.

## 7. User roles and permissions

### Employee

Employees can:

- Register and sign in
- Create tickets
- View their permitted tickets
- Request AI ticket analysis
- Use normal AI assistant chat
- Use Knowledge chat
- Add public ticket comments

Employees cannot:

- Add knowledge articles
- Accept AI recommendations
- Assign tickets
- Add internal notes

### Support engineer

Support engineers can:

- View tickets assigned or permitted by the backend
- Request and review AI analysis
- Update ticket status
- Add public comments
- Add internal notes
- Create verified knowledge articles
- Use Knowledge chat

Support engineers do not assign tickets in the current workflow.

### Admin

Admins can:

- Manage tickets permitted by the backend
- Review and accept AI recommendations
- Assign tickets to support engineers
- Add public comments and internal notes
- Create verified knowledge articles
- Use Knowledge chat

The frontend hides actions that are not appropriate for the current role.
The backend remains the final authority and also validates permissions.

## 8. Frontend screens

### Authentication screen

Supports:

- Employee, support engineer, and admin registration
- Login
- Support team selection for support engineer registration
- Display of backend validation and authentication errors

### My support tickets

Employees can create tickets and view their tickets. Ticket details include:

- Title and description
- Priority and status
- Assigned support team
- Creation date
- AI analysis
- Comments

Support engineers and admins receive additional actions according to their
role.

### AI assistant

The AI assistant provides general troubleshooting guidance and maintains
conversation memory through the backend chat service.

### Knowledge chat

Knowledge chat is available to authenticated users, including employees. It
answers questions using verified internal knowledge articles retrieved by the
backend RAG service. Retrieved source article titles are shown under
`Sources used`.

### Manage knowledge

This screen is visible only to admins and support engineers. It allows them to:

- View existing verified knowledge articles
- Add a title
- Add a verified solution
- Optionally associate the article with a resolved or closed ticket
- Submit the article for vector indexing

Employees do not see this screen and cannot create articles through the
frontend. The backend also rejects article creation by unauthorized roles.

## 9. Backend APIs used by the frontend

All paths below include the `/api` prefix. Authenticated endpoints require the
JWT bearer token unless stated otherwise.

### 9.1 Authentication

#### Register

```http
POST /api/auth/register
```

Example request:

```json
{
  "fullName": "Naveen Support",
  "email": "naveen.support@example.com",
  "password": "password123",
  "role": "SUPPORT_ENGINEER",
  "teamId": "SUPPORT_TEAM_UUID"
}
```

For employees and admins, `teamId` should be `null`.

#### Login

```http
POST /api/auth/login
```

Example request:

```json
{
  "email": "employee@example.com",
  "password": "password123"
}
```

Returns the JWT, user ID, name, email, role, and optional team information.

### 9.2 Support directory

```http
GET /api/support/teams
GET /api/support/engineers
GET /api/support/teams/{teamId}/engineers
GET /api/ticket-categories
```

The frontend uses these endpoints for registration team selection, ticket
assignment, support engineer selection, and admin ticket category filtering.
Active support teams, active support engineers, and active ticket categories are
returned.

### 9.3 Tickets

#### Create a ticket

```http
POST /api/tickets
```

Example request:

```json
{
  "title": "Laptop cannot connect to Wi-Fi",
  "description": "The connection disconnects every few minutes.",
  "categoryId": null
}
```

#### Search tickets

```http
GET /api/tickets/search?page=0&size=50
```

The backend applies role-based visibility and returns a paginated ticket
response. Admins and support engineers can add the following optional filters:

```text
status=OPEN|ASSIGNED|RESOLVED
priority=LOW|MEDIUM|HIGH|CRITICAL
```

Admins can also filter by category:

```text
categoryId=CATEGORY_UUID
```

Examples:

```text
GET /api/tickets/search?page=0&size=50&status=ASSIGNED&priority=HIGH
GET /api/tickets/search?page=0&size=50&categoryId=CATEGORY_UUID
```

#### Analyze a ticket with AI

```http
POST /api/ai/analyze/{ticketId}
```

The frontend uses the response to show the summary, suggested category,
priority, team, troubleshooting steps, and suggested response.

#### Load the latest ticket analysis

```http
GET /api/ai/analyze/{ticketId}
```

#### Accept an AI recommendation

```http
POST /api/ai/recommendations/{analysisId}/accept
```

Example request:

```json
{
  "remarks": "Reviewed and approved"
}
```

This action is available in the frontend to admins and authorized support
users according to backend rules.

#### Assign a ticket

```http
POST /api/tickets/{ticketId}/assign
```

Example request:

```json
{
  "assignedEngineerId": "SUPPORT_ENGINEER_UUID",
  "teamId": "SUPPORT_TEAM_UUID",
  "remarks": "Assigned for investigation"
}
```

The backend verifies that the engineer is active and belongs to the selected
team.

#### Update ticket status

```http
PATCH /api/tickets/{ticketId}/status
```

Example request:

```json
{
  "status": "IN_PROGRESS",
  "remarks": "Investigation started"
}
```

#### Rate the support team

```http
POST /api/tickets/{ticketId}/rating
```

Employees can submit a 1–5 rating with optional feedback after their ticket is
`RESOLVED` or `CLOSED`. The rating is stored in the
`support_team_ratings` table and linked to the ticket, employee, and support
team. Submitting the same ticket again updates the employee's existing rating.

Example request:

```json
{
  "rating": 5,
  "feedback": "The support team resolved the issue quickly."
}
```

The backend only allows the ticket's employee to submit the rating, requires an
assigned support team, and rejects ratings before resolution.

#### Add a ticket comment

```http
POST /api/tickets/{ticketId}/comments
```

Example public comment:

```json
{
  "message": "Please try restarting the VPN application.",
  "internalNote": false
}
```

Example internal note:

```json
{
  "message": "VPN client logs show a certificate error.",
  "internalNote": true
}
```

Internal notes are intended for support staff and are not part of the
employee-facing public conversation.

#### Get ticket comments

```http
GET /api/tickets/{ticketId}/comments
```

### 9.4 Normal AI assistant

#### Get saved conversations

```http
GET /api/ai/chat/conversations
```

#### Send a chat message

```http
POST /api/ai/chat
```

Example request:

```json
{
  "conversationId": null,
  "message": "My laptop is running slowly. What should I check?"
}
```

For a continuing conversation, the frontend sends the returned
`conversationId`.

### 9.5 RAG Knowledge chat

#### Send a Knowledge chat question

```http
POST /api/ai/rag/chat
```

Example request:

```json
{
  "conversationId": null,
  "message": "My company VPN is not connecting. What should I do?"
}
```

The response contains:

- `conversationId`
- `answer`
- `sources`
- `timestamp`

Each source contains the article ID, title, and optional source ticket ID.

### 9.6 Knowledge article management

#### Get verified articles

```http
GET /api/knowledge/articles
```

This is used by the Manage knowledge screen. The backend permits active
support engineers and admins to access this management operation.

#### Create and index a verified article

```http
POST /api/knowledge/articles
```

Example request:

```json
{
  "title": "Company VPN troubleshooting",
  "content": "Verify internet access, restart the VPN application, sign in
  again, and restart the computer if the issue continues. Create a support
  ticket with the VPN error message if the issue remains.",
  "sourceTicketId": null
}
```

When an article is created, the backend:

1. Validates the user role.
2. Saves the article as verified and active.
3. Creates vector metadata.
4. Adds the article to the vector store.
5. Makes it available to Knowledge chat retrieval.

If `sourceTicketId` is supplied, the ticket must be resolved or closed and
must contain resolution notes.

## 10. RAG processing flow

Knowledge chat uses Retrieval-Augmented Generation:

```text
Frontend question
        |
        v
POST /api/ai/rag/chat
        |
        v
Vector similarity search for verified and active articles
        |
        v
Top matching articles are added as LLM context
        |
        v
Spring AI calls the configured LLM
        |
        v
Answer and source articles return to the frontend
```

The current backend retrieves up to five results and uses a similarity
threshold of `0.60`. If no matching article exists, the model is instructed to
say that no verified solution was found and to clearly distinguish general
advice from verified company knowledge.

The frontend does not add questions to the vector database. Admins and support
engineers add verified solution articles through Manage knowledge.

## 11. API client error handling

The frontend uses one shared request helper in `src/api.ts`. It:

- Adds `Content-Type: application/json` for requests with bodies.
- Adds the JWT authorization header when a token is available.
- Parses JSON responses.
- Reads backend `{ "message": "..." }` error responses.
- Throws a typed `ApiError` containing the message and HTTP status.

Screens display these messages in an error banner. This is why a failed login
now displays `Incorrect password` instead of a generic unexpected server
error.

## 12. Typical end-to-end workflows

### Employee workflow

1. Register as an employee.
2. Sign in.
3. Create a ticket.
4. Select the ticket and request AI analysis.
5. Add public comments if needed.
6. Ask general questions in AI assistant.
7. Ask knowledge-based questions in Knowledge chat.
8. Review the displayed source articles.

### Admin/support knowledge workflow

1. Sign in as an admin or support engineer.
2. Open **Manage knowledge**.
3. Click **Add article**.
4. Enter a meaningful article title.
5. Enter the verified solution steps.
6. Optionally enter a resolved or closed ticket UUID.
7. Click **Add and index article**.
8. Open Knowledge chat and test a question matching the article.
9. Confirm the article appears under **Sources used**.

## 13. Security considerations

- JWTs are sent only through the backend API authorization header.
- Role checks are enforced by the backend; hiding frontend controls is only a
  user experience measure.
- Employees cannot create verified knowledge articles through the backend
  service.
- Internal notes are marked separately from public comments.
- The RAG system is instructed not to request passwords, OTPs, access tokens,
  or secret keys.
- The frontend should be served over HTTPS in production.
- Production API URLs should be configured through environment variables rather
  than hard-coded into source code.

## 14. Validation and maintenance

Run the frontend build after frontend changes:

```powershell
cd frontend
npm run build
```

Run the backend tests from the repository root:

```powershell
.\mvnw.cmd test
```

When adding a new backend endpoint:

1. Add a typed method in `frontend/src/api.ts`.
2. Add or update response types in `frontend/src/types.ts`.
3. Add the UI action in the appropriate component.
4. Handle `ApiError` and display a useful message.
5. Run the frontend build.
6. Run targeted backend tests and the full test suite when appropriate.

## 15. Current limitations and future improvements

- The frontend is a single-page React application without a client-side
  routing library.
- Knowledge article management currently supports creating and listing
  articles; editing and deactivating articles are not exposed in the
  frontend.
- Ticket search currently loads the first 50 results for the main workspace.
- The frontend uses browser local storage for the JWT. A production deployment
  may consider a stronger session strategy depending on the security
  requirements.
- Additional automated frontend tests could be added for role visibility,
  article creation, and API error rendering.

