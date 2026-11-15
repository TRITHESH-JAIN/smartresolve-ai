import type {
  AIAnalysis,
  AuthResponse,
  ChatConversation,
  ChatResult,
  RAGResult,
  SupportEngineer,
  SupportTeam,
  Ticket,
  TicketComment,
  TicketPage
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const text = await response.text();
  let body: unknown = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof body === "object" &&
      body !== null &&
      "message" in body &&
      typeof body.message === "string"
        ? body.message
        : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }
  return body as T;
}

const json = (body: unknown): RequestInit => ({
  method: "POST",
  body: JSON.stringify(body)
});

export const api = {
  register: (body: {
    fullName: string;
    email: string;
    password: string;
    role: "EMPLOYEE" | "SUPPORT_ENGINEER" | "ADMIN";
    teamId: string | null;
  }) => request<AuthResponse>("/auth/register", json(body)),

  login: (body: { email: string; password: string }) =>
    request<AuthResponse>("/auth/login", json(body)),

  getSupportTeams: () => request<SupportTeam[]>("/support/teams"),

  getSupportEngineers: (token: string, teamId?: string) =>
    request<SupportEngineer[]>(
      teamId ? `/support/teams/${teamId}/engineers` : "/support/engineers",
      {},
      token
    ),

  getTickets: (token: string) =>
    request<TicketPage>("/tickets/search?page=0&size=50", {}, token),

  createTicket: (
    token: string,
    body: { title: string; description: string; categoryId: string | null }
  ) => request<Ticket>("/tickets", json(body), token),

  analyzeTicket: (token: string, ticketId: string) =>
    request<AIAnalysis>(`/ai/analyze/${ticketId}`, { method: "POST" }, token),

  getLatestAnalysis: (token: string, ticketId: string) =>
    request<AIAnalysis>(`/ai/analyze/${ticketId}`, {}, token),

  acceptRecommendation: (
    token: string,
    analysisId: string,
    remarks: string
  ) =>
    request<AIAnalysis>(
      `/ai/recommendations/${analysisId}/accept`,
      json({ remarks }),
      token
    ),

  assignTicket: (
    token: string,
    ticketId: string,
    body: { assignedEngineerId: string; teamId: string | null; remarks: string }
  ) => request<Ticket>(`/tickets/${ticketId}/assign`, json(body), token),

  updateStatus: (
    token: string,
    ticketId: string,
    body: { status: Ticket["status"]; remarks: string }
  ) => request<Ticket>(`/tickets/${ticketId}/status`, {
    method: "PATCH",
    body: JSON.stringify(body)
  }, token),

  addComment: (
    token: string,
    ticketId: string,
    body: { message: string; internalNote: boolean }
  ) => request<TicketComment>(`/tickets/${ticketId}/comments`, json(body), token),

  getComments: (token: string, ticketId: string) =>
    request<TicketComment[]>(`/tickets/${ticketId}/comments`, {}, token),

  getConversations: (token: string) =>
    request<ChatConversation[]>("/ai/chat/conversations", {}, token),

  chat: (token: string, conversationId: string | null, message: string) =>
    request<ChatResult>(
      "/ai/chat",
      json({ conversationId, message }),
      token
    ),

  ragChat: (token: string, conversationId: string | null, message: string) =>
    request<RAGResult>(
      "/ai/rag/chat",
      json({ conversationId, message }),
      token
    )
};
