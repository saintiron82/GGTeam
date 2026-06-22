// API 호출 함수 모음 — 01-api-contract 기준. ApiResponse 래퍼를 벗겨 반환.
import { apiClient } from "./apiClient";
import type { ApiResponse } from "./types";
import type { DashboardStats } from "./types";
import type {
  LoginRequest,
  TokenResponse,
  InquiryCreateRequest,
  InquiryCreateResponse,
  InquiryDetail,
  DraftResponse,
  ApprovalResult,
  RejectResult,
  KanbanBoard,
  NotificationSummary,
  DashboardFilter,
  PagedInquiries,
} from "./apiTypes";

function unwrap<T>(res: { data: ApiResponse<T> }): T {
  return res.data.data;
}

// 인증
export async function login(req: LoginRequest): Promise<TokenResponse> {
  const res = await apiClient.post<ApiResponse<TokenResponse>>("/auth/login", req);
  return unwrap(res);
}
export async function logout(): Promise<void> {
  await apiClient.post("/auth/logout");
}

// 고객 문의 접수 (비인증)
export async function createInquiry(
  req: InquiryCreateRequest,
): Promise<InquiryCreateResponse> {
  const res = await apiClient.post<ApiResponse<InquiryCreateResponse>>(
    "/inquiries",
    req,
  );
  return unwrap(res);
}

// 문의 상세
export async function fetchDetail(inquiryId: string): Promise<InquiryDetail> {
  const res = await apiClient.get<ApiResponse<InquiryDetail>>(
    `/inquiries/${inquiryId}`,
  );
  return unwrap(res);
}

// Pull 배정 (미배정 1건 자동) — 204면 null
export async function pullInquiry(): Promise<InquiryDetail | null> {
  const res = await apiClient.post<ApiResponse<InquiryDetail> | "">(
    "/operator/inquiries/pull",
  );
  if (res.status === 204 || !res.data) return null;
  return (res.data as ApiResponse<InquiryDetail>).data;
}

// 초안 수정
export async function saveDraft(
  inquiryId: string,
  content: string,
): Promise<DraftResponse> {
  const res = await apiClient.patch<ApiResponse<DraftResponse>>(
    `/operator/inquiries/${inquiryId}/draft`,
    { content },
  );
  return unwrap(res);
}

// 승인 (content 있으면 수정 후 승인)
export async function approve(
  inquiryId: string,
  content?: string,
): Promise<ApprovalResult> {
  const res = await apiClient.post<ApiResponse<ApprovalResult>>(
    `/operator/inquiries/${inquiryId}/approve`,
    content !== undefined ? { content } : {},
  );
  return unwrap(res);
}

// 반려 → 재생성 (사유 필수)
export async function reject(
  inquiryId: string,
  reason: string,
): Promise<RejectResult> {
  const res = await apiClient.post<ApiResponse<RejectResult>>(
    `/operator/inquiries/${inquiryId}/reject`,
    { reason },
  );
  return unwrap(res);
}

// 수동 재분석
export async function reanalyze(
  inquiryId: string,
  reason?: string,
): Promise<ApprovalResult> {
  const res = await apiClient.post<ApiResponse<ApprovalResult>>(
    `/operator/inquiries/${inquiryId}/reanalyze`,
    reason ? { reason } : {},
  );
  return unwrap(res);
}

// 칸반 보드
export async function fetchBoard(filter: DashboardFilter = {}): Promise<KanbanBoard> {
  const res = await apiClient.get<ApiResponse<KanbanBoard>>("/dashboard/board", {
    params: filter,
  });
  return unwrap(res);
}

// 목록 (필터/검색/페이징) — 페이지네이션 메타 포함이라 통째로 반환
export async function fetchInquiries(
  filter: DashboardFilter = {},
): Promise<PagedInquiries> {
  const res = await apiClient.get<PagedInquiries>("/dashboard/inquiries", {
    params: filter,
  });
  return res.data;
}

// 알림 카운트
export async function fetchNotifications(): Promise<NotificationSummary> {
  const res = await apiClient.get<ApiResponse<NotificationSummary>>(
    "/dashboard/notifications",
  );
  return unwrap(res);
}

// 통계 (US-27)
export async function fetchStats(): Promise<DashboardStats> {
  const res = await apiClient.get<ApiResponse<DashboardStats>>("/dashboard/stats");
  return unwrap(res);
}

// 에러 메시지 추출 헬퍼
export function extractErrorMessage(err: unknown, fallback = "오류가 발생했습니다."): string {
  const e = err as { response?: { data?: { error?: { message?: string } } } };
  return e?.response?.data?.error?.message ?? fallback;
}
