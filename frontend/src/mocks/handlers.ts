import { http, HttpResponse } from "msw";
import {
  inquiries,
  nextInquiryId,
  nextDraftId,
  nowKst,
} from "./store";
import type { InquiryStatus } from "../common/types";
import type { KanbanBoard } from "../common/apiTypes";
import type { StoredInquiry } from "./store";

const BASE = "/api/v1";
const REGENERATION_LIMIT = 3;

function ok<T>(data: T, init?: ResponseInit) {
  return HttpResponse.json({ data, timestamp: nowKst() }, init);
}
function err(code: string, message: string, status: number) {
  return HttpResponse.json({ error: { code, message }, timestamp: nowKst() }, { status });
}

function toCard(s: StoredInquiry) {
  return {
    inquiryId: s.inquiry.inquiryId,
    customerType: s.inquiry.customerType,
    aiType: s.analysis?.aiType,
    urgency: s.analysis?.urgency ?? "NORMAL",
    summary: s.analysis?.summary,
    status: s.inquiry.status,
    assignedOperator: s.inquiry.assignedOperator,
    createdAt: s.inquiry.createdAt,
    completedAt: s.inquiry.status === "SENT" ? s.inquiry.completedAt ?? null : null,
  };
}

export const handlers = [
  // 로그인
  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { username?: string; password?: string };
    if (!body.username || !body.password) {
      return err("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.", 401);
    }
    // 데모: 비밀번호가 'wrong'이면 실패
    if (body.password === "wrong") {
      return err("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.", 401);
    }
    return ok({
      token: "mock-jwt-token-" + Date.now(),
      operator: { id: "op-1", username: body.username, role: "OPERATOR" },
      expiresAt: nowKst(-60),
    });
  }),

  http.post(`${BASE}/auth/logout`, () => ok({ success: true })),

  // 고객 문의 접수 (비인증)
  http.post(`${BASE}/inquiries`, async ({ request }) => {
    const body = (await request.json()) as {
      customerInfo?: Record<string, unknown>;
      customerType?: string;
      content?: string;
    };
    if (!body.content || body.content.trim().length < 10) {
      return err("VALIDATION_ERROR", "문의 내용은 최소 10자 이상이어야 합니다.", 400);
    }
    if (!body.customerType) {
      return err("VALIDATION_ERROR", "문의 유형은 필수입니다.", 400);
    }
    const id = nextInquiryId();
    const createdAt = nowKst();
    inquiries[id] = {
      inquiry: {
        inquiryId: id,
        customerInfo: body.customerInfo ?? {},
        customerType: body.customerType as never,
        content: body.content,
        status: "RECEIVED",
        createdAt,
      },
      analysis: null,
      diagnosis: null,
      currentDraft: null,
      history: [{ action: "ASSIGN", operator: "system", timestamp: createdAt }],
    };
    return ok({ inquiryId: id, status: "RECEIVED", createdAt }, { status: 201 });
  }),

  // 문의 상세
  http.get(`${BASE}/inquiries/:id`, ({ params }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    return ok(s);
  }),

  // Pull 배정 (미배정 1건 자동)
  http.post(`${BASE}/operator/inquiries/pull`, () => {
    const target = Object.values(inquiries).find(
      (s) => s.inquiry.status === "PENDING_ASSIGNMENT",
    );
    if (!target) return new HttpResponse(null, { status: 204 });
    target.inquiry.status = "OPERATOR_REVIEWING";
    target.inquiry.assignedOperator = "operator1";
    target.history.push({ action: "ASSIGN", operator: "operator1", timestamp: nowKst() });
    return ok(target);
  }),

  // 초안 수정
  http.patch(`${BASE}/operator/inquiries/:id/draft`, async ({ params, request }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    const body = (await request.json()) as { content?: string };
    if (!s.currentDraft) return err("VALIDATION_ERROR", "초안이 없습니다.", 400);
    s.currentDraft.content = body.content ?? s.currentDraft.content;
    s.currentDraft.status = "EDITED";
    s.history.push({ action: "EDIT", operator: "operator1", timestamp: nowKst() });
    return ok({
      draftId: s.currentDraft.draftId,
      content: s.currentDraft.content,
      status: "EDITED",
    });
  }),

  // 승인
  http.post(`${BASE}/operator/inquiries/:id/approve`, async ({ params, request }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    if (s.inquiry.status !== "OPERATOR_REVIEWING") {
      return err("INVALID_STATE_TRANSITION", "승인할 수 없는 상태입니다.", 409);
    }
    const body = (await request.json().catch(() => ({}))) as { content?: string };
    if (body.content && s.currentDraft) {
      s.currentDraft.content = body.content;
      s.currentDraft.status = "EDITED";
      s.history.push({ action: "EDIT", operator: "operator1", timestamp: nowKst() });
    }
    s.inquiry.status = "APPROVED";
    if (s.currentDraft) s.currentDraft.status = "APPROVED";
    s.history.push({ action: "APPROVE", operator: "operator1", timestamp: nowKst() });
    // 데모: 승인 후 자동 발송
    s.inquiry.status = "SENT";
    return ok({ inquiryId: s.inquiry.inquiryId, status: "APPROVED" });
  }),

  // 반려 → 재생성
  http.post(`${BASE}/operator/inquiries/:id/reject`, async ({ params, request }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    const body = (await request.json()) as { reason?: string };
    if (!body.reason || !body.reason.trim()) {
      return err("REASON_REQUIRED", "반려 사유는 필수입니다.", 400);
    }
    if (!s.currentDraft) return err("VALIDATION_ERROR", "초안이 없습니다.", 400);
    if ((s.currentDraft.regenerationCount ?? 0) >= REGENERATION_LIMIT) {
      return err("REGENERATION_LIMIT_EXCEEDED", "재생성 한도(3회)를 초과했습니다.", 409);
    }
    const newCount = (s.currentDraft.regenerationCount ?? 0) + 1;
    const draftId = nextDraftId();
    s.currentDraft = {
      draftId,
      content: `[재생성 ${newCount}회차] 고객님의 문의를 다시 검토하여 답변을 보강했습니다. ${s.inquiry.content.slice(0, 20)}... 에 대해 정확히 안내드리겠습니다.`,
      status: "GENERATED",
      regenerationCount: newCount,
    };
    s.history.push({ action: "REJECT", operator: "operator1", reason: body.reason, timestamp: nowKst() });
    s.history.push({ action: "REGENERATE", operator: "system", timestamp: nowKst() });
    return ok({
      inquiryId: s.inquiry.inquiryId,
      newDraft: { draftId, content: s.currentDraft.content, regenerationCount: newCount },
    });
  }),

  // 수동 재분석
  http.post(`${BASE}/operator/inquiries/:id/reanalyze`, async ({ params, request }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    const body = (await request.json().catch(() => ({}))) as { reason?: string };
    s.inquiry.status = "AI_ANALYZING";
    s.history.push({ action: "REANALYZE", operator: "operator1", reason: body.reason, timestamp: nowKst() });
    return ok({ inquiryId: s.inquiry.inquiryId, status: "AI_ANALYZING" });
  }),

  // 처리 이력
  http.get(`${BASE}/operator/inquiries/:id/history`, ({ params }) => {
    const s = inquiries[params.id as string];
    if (!s) return err("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다.", 404);
    return ok(s.history);
  }),

  // 칸반 보드
  http.get(`${BASE}/dashboard/board`, ({ request }) => {
    const url = new URL(request.url);
    const type = url.searchParams.get("type");
    const urgency = url.searchParams.get("urgency");
    const keyword = url.searchParams.get("keyword");
    const board: KanbanBoard = {
      RECEIVED: [],
      AI_ANALYZING: [],
      PENDING_ASSIGNMENT: [],
      OPERATOR_REVIEWING: [],
      APPROVED: [],
      SENT: [],
      MANUAL_CLASSIFICATION_PENDING: [],
    };
    Object.values(inquiries).forEach((s) => {
      const card = toCard(s);
      if (type && card.customerType !== type && card.aiType !== type) return;
      if (urgency && card.urgency !== urgency) return;
      if (
        keyword &&
        !card.inquiryId.toLowerCase().includes(keyword.toLowerCase()) &&
        !(card.summary ?? "").toLowerCase().includes(keyword.toLowerCase())
      )
        return;
      (board[card.status as InquiryStatus] ??= []).push(card);
    });
    return ok(board);
  }),

  // 목록 (페이징)
  http.get(`${BASE}/dashboard/inquiries`, ({ request }) => {
    const url = new URL(request.url);
    const type = url.searchParams.get("type");
    const urgency = url.searchParams.get("urgency");
    const keyword = url.searchParams.get("keyword");
    const status = url.searchParams.get("status");
    const assignee = url.searchParams.get("assignee");
    let cards = Object.values(inquiries).map(toCard);
    if (type) cards = cards.filter((c) => c.customerType === type || c.aiType === type);
    if (urgency) cards = cards.filter((c) => c.urgency === urgency);
    if (status) cards = cards.filter((c) => c.status === status);
    if (assignee)
      cards = cards.filter((c) =>
        (c.assignedOperator ?? "").toLowerCase().includes(assignee.toLowerCase()),
      );
    if (keyword)
      cards = cards.filter(
        (c) =>
          c.inquiryId.toLowerCase().includes(keyword.toLowerCase()) ||
          (c.summary ?? "").toLowerCase().includes(keyword.toLowerCase()),
      );
    const page = Number(url.searchParams.get("page") ?? 0);
    const size = Number(url.searchParams.get("size") ?? 20);
    const start = page * size;
    return HttpResponse.json({
      data: cards.slice(start, start + size),
      page,
      size,
      totalElements: cards.length,
      totalPages: Math.max(1, Math.ceil(cards.length / size)),
      timestamp: nowKst(),
    });
  }),

  // 알림 카운트
  http.get(`${BASE}/dashboard/notifications`, () => {
    const all = Object.values(inquiries);
    const unassignedCount = all.filter(
      (s) => s.inquiry.status === "PENDING_ASSIGNMENT",
    ).length;
    const urgentCount = all.filter((s) => s.analysis?.urgency === "HIGH").length;
    return ok({ unassignedCount, urgentCount });
  }),
];
