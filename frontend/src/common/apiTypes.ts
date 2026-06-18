// API 요청/응답 타입 — 01-api-contract 기준. types.ts(도메인 DTO)를 보완.
import type {
  InquiryType,
  Urgency,
  InquiryStatus,
  InquiryCard,
  InquiryDetail,
  DraftResponseStatus,
  OperatorRole,
} from "./types";

// 로그인
export interface LoginRequest {
  username: string;
  password: string;
}
export interface TokenResponse {
  token: string;
  operator: { id: string; username: string; role: OperatorRole };
  expiresAt: string;
}

// 문의 접수
export interface InquiryCreateRequest {
  customerInfo: { userId: string; nickname: string; channel: string };
  customerType: InquiryType;
  content: string;
}
export interface InquiryCreateResponse {
  inquiryId: string;
  status: InquiryStatus;
  createdAt: string;
}

// 초안 수정/승인/반려
export interface DraftResponse {
  draftId: string;
  content: string;
  status: DraftResponseStatus;
  regenerationCount?: number;
}
export interface ApprovalResult {
  inquiryId: string;
  status: InquiryStatus;
}
export interface RejectResult {
  inquiryId: string;
  newDraft: { draftId: string; content: string; regenerationCount: number };
}

// 대시보드
export type KanbanBoard = Record<InquiryStatus, InquiryCard[]>;
export interface NotificationSummary {
  unassignedCount: number;
  urgentCount: number;
}
export interface DashboardFilter {
  status?: InquiryStatus;
  urgency?: Urgency;
  type?: InquiryType;
  assignee?: string;
  keyword?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
export interface PagedInquiries {
  data: InquiryCard[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type { InquiryDetail };
