import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "./api";
import type {
  AIAnalysis,
  AuthResponse,
  ChatConversation,
  KnowledgeArticle,
  RAGResult,
  SupportEngineer,
  SupportTeam,
  Ticket,
  TicketCategory,
  TicketComment
} from "./types";

type View = "tickets" | "chat" | "rag" | "knowledge";
type ChatMessage = { role: "user" | "assistant"; content: string };

const authStorageKey = "smartresolve.auth";

function readAuth(): AuthResponse | null {
  const saved = localStorage.getItem(authStorageKey);
  if (!saved) return null;
  try {
    return JSON.parse(saved) as AuthResponse;
  } catch {
    localStorage.removeItem(authStorageKey);
    return null;
  }
}

function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(readAuth);
  const [view, setView] = useState<View>("tickets");

  if (!auth) {
    return (
      <AuthScreen
        onAuthenticated={(nextAuth) => {
          localStorage.setItem(authStorageKey, JSON.stringify(nextAuth));
          setAuth(nextAuth);
        }}
      />
    );
  }

  const logout = () => {
    localStorage.removeItem(authStorageKey);
    setAuth(null);
  };
  const canManageKnowledge = auth.role === "ADMIN" || auth.role === "SUPPORT_ENGINEER";

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <div className="brand-mark">SR</div>
          <div>
            <strong>SmartResolve</strong>
            <span>AI support workspace</span>
          </div>
        </div>
        <div className="account">
          <div className="avatar">{auth.fullName.charAt(0).toUpperCase()}</div>
          <div>
            <strong>{auth.fullName}</strong>
            <span>{auth.role.toLowerCase().replace("_", " ")}</span>
          </div>
          <button className="link-button" onClick={logout}>Sign out</button>
        </div>
      </header>
      <div className="workspace">
        <aside className="sidebar">
          <p className="eyebrow">Workspace</p>
          <button className={view === "tickets" ? "nav-button active" : "nav-button"} onClick={() => setView("tickets")}>
            <span>▣</span> My tickets
          </button>
          <button className={view === "chat" ? "nav-button active" : "nav-button"} onClick={() => setView("chat")}>
            <span>✦</span> AI assistant
          </button>
          <button className={view === "rag" ? "nav-button active" : "nav-button"} onClick={() => setView("rag")}>
            <span>⌁</span> Knowledge chat
          </button>
          {canManageKnowledge && <button className={view === "knowledge" ? "nav-button active" : "nav-button"} onClick={() => setView("knowledge")}>
            <span>＋</span> Manage knowledge
          </button>}
          <div className="sidebar-note">
            <strong>Need help?</strong>
            <span>Ask the AI assistant or create a ticket for your support team.</span>
          </div>
        </aside>
        <main className="main-content">
          {view === "tickets" && <TicketsView auth={auth} />}
          {view === "chat" && <ChatView auth={auth} rag={false} />}
          {view === "rag" && <ChatView auth={auth} rag />}
          {view === "knowledge" && canManageKnowledge && <KnowledgeView auth={auth} />}
        </main>
      </div>
    </div>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (auth: AuthResponse) => void }) {
  const [registerMode, setRegisterMode] = useState(false);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"EMPLOYEE" | "SUPPORT_ENGINEER" | "ADMIN">("EMPLOYEE");
  const [teamId, setTeamId] = useState("");
  const [teams, setTeams] = useState<SupportTeam[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!registerMode) return;
    void api.getSupportTeams().then(setTeams).catch(() => {
      setError("Could not load support teams.");
    });
  }, [registerMode]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setNotice("");
    setBusy(true);
    try {
      if (registerMode) {
        await api.register({
            fullName,
            email,
            password,
            role,
            teamId: role === "SUPPORT_ENGINEER" ? teamId : null
        });
        setRegisterMode(false);
        setPassword("");
        setTeamId("");
        setNotice("Account created successfully. Please sign in to continue.");
        return;
      }

      onAuthenticated(await api.login({ email, password }));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Unable to connect to SmartResolve.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-hero">
        <div className="brand-lockup"><div className="brand-mark">SR</div><strong>SmartResolve</strong></div>
        <div className="hero-copy">
          <p className="eyebrow">IT support, made intelligent</p>
          <h1>Resolve issues with a little more clarity.</h1>
          <p>Raise tickets, get AI guidance, and search verified support knowledge from one focused workspace.</p>
        </div>
        <div className="hero-stats"><span><strong>24/7</strong> AI guidance</span><span><strong>1</strong> support workspace</span></div>
      </section>
      <section className="auth-card">
        <p className="eyebrow">{registerMode ? "Create your account" : "Welcome back"}</p>
        <h2>{registerMode ? "Join SmartResolve" : "Sign in to your workspace"}</h2>
        <p className="muted">{registerMode ? "Create an employee account to raise and track support requests." : "Use your SmartResolve credentials to continue."}</p>
        {error && <div className="error-banner">{error}</div>}
        {notice && <div className="success-banner">{notice}</div>}
        <form onSubmit={submit} className="stack-form">
          {registerMode && <label>Full name<input value={fullName} onChange={(event) => setFullName(event.target.value)} required maxLength={100} /></label>}
          <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} /></label>
          {registerMode && <label>Role<select value={role} onChange={(event) => { setRole(event.target.value as typeof role); if (event.target.value !== "SUPPORT_ENGINEER") setTeamId(""); }}>
            <option value="EMPLOYEE">Employee</option>
            <option value="SUPPORT_ENGINEER">Support engineer</option>
            <option value="ADMIN">Admin</option>
          </select></label>}
          {registerMode && role === "SUPPORT_ENGINEER" && <label>Support team<select value={teamId} onChange={(event) => setTeamId(event.target.value)} required>
            <option value="">Select a support team</option>
            {teams.map((team) => <option key={team.id} value={team.id}>{team.teamName}</option>)}
          </select></label>}
          <button className="primary-button" disabled={busy}>{busy ? "Please wait..." : registerMode ? "Create account" : "Sign in"}</button>
        </form>
        <button className="switch-button" onClick={() => { setRegisterMode(!registerMode); setError(""); setNotice(""); }}>
          {registerMode ? "Already have an account? Sign in" : "New to SmartResolve? Create an account"}
        </button>
      </section>
    </div>
  );
}

function TicketsView({ auth }: { auth: AuthResponse }) {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selected, setSelected] = useState<Ticket | null>(null);
  const [analysis, setAnalysis] = useState<AIAnalysis | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [reviewRemarks, setReviewRemarks] = useState("");
  const [engineerId, setEngineerId] = useState("");
  const [teamId, setTeamId] = useState("");
  const [engineers, setEngineers] = useState<SupportEngineer[]>([]);
  const [allEngineers, setAllEngineers] = useState<SupportEngineer[]>([]);
  const [teams, setTeams] = useState<SupportTeam[]>([]);
  const [assignmentRemarks, setAssignmentRemarks] = useState("");
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [status, setStatus] = useState<Ticket["status"]>("NEW");
  const [filterStatus, setFilterStatus] = useState<Ticket["status"] | "">("");
  const [filterPriority, setFilterPriority] = useState<Ticket["priority"] | "">("");
  const [filterCategoryId, setFilterCategoryId] = useState("");
  const [categories, setCategories] = useState<TicketCategory[]>([]);
  const [statusRemarks, setStatusRemarks] = useState("");
  const [commentMessage, setCommentMessage] = useState("");
  const [internalNote, setInternalNote] = useState(false);
  const [teamRating, setTeamRating] = useState(0);
  const [ratingFeedback, setRatingFeedback] = useState("");
  const canManageTickets = auth.role === "ADMIN" || auth.role === "SUPPORT_ENGINEER";
  const canFilterTickets = canManageTickets;
  const canFilterCategories = auth.role === "ADMIN";
  const canUpdateTicketStatus = auth.role === "SUPPORT_ENGINEER";
  const canAssignTickets = auth.role === "ADMIN";

  useEffect(() => {
    if (!canManageTickets) return;
    Promise.all([
      api.getSupportTeams(),
      api.getSupportEngineers(auth.token)
    ]).then(([loadedTeams, loadedEngineers]) => {
      setTeams(loadedTeams);
      setAllEngineers(loadedEngineers);
    }).catch((reason) => {
      setError(reason instanceof ApiError ? reason.message : "Could not load support directory.");
    });
  }, [auth.token, canManageTickets]);

  useEffect(() => {
    if (!canFilterCategories) return;
    api.getTicketCategories(auth.token).then(setCategories).catch((reason) => {
      setError(reason instanceof ApiError ? reason.message : "Could not load ticket categories.");
    });
  }, [auth.token, canFilterCategories]);

  const loadTickets = async (filters = {
    status: filterStatus || undefined,
    priority: filterPriority || undefined,
    categoryId: filterCategoryId || undefined
  }) => {
    try {
      const page = await api.getTickets(auth.token, canFilterTickets ? filters : {});
      setTickets(page.content);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not load tickets.");
    }
  };
  useEffect(() => { void loadTickets(); }, [auth.token]);

  function clearTicketFilters() {
    setFilterStatus("");
    setFilterPriority("");
    setFilterCategoryId("");
    void loadTickets({ status: undefined, priority: undefined, categoryId: undefined });
  }

  async function selectTicket(ticket: Ticket) {
    setSelected(ticket);
    setAnalysis(null);
    setStatus(ticket.status);
    setComments([]);
    try {
      setComments(await api.getComments(auth.token, ticket.id));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not load ticket comments.");
    }
  }

  async function createTicket(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const created = await api.createTicket(auth.token, { title, description, categoryId: null });
      setTickets((current) => [created, ...current]);
      setTitle("");
      setDescription("");
      setShowForm(false);
      await selectTicket(created);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not create ticket.");
    } finally {
      setBusy(false);
    }
  }

  async function analyze(ticket: Ticket) {
    setBusy(true);
    setError("");
    setSelected(ticket);
    try {
      setAnalysis(await api.analyzeTicket(auth.token, ticket.id));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "AI analysis failed.");
    } finally {
      setBusy(false);
    }
  }

  async function loadLatestAnalysis(ticket: Ticket) {
    setBusy(true);
    setError("");
    setSelected(ticket);
    try {
      setAnalysis(await api.getLatestAnalysis(auth.token, ticket.id));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not load the saved AI analysis.");
    } finally {
      setBusy(false);
    }
  }

  async function acceptRecommendation() {
    if (!analysis) return;
    setBusy(true);
    setError("");
    try {
      const accepted = await api.acceptRecommendation(
        auth.token,
        analysis.id,
        reviewRemarks
      );
      setAnalysis(accepted);
      await loadTickets();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not accept the recommendation.");
    } finally {
      setBusy(false);
    }
  }

  async function assignTicket(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setBusy(true);
    setError("");
    try {
      const assigned = await api.assignTicket(auth.token, selected.id, {
        assignedEngineerId: engineerId.trim(),
        teamId: teamId.trim() || null,
        remarks: assignmentRemarks
      });
      setSelected(assigned);
      setTickets((current) =>
        current.map((ticket) => ticket.id === assigned.id ? assigned : ticket)
      );
      setEngineerId("");
      setTeamId("");
      setAssignmentRemarks("");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not assign the ticket.");
    } finally {
      setBusy(false);
    }
  }

  async function selectAssignmentTeam(nextTeamId: string) {
    setTeamId(nextTeamId);
    setEngineerId("");
    setEngineers([]);
    if (!nextTeamId) return;

    try {
      const teamEngineers = await api.getSupportEngineers(auth.token, nextTeamId);
      setEngineers(
        teamEngineers.length > 0
          ? teamEngineers
          : allEngineers.filter((engineer) => engineer.teamId === nextTeamId)
      );
    } catch (reason) {
      const fallbackEngineers = allEngineers.filter(
        (engineer) => engineer.teamId === nextTeamId
      );
      if (fallbackEngineers.length > 0) {
        setEngineers(fallbackEngineers);
        return;
      }
      setError(reason instanceof ApiError ? reason.message : "Could not load team members.");
    }
  }

  async function updateStatus(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setBusy(true);
    setError("");
    try {
      const updated = await api.updateStatus(auth.token, selected.id, {
        status,
        remarks: statusRemarks
      });
      setSelected(updated);
      setTickets((current) =>
        current.map((ticket) => ticket.id === updated.id ? updated : ticket)
      );
      setStatusRemarks("");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not update ticket status.");
    } finally {
      setBusy(false);
    }
  }

  async function addComment(event: FormEvent) {
    event.preventDefault();
    if (!selected || !commentMessage.trim()) return;
    setBusy(true);
    setError("");
    try {
      const comment = await api.addComment(auth.token, selected.id, {
        message: commentMessage,
        internalNote
      });
      setComments((current) => [...current, comment]);
      setCommentMessage("");
      setInternalNote(false);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not add comment.");
    } finally {
      setBusy(false);
    }
  }

  async function rateSupportTeam(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    const rating = teamRating || selected.supportTeamRating || 0;
    if (rating < 1) return;
    setBusy(true);
    setError("");
    try {
      const updated = await api.rateSupportTeam(auth.token, selected.id, {
        rating,
        feedback: ratingFeedback
      });
      setSelected(updated);
      setTickets((current) =>
        current.map((ticket) => ticket.id === updated.id ? updated : ticket)
      );
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not save the support team rating.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div><p className="eyebrow">Employee workspace</p><h1>My support tickets</h1><p className="muted">Track your requests and ask AI for safe first-step guidance.</p></div>
        <button className="primary-button" onClick={() => setShowForm(!showForm)}>＋ New ticket</button>
      </div>
      {error && <div className="error-banner">{error}</div>}
      {showForm && <form className="panel ticket-form" onSubmit={createTicket}>
        <div className="panel-heading"><div><h2>Raise a support ticket</h2><p className="muted">Describe the issue clearly so the AI and support team have useful context.</p></div><button type="button" className="icon-button" onClick={() => setShowForm(false)}>×</button></div>
        <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="e.g. Laptop keeps disconnecting from Wi-Fi" required maxLength={200} /></label>
        <label>Description<textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder="What happened? When did it start? What have you already tried?" required maxLength={5000} rows={5} /></label>
        <button className="primary-button" disabled={busy}>{busy ? "Creating..." : "Create ticket"}</button>
      </form>}
      <div className="ticket-layout">
        <section className="panel ticket-list">
          <div className="panel-heading"><div><h2>Recent tickets</h2><p className="muted">{tickets.length} request{tickets.length === 1 ? "" : "s"}</p></div><button className="link-button" onClick={() => void loadTickets()}>Refresh</button></div>
          {canFilterTickets && <form className="ticket-filters" onSubmit={(event) => { event.preventDefault(); void loadTickets(); }}>
            <label>Status<select value={filterStatus} onChange={(event) => setFilterStatus(event.target.value as Ticket["status"] | "")}>
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="RESOLVED">Resolved</option>
            </select></label>
            <label>Priority<select value={filterPriority} onChange={(event) => setFilterPriority(event.target.value as Ticket["priority"] | "")}>
              <option value="">All priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select></label>
            {canFilterCategories && <label>Category<select value={filterCategoryId} onChange={(event) => setFilterCategoryId(event.target.value)}>
              <option value="">All categories</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.categoryName}</option>)}
            </select></label>}
            <div className="ticket-filter-actions"><button className="secondary-button" type="submit">Apply filters</button><button className="link-button" type="button" onClick={clearTicketFilters}>Clear</button></div>
          </form>}
          {tickets.length === 0 ? <div className="empty-state"><span>◌</span><strong>No tickets yet</strong><p>Create your first support request to get started.</p></div> : tickets.map((ticket) => <button key={ticket.id} className={selected?.id === ticket.id ? "ticket-row selected" : "ticket-row"} onClick={() => void selectTicket(ticket)}>
            <div className="ticket-row-top"><strong>{ticket.title}</strong><StatusBadge status={ticket.status} /></div>
            <p>{ticket.description}</p><small>{new Date(ticket.createdAt).toLocaleString()} · {ticket.priority}</small>
          </button>)}
        </section>
        <section className="panel ticket-detail">
          {!selected ? <div className="empty-state"><span>↖</span><strong>Select a ticket</strong><p>Choose a ticket to view its details and request an AI analysis.</p></div> : <>
            <div className="panel-heading"><div><p className="eyebrow">Ticket details</p><h2>{selected.title}</h2></div><StatusBadge status={selected.status} /></div>
            <p className="detail-description">{selected.description}</p>
            <div className="detail-meta"><span><small>Priority</small><strong>{selected.priority}</strong></span><span><small>Assigned team</small><strong>{selected.teamName ?? "Not assigned"}</strong></span><span><small>Created</small><strong>{new Date(selected.createdAt).toLocaleDateString()}</strong></span></div>
            <div className="ticket-actions">
              <button className="secondary-button" onClick={() => void analyze(selected)} disabled={busy}>{busy ? "Analyzing..." : "✦ Get AI analysis"}</button>
              {canManageTickets && <button className="secondary-button" onClick={() => void loadLatestAnalysis(selected)} disabled={busy}>Load saved analysis</button>}
            </div>
            {analysis && <AnalysisCard
              analysis={analysis}
              canManage={canManageTickets}
              busy={busy}
              reviewRemarks={reviewRemarks}
              onReviewRemarksChange={setReviewRemarks}
              onAccept={() => void acceptRecommendation()}
            />}
            {canAssignTickets && <form className="assignment-card" onSubmit={assignTicket}>
              <div><p className="eyebrow">Admin action</p><h3>Assign this ticket</h3><p className="muted">Choose an active support engineer and team.</p></div>
              <label>Support team<select value={teamId} onChange={(event) => void selectAssignmentTeam(event.target.value)} required>
                <option value="">Select a support team</option>
                {teams.map((team) => <option key={team.id} value={team.id}>{team.teamName}</option>)}
              </select></label>
              <label>Support engineer<select value={engineerId} onChange={(event) => setEngineerId(event.target.value)} disabled={!teamId || engineers.length === 0} required>
                <option value="">{teamId ? "Select a team member" : "Select a team first"}</option>
                {engineers.map((engineer) => <option key={engineer.id} value={engineer.id}>{engineer.fullName}</option>)}
              </select></label>
              <label>Assignment remarks<input value={assignmentRemarks} onChange={(event) => setAssignmentRemarks(event.target.value)} placeholder="Assigned for investigation" maxLength={1000} /></label>
              <button className="primary-button" disabled={busy}>{busy ? "Assigning..." : "Assign to support engineer"}</button>
            </form>}
            {canUpdateTicketStatus && <form className="support-actions-card" onSubmit={updateStatus}>
              <div><p className="eyebrow">Support engineer action</p><h3>Update ticket status</h3></div>
              <div className="inline-fields"><label>Status<select value={status} onChange={(event) => setStatus(event.target.value as Ticket["status"])}>
                {["NEW", "OPEN", "ASSIGNED", "IN_PROGRESS", "ON_HOLD", "RESOLVED", "CLOSED", "REOPENED"].map((option) => <option key={option} value={option}>{option.replace("_", " ")}</option>)}
              </select></label><label>Remarks<input value={statusRemarks} onChange={(event) => setStatusRemarks(event.target.value)} maxLength={1000} placeholder="Resolution or progress note" /></label></div>
              <button className="primary-button" disabled={busy}>{busy ? "Updating..." : "Update status"}</button>
            </form>}
            {auth.role === "EMPLOYEE"
              && (selected.status === "RESOLVED" || selected.status === "CLOSED")
              && selected.teamId
              && selected.supportTeamRating
              && <div className="rating-card">
                <div><p className="eyebrow">Support team feedback</p><h3>Thank you for your feedback</h3><p className="muted">You have already rated this support team.</p></div>
                <div className="rating-stars" aria-label={`Submitted rating: ${selected.supportTeamRating} out of 5`}>
                  {[1, 2, 3, 4, 5].map((value) => <span key={value} className={value <= selected.supportTeamRating! ? "rating-star selected" : "rating-star"}>★</span>)}
                </div>
                {selected.supportTeamRatingFeedback && <p className="rating-feedback">{selected.supportTeamRatingFeedback}</p>}
              </div>}
            {auth.role === "EMPLOYEE"
              && (selected.status === "RESOLVED" || selected.status === "CLOSED")
              && selected.teamId
              && !selected.supportTeamRating
              && <form className="rating-card" onSubmit={rateSupportTeam}>
                <div><p className="eyebrow">Support team feedback</p><h3>Rate your support experience</h3><p className="muted">Your rating helps us improve the support team.</p></div>
                <div className="rating-stars" role="radiogroup" aria-label="Support team rating">
                  {[1, 2, 3, 4, 5].map((value) => <button key={value} type="button" className={value <= teamRating ? "rating-star selected" : "rating-star"} onClick={() => setTeamRating(value)} aria-label={`${value} star${value === 1 ? "" : "s"}`}>★</button>)}
                </div>
                <textarea value={ratingFeedback} onChange={(event) => setRatingFeedback(event.target.value)} placeholder="Optional feedback" maxLength={2000} rows={3} />
                <button className="primary-button" disabled={busy || !teamRating}>{busy ? "Saving..." : "Submit rating"}</button>
              </form>}
            <section className="comments-card">
              <div className="panel-heading"><div><p className="eyebrow">Ticket communication</p><h3>Comments and visibility</h3></div><button type="button" className="link-button" onClick={() => void selectTicket(selected)}>Refresh</button></div>
              <div className="comment-list">{comments.length === 0 ? <p className="muted">No comments yet.</p> : comments.map((comment) => <div className="comment-item" key={comment.id}><div><strong>{comment.userName}</strong><small>{new Date(comment.createdAt).toLocaleString()} {comment.internalNote ? "· Internal note" : ""}</small></div><p>{comment.message}</p></div>)}</div>
              <form onSubmit={addComment} className="comment-form"><textarea value={commentMessage} onChange={(event) => setCommentMessage(event.target.value)} placeholder="Add a message for the support team..." maxLength={3000} rows={3} required />{canManageTickets && <label className="checkbox-label"><input type="checkbox" checked={internalNote} onChange={(event) => setInternalNote(event.target.checked)} /> Internal note</label>}<button className="secondary-button" disabled={busy || !commentMessage.trim()}>Add comment</button></form>
            </section>
          </>}
        </section>
      </div>
    </>
  );
}

function AnalysisCard({
  analysis,
  canManage,
  busy,
  reviewRemarks,
  onReviewRemarksChange,
  onAccept
}: {
  analysis: AIAnalysis;
  canManage: boolean;
  busy: boolean;
  reviewRemarks: string;
  onReviewRemarksChange: (value: string) => void;
  onAccept: () => void;
}) {
  return <div className="analysis-card">
    <div className="analysis-title"><div><p className="eyebrow">AI recommendation</p><h3>{analysis.summary}</h3></div><span className="status-pill pending">{analysis.recommendationStatus}</span></div>
    <div className="recommendation-grid"><span><small>Category</small><strong>{analysis.suggestedCategory ?? "Not specified"}</strong></span><span><small>Priority</small><strong>{analysis.suggestedPriority ?? "Not specified"}</strong></span><span><small>Team</small><strong>{analysis.suggestedTeam ?? "Not specified"}</strong></span></div>
    <div className="analysis-section"><small>Suggested troubleshooting</small><p>{analysis.troubleshootingSteps || "No troubleshooting steps returned."}</p></div>
    <div className="analysis-section"><small>Suggested response</small><p>{analysis.suggestedResponse ?? "No suggested response returned."}</p></div>
    {canManage && analysis.recommendationStatus === "PENDING" && <div className="review-actions">
      <label>Review remarks<input value={reviewRemarks} onChange={(event) => onReviewRemarksChange(event.target.value)} placeholder="Reviewed and approved" maxLength={1000} /></label>
      <button type="button" className="primary-button" onClick={onAccept} disabled={busy}>✓ Accept AI recommendation</button>
    </div>}
    {canManage && analysis.recommendationStatus === "ACCEPTED" && <p className="accepted-note">This AI recommendation has been accepted and applied to the ticket.</p>}
    <p className="ai-disclaimer">AI recommendations are suggestions. A support engineer or admin must review them before applying them to the ticket.</p>
  </div>;
}

function StatusBadge({ status }: { status: Ticket["status"] }) {
  return <span className={`status-pill ${status.toLowerCase()}`}>{status.replace("_", " ")}</span>;
}

function KnowledgeView({ auth }: { auth: AuthResponse }) {
  const [articles, setArticles] = useState<KnowledgeArticle[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [sourceTicketId, setSourceTicketId] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadArticles = async () => {
    try {
      setArticles(await api.getKnowledgeArticles(auth.token));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not load knowledge articles.");
    }
  };

  useEffect(() => {
    void loadArticles();
  }, [auth.token]);

  async function createArticle(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const article = await api.createKnowledgeArticle(auth.token, {
        title: title.trim(),
        content: content.trim(),
        sourceTicketId: sourceTicketId.trim() || null
      });
      setArticles((current) => [article, ...current]);
      setTitle("");
      setContent("");
      setSourceTicketId("");
      setShowForm(false);
      setNotice("Knowledge article added and indexed for Knowledge chat.");
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not add the knowledge article.");
    } finally {
      setBusy(false);
    }
  }

  return <div>
    <div className="page-heading">
      <div><p className="eyebrow">Admin and support tools</p><h1>Manage knowledge</h1><p className="muted">Add verified solutions that employees can search in Knowledge chat.</p></div>
      <button className="primary-button" onClick={() => setShowForm((current) => !current)}>＋ Add article</button>
    </div>
    {error && <div className="error-banner">{error}</div>}
    {notice && <div className="success-banner">{notice}</div>}
    {showForm && <form className="panel ticket-form" onSubmit={createArticle}>
      <div className="panel-heading"><div><h2>Add verified knowledge</h2><p className="muted">Write the solution, not just a question or prompt.</p></div><button type="button" className="icon-button" onClick={() => setShowForm(false)}>×</button></div>
      <label>Article title<input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="e.g. Company VPN troubleshooting" required maxLength={200} /></label>
      <label>Verified solution<textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="Describe the verified steps an employee should follow..." required maxLength={10000} rows={7} /></label>
      <label>Resolved ticket ID <span className="optional">(optional)</span><input value={sourceTicketId} onChange={(event) => setSourceTicketId(event.target.value)} placeholder="UUID of a resolved or closed ticket" /></label>
      <button className="primary-button" disabled={busy}>{busy ? "Adding..." : "Add and index article"}</button>
    </form>}
    <section className="panel">
      <div className="panel-heading"><div><h2>Verified articles</h2><p className="muted">{articles.length} article{articles.length === 1 ? "" : "s"} available to Knowledge chat.</p></div><button className="link-button" onClick={() => void loadArticles()}>Refresh</button></div>
      {articles.length === 0 ? <div className="empty-state"><span>⌁</span><strong>No knowledge articles yet</strong><p>Add a verified solution for employees to use.</p></div> : <div className="knowledge-list">{articles.map((article) => <div className="knowledge-row" key={article.id}><div><strong>{article.title}</strong><p>{article.verifiedBy ? `Verified by ${article.verifiedBy}` : "Verified article"}</p></div><small>{new Date(article.createdAt).toLocaleDateString()}</small></div>)}</div>}
    </section>
  </div>;
}

function ChatView({ auth, rag }: { auth: AuthResponse; rag: boolean }) {
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [message, setMessage] = useState("");
  const [sources, setSources] = useState<RAGResult["sources"]>([]);
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const title = rag ? "Knowledge chat" : "AI assistant";
  const subtitle = rag ? "Ask questions grounded in verified internal support knowledge." : "Get safe troubleshooting guidance before raising or updating a ticket.";

  useEffect(() => {
    void api.getConversations(auth.token).then(setConversations).catch(() => undefined);
  }, [auth.token]);

  async function send(event: FormEvent) {
    event.preventDefault();
    const currentMessage = message.trim();
    if (!currentMessage || busy) return;
    setMessage("");
    setError("");
    setMessages((current) => [...current, { role: "user", content: currentMessage }]);
    setBusy(true);
    try {
      const result = rag
        ? await api.ragChat(auth.token, conversationId, currentMessage)
        : await api.chat(auth.token, conversationId, currentMessage);
      setConversationId(result.conversationId);
      setMessages((current) => [...current, { role: "assistant", content: result.answer }]);
      if (rag) setSources((result as RAGResult).sources);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "The AI assistant is unavailable.");
    } finally {
      setBusy(false);
    }
  }

  return <div className="chat-page">
    <div className="page-heading"><div><p className="eyebrow">{rag ? "Verified knowledge" : "Personal assistant"}</p><h1>{title}</h1><p className="muted">{subtitle}</p></div><span className="mode-indicator">{rag ? "RAG mode" : "Chat memory active"}</span></div>
    <section className="chat-panel panel">
      {conversations.length > 0 && <div className="conversation-strip"><strong>Saved conversations</strong>{conversations.slice(0, 5).map((conversation) => <button key={conversation.id} onClick={() => { setConversationId(conversation.id); setMessages([]); setSources([]); }}>{conversation.title}</button>)}</div>}
      <div className="chat-messages">
        {messages.length === 0 && <div className="chat-welcome"><div className="sparkle">✦</div><h2>{rag ? "Search the support knowledge base" : "How can I help today?"}</h2><p>{rag ? "Ask about an IT issue and I will use verified solutions when a relevant article exists." : "Describe an IT issue and I will suggest safe, practical next steps."}</p></div>}
        {messages.map((item, index) => <div key={`${item.role}-${index}`} className={`message ${item.role}`}><div className="message-label">{item.role === "user" ? auth.fullName : "SmartResolve AI"}</div><div className="message-bubble">{item.content}</div></div>)}
        {busy && <div className="message assistant"><div className="message-label">SmartResolve AI</div><div className="message-bubble typing">Thinking<span>.</span><span>.</span><span>.</span></div></div>}
      </div>
      {error && <div className="error-banner">{error}</div>}
      {rag && sources.length > 0 && <div className="sources"><strong>Sources used</strong>{sources.map((source) => <span key={source.articleId}>{source.title}</span>)}</div>}
      <form className="chat-composer" onSubmit={send}><textarea value={message} onChange={(event) => setMessage(event.target.value)} placeholder={rag ? "Ask about a support issue..." : "Describe your IT issue..."} rows={2} maxLength={5000} /><button className="primary-button" disabled={busy || !message.trim()}>Send <span>↗</span></button></form>
      {conversationId && <small className="conversation-note">Conversation memory active · {conversationId.slice(0, 8)}...</small>}
    </section>
  </div>;
}

export default App;
