# SmartResolve AI
## Frontend-to-Backend API Work Document

## 1. Application overview

SmartResolve AI is an IT support ticket management system. The React frontend
communicates with the Spring Boot backend through REST APIs. The backend
handles authentication, authorization, ticket management, AI analysis, RAG
knowledge search, comments, assignment, and support-team ratings.

All authenticated requests include:

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

The local frontend runs at `http://localhost:5173` and the backend runs at
`http://localhost:8080`.

## 2. Employee flow

### 2.1 Employee registration

```http
POST /api/auth/register
```

Purpose: Creates an employee account.

Request:

```json
{
  "fullName": "Pritam Patel",
  "email": "pritam@example.com",
  "password": "password123",
  "role": "EMPLOYEE",
  "teamId": null
}
```

Response:

```json
{
  "token": "JWT_TOKEN",
  "tokenType": "Bearer",
  "userId": "USER_UUID",
  "fullName": "Pritam Patel",
  "email": "pritam@example.com",
  "role": "EMPLOYEE",
  "teamId": null,
  "teamName": null
}
```

Tables used:

- `users`

### 2.2 Employee login

```http
POST /api/auth/login
```

Purpose: Authenticates the employee and returns a JWT token.

Request:

```json
{
  "email": "pritam@example.com",
  "password": "password123"
}
```

Response: Authentication response containing the JWT and employee details.

Tables used:

- `users`

Incorrect passwords return:

```text
Incorrect password
```

### 2.3 Load employee tickets

```http
GET /api/tickets/search?page=0&size=50
```

Purpose: Returns tickets created by the logged-in employee.

Response:

```json
{
  "content": [
    {
      "id": "TICKET_UUID",
      "title": "VPN is not connecting",
      "description": "The company VPN fails to connect.",
      "categoryName": "Network",
      "priority": "MEDIUM",
      "status": "NEW",
      "employeeId": "EMPLOYEE_UUID",
      "employeeName": "Pritam Patel",
      "assignedEngineerId": null,
      "assignedEngineerName": null,
      "teamId": "TEAM_UUID",
      "teamName": "Network Support",
      "resolutionNotes": null,
      "supportTeamRating": null,
      "supportTeamRatingFeedback": null
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Tables used:

- `tickets`
- `users`
- `ticket_categories`
- `support_teams`
- `support_team_ratings`

### 2.4 Load ticket categories

```http
GET /api/ticket-categories
```

Purpose: Returns active categories that may be used while creating tickets.

Response:

```json
[
  {
    "id": "CATEGORY_UUID",
    "categoryName": "Network",
    "description": "Network and VPN issues",
    "active": true
  }
]
```

Tables used:

- `ticket_categories`

### 2.5 Create a ticket

```http
POST /api/tickets
```

Purpose: Creates a new employee support request.

Request:

```json
{
  "title": "VPN is not connecting",
  "description": "The company VPN fails to connect.",
  "categoryId": "CATEGORY_UUID"
}
```

Response: A newly created ticket with status `NEW` and default priority
`MEDIUM`.

Tables used:

- `tickets`
- `users`
- `ticket_categories`
- `support_teams`
- `ticket_history`

### 2.6 Request AI ticket analysis

```http
POST /api/ai/analyze/{ticketId}
```

Purpose: Sends ticket information to the backend AI service and generates a
summary, priority, category, team, and troubleshooting recommendation.

Response:

```json
{
  "id": "ANALYSIS_UUID",
  "ticketId": "TICKET_UUID",
  "summary": "The employee cannot connect to the VPN.",
  "suggestedCategory": "Network",
  "suggestedPriority": "HIGH",
  "suggestedTeam": "Network Support",
  "troubleshootingSteps": "Check internet access and restart the VPN client.",
  "suggestedResponse": "Please restart the VPN application.",
  "recommendationStatus": "PENDING"
}
```

Tables used:

- `tickets`
- `ticket_ai_analysis`
- `ticket_categories`
- `support_teams`
- `users`

### 2.7 Load saved AI analysis

```http
GET /api/ai/analyze/{ticketId}
```

Purpose: Loads the latest saved AI analysis for a ticket.

Tables used:

- `ticket_ai_analysis`
- `tickets`

### 2.8 Add a public comment

```http
POST /api/tickets/{ticketId}/comments
```

Request:

```json
{
  "message": "I restarted the VPN but the issue continues.",
  "internalNote": false
}
```

Response:

```json
{
  "id": "COMMENT_UUID",
  "ticketId": "TICKET_UUID",
  "userId": "EMPLOYEE_UUID",
  "userName": "Pritam Patel",
  "message": "I restarted the VPN but the issue continues.",
  "internalNote": false,
  "createdAt": "2026-11-15T11:00:00"
}
```

Tables used:

- `ticket_comments`
- `tickets`
- `users`

### 2.9 View ticket comments

```http
GET /api/tickets/{ticketId}/comments
```

Purpose: Returns comments visible to the logged-in user.

Tables used:

- `ticket_comments`
- `tickets`
- `users`

### 2.10 Use the normal AI assistant

Get conversations:

```http
GET /api/ai/chat/conversations
```

Send a message:

```http
POST /api/ai/chat
```

Request:

```json
{
  "conversationId": null,
  "message": "My laptop is running slowly. What should I check?"
}
```

Response:

```json
{
  "conversationId": "CONVERSATION_UUID",
  "answer": "Close unused applications and restart the laptop.",
  "timestamp": "2026-11-15T11:10:00"
}
```

Tables used:

- `chat_conversations`
- Spring AI JDBC chat-memory tables
- `users`

### 2.11 Use Knowledge chat

```http
POST /api/ai/rag/chat
```

Request:

```json
{
  "conversationId": null,
  "message": "My company VPN is not connecting. What should I do?"
}
```

Response:

```json
{
  "conversationId": "CONVERSATION_UUID",
  "answer": "Verify internet access and restart the VPN application.",
  "sources": [
    {
      "articleId": "ARTICLE_UUID",
      "title": "Company VPN troubleshooting",
      "sourceTicketId": null
    }
  ],
  "timestamp": "2026-11-15T11:15:00"
}
```

Tables and storage used:

- `knowledge_articles`
- `smartresolve_knowledge_vectors`
- `chat_conversations`
- Spring AI JDBC chat-memory tables
- `users`

RAG flow:

```text
Employee question
        ↓
Vector search finds verified active articles
        ↓
Retrieved articles are sent as context to the LLM
        ↓
LLM generates the answer
        ↓
Answer and source articles return to the frontend
```

### 2.12 Rate the support team

Available only when the employee's ticket is `RESOLVED` or `CLOSED`.

```http
POST /api/tickets/{ticketId}/rating
```

Request:

```json
{
  "rating": 5,
  "feedback": "The support team resolved my issue quickly."
}
```

Response: Updated ticket response containing:

```json
{
  "supportTeamRating": 5,
  "supportTeamRatingFeedback": "The support team resolved my issue quickly."
}
```

Tables used:

- `support_team_ratings`
- `tickets`
- `support_teams`
- `users`

The rating table stores:

```text
id
ticket_id
support_team_id
employee_id
rating
feedback
created_at
updated_at
```

Each ticket can be rated only once. A second rating attempt returns HTTP 409
with the message `This ticket has already been rated`.

## 3. Admin flow

### 3.1 Load and filter all tickets

```http
GET /api/tickets/search?page=0&size=50
```

Admin filters:

```http
GET /api/tickets/search?page=0&size=50&status=OPEN&priority=HIGH
GET /api/tickets/search?page=0&size=50&categoryId=CATEGORY_UUID
```

Supported values:

```text
status: OPEN, ASSIGNED, RESOLVED
priority: LOW, MEDIUM, HIGH, CRITICAL
```

Purpose: Admins can search all permitted tickets by status, priority, and
category.

Tables used:

- `tickets`
- `users`
- `ticket_categories`
- `support_teams`
- `support_team_ratings`

### 3.2 Load active ticket categories

```http
GET /api/ticket-categories
```

Purpose: Populates the admin category filter.

Tables used:

- `ticket_categories`

### 3.3 Review AI analysis

```http
POST /api/ai/analyze/{ticketId}
GET /api/ai/analyze/{ticketId}
```

Purpose: Generates or loads the AI recommendation for the selected ticket.

Tables used:

- `tickets`
- `ticket_ai_analysis`
- `ticket_categories`
- `support_teams`

### 3.4 Accept an AI recommendation

```http
POST /api/ai/recommendations/{analysisId}/accept
```

Request:

```json
{
  "remarks": "Reviewed and approved"
}
```

Purpose: Accepts the recommendation and applies the suggested ticket values.

Tables used:

- `ticket_ai_analysis`
- `tickets`
- `ticket_categories`
- `support_teams`
- `ticket_history`
- `users`

### 3.5 Load support engineers

```http
GET /api/support/engineers
GET /api/support/teams/{teamId}/engineers
```

Purpose: Loads active support engineers for ticket assignment.

Tables used:

- `users`
- `support_teams`

### 3.6 Assign a ticket

```http
POST /api/tickets/{ticketId}/assign
```

Request:

```json
{
  "assignedEngineerId": "ENGINEER_UUID",
  "teamId": "TEAM_UUID",
  "remarks": "Assigned for investigation"
}
```

Response: Updated ticket containing assigned engineer and team information.

Tables used:

- `tickets`
- `users`
- `support_teams`
- `ticket_history`

### 3.7 Add an internal note

```http
POST /api/tickets/{ticketId}/comments
```

Request:

```json
{
  "message": "Investigate the VPN certificate issue.",
  "internalNote": true
}
```

Purpose: Adds a private support-team note that is not part of the employee
public conversation.

Tables used:

- `ticket_comments`
- `tickets`
- `users`

### 3.8 Create verified knowledge

Get articles:

```http
GET /api/knowledge/articles
```

Create an article:

```http
POST /api/knowledge/articles
```

Request:

```json
{
  "title": "Company VPN troubleshooting",
  "content": "Verify internet access, restart the VPN application, and sign in again.",
  "sourceTicketId": null
}
```

Purpose: Saves and indexes a verified article for Knowledge chat.

Tables and storage used:

- `knowledge_articles`
- `users`
- `tickets` when a source ticket is provided
- `smartresolve_knowledge_vectors`

## 4. Support engineer flow

### 4.1 Load assigned tickets

```http
GET /api/tickets/search?page=0&size=50
```

Support engineer filters:

```http
GET /api/tickets/search?page=0&size=50&status=ASSIGNED&priority=HIGH
```

Support engineers can filter by status and priority. The backend limits results
to tickets assigned to the logged-in support engineer.

Tables used:

- `tickets`
- `users`
- `support_teams`
- `ticket_categories`
- `support_team_ratings`

### 4.2 Review ticket AI analysis

```http
POST /api/ai/analyze/{ticketId}
GET /api/ai/analyze/{ticketId}
```

Purpose: Generates or loads troubleshooting guidance for the assigned ticket.

Tables used:

- `tickets`
- `ticket_ai_analysis`
- `ticket_categories`
- `support_teams`

### 4.3 Update ticket status

```http
PATCH /api/tickets/{ticketId}/status
```

Request:

```json
{
  "status": "IN_PROGRESS",
  "remarks": "Investigation started"
}
```

Response:

```json
{
  "id": "TICKET_UUID",
  "status": "IN_PROGRESS",
  "updatedAt": "2026-11-15T12:00:00"
}
```

When status becomes `RESOLVED`, `resolvedAt` is set. When status becomes
`CLOSED`, `closedAt` is set.

Tables used:

- `tickets`
- `ticket_history`
- `users`

### 4.4 Add a public comment

```http
POST /api/tickets/{ticketId}/comments
```

Request:

```json
{
  "message": "The VPN certificate was renewed. Please try again.",
  "internalNote": false
}
```

Tables used:

- `ticket_comments`
- `tickets`
- `users`

### 4.5 Add an internal note

```http
POST /api/tickets/{ticketId}/comments
```

Request:

```json
{
  "message": "Certificate renewal completed successfully.",
  "internalNote": true
}
```

Tables used:

- `ticket_comments`
- `tickets`
- `users`

### 4.6 Create verified knowledge

```http
POST /api/knowledge/articles
```

Request:

```json
{
  "title": "VPN certificate troubleshooting",
  "content": "Renew the VPN certificate, restart the VPN application, and test the connection.",
  "sourceTicketId": "RESOLVED_TICKET_UUID"
}
```

The source ticket must be resolved or closed and must contain resolution notes.

Tables and storage used:

- `knowledge_articles`
- `tickets`
- `users`
- `smartresolve_knowledge_vectors`

## 5. Database table summary

| Table | Description |
|---|---|
| `users` | Employee, admin, and support engineer accounts |
| `support_teams` | Support team names and descriptions |
| `ticket_categories` | Active and inactive ticket categories |
| `tickets` | Main ticket record |
| `ticket_history` | Ticket status, assignment, and activity history |
| `ticket_comments` | Public comments and internal notes |
| `ticket_ai_analysis` | AI summaries and recommendations |
| `chat_conversations` | Saved conversation metadata |
| Spring AI JDBC memory tables | Chat messages and conversation memory |
| `knowledge_articles` | Verified support solutions |
| `support_team_ratings` | Employee ratings and feedback |
| `smartresolve_knowledge_vectors` | Vector embeddings for RAG search |

## 6. Backend endpoints available but not currently used by visible frontend controls

These endpoints exist in the backend for future UI features:

```http
GET    /api/tickets/{ticketId}
PUT    /api/tickets/{ticketId}
GET    /api/tickets/{ticketId}/history
GET    /api/ai/chat/conversations/{conversationId}
PUT    /api/ai/recommendations/{analysisId}/modify
POST   /api/ai/recommendations/{analysisId}/reject
```

They use the following tables depending on the operation:

- `tickets`
- `ticket_history`
- `ticket_ai_analysis`
- `ticket_comments`
- `users`
- `ticket_categories`
- `support_teams`

