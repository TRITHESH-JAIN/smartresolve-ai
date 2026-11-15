export type Role = "EMPLOYEE" | "SUPPORT_ENGINEER" | "ADMIN";

export interface AuthUser {
  userId: string;
  fullName: string;
  email: string;
  role: Role;
  teamId?: string | null;
  teamName?: string | null;
}

export interface AuthResponse extends AuthUser {
  token: string;
  tokenType: string;
}

export interface SupportTeam {
  id: string;
  teamName: string;
  description: string;
  active: boolean;
}

export interface SupportEngineer {
  id: string;
  fullName: string;
  email: string;
  teamId: string | null;
  teamName: string | null;
}

export interface Ticket {
  id: string;
  title: string;
  description: string;
  categoryName: string | null;
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  status:
    | "NEW"
    | "AI_ANALYSIS"
    | "OPEN"
    | "ASSIGNED"
    | "IN_PROGRESS"
    | "ON_HOLD"
    | "RESOLVED"
    | "CLOSED"
    | "REOPENED";
  employeeId: string;
  employeeName: string;
  assignedEngineerId: string | null;
  assignedEngineerName: string | null;
  teamId: string | null;
  teamName: string | null;
  resolutionNotes: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
  supportTeamRating: number | null;
  supportTeamRatingFeedback: string | null;
}

export interface TicketPage {
  content: Ticket[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TicketCategory {
  id: string;
  categoryName: string;
  description: string | null;
  active: boolean;
}

export interface AIAnalysis {
  id: string;
  ticketId: string;
  summary: string;
  suggestedCategory: string | null;
  suggestedPriority: Ticket["priority"] | null;
  suggestedTeam: string | null;
  troubleshootingSteps: string;
  suggestedResponse: string | null;
  recommendationStatus: "PENDING" | "ACCEPTED" | "MODIFIED" | "REJECTED";
  reviewedById: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface ChatResult {
  conversationId: string;
  answer: string;
  timestamp: string;
}

export interface RAGResult extends ChatResult {
  sources: Array<{
    articleId: string;
    title: string;
    sourceTicketId: string | null;
  }>;
}

export interface KnowledgeArticle {
  id: string;
  title: string;
  sourceTicketId: string | null;
  verified: boolean;
  verifiedBy: string | null;
  createdAt: string;
}

export interface TicketComment {
  id: string;
  ticketId: string;
  userId: string;
  userName: string;
  message: string;
  internalNote: boolean;
  createdAt: string;
}

export interface ChatConversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}
