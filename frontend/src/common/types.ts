// 공유 타입 정의 — 02-shared-contracts 기준. 백엔드 enum과 1:1 일치.
// 변경 시 백엔드 02 문서와 동기화 필수.

export type InquiryStatus =
  | "RECEIVED"
  | "AI_ANALYZING"
  | "PENDING_ASSIGNMENT"
  | "OPERATOR_REVIEWING"
  | "APPROVED"
  | "SENT"
  | "MANUAL_CLASSIFICATION_PENDING";

export type InquiryType = "PAYMENT" | "ITEM_DELIVERY" | "ACCOUNT" | "ETC";

export type Urgency = "HIGH" | "NORMAL" | "LOW";

export type ApprovalAction =
  | "APPROVE"
  | "REJECT"
  | "EDIT"
  | "REGENERATE"
  | "ASSIGN"
  | "REANALYZE";

export type DraftResponseStatus = "GENERATED" | "EDITED" | "REJECTED" | "APPROVED";

export type FailureType = "TIMEOUT" | "API_ERROR" | null;

export type OperatorRole = "OPERATOR" | "ADMIN";

// 표시용 한글 라벨
export const STATUS_LABEL: Record<InquiryStatus, string> = {
  RECEIVED: "접수",
  AI_ANALYZING: "AI분석중",
  PENDING_ASSIGNMENT: "담당자배정대기",
  OPERATOR_REVIEWING: "운영자확인중",
  APPROVED: "승인완료",
  SENT: "발송완료",
  MANUAL_CLASSIFICATION_PENDING: "수동분류대기",
};

export const TYPE_LABEL: Record<InquiryType, string> = {
  PAYMENT: "결제",
  ITEM_DELIVERY: "아이템지급",
  ACCOUNT: "계정",
  ETC: "기타",
};

export const URGENCY_LABEL: Record<Urgency, string> = {
  HIGH: "긴급",
  NORMAL: "보통",
  LOW: "낮음",
};

// API 응답 래퍼
export interface ApiResponse<T> {
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  error: { code: string; message: string };
  timestamp: string;
}

// 도메인 DTO (01-api-contract §6 InquiryDetail)
export interface InquiryCard {
  inquiryId: string;
  customerType: InquiryType;
  aiType?: InquiryType;
  urgency: Urgency;
  summary?: string;
  status: InquiryStatus;
  assignedOperator?: string;
  createdAt: string;
}

export interface InquiryDetail {
  inquiry: {
    inquiryId: string;
    customerInfo: Record<string, unknown>;
    customerType: InquiryType;
    content: string;
    status: InquiryStatus;
    createdAt: string;
    assignedOperator?: string;
  };
  analysis: {
    aiType: InquiryType;
    subCategory?: string;
    urgency: Urgency;
    summary?: string;
    keywords?: string[];
    systemQueryResult?: Record<string, unknown>;
    failureType: FailureType;
  } | null;
  diagnosis: {
    cause: string;
    suggestedDirection: string;
    confidence: number;
  } | null;
  currentDraft: {
    draftId: string;
    content: string;
    status: DraftResponseStatus;
    regenerationCount: number;
  } | null;
  history: Array<{
    action: ApprovalAction;
    operator: string;
    reason?: string;
    timestamp: string;
  }>;
}
