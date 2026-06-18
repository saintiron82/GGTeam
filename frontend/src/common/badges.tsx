// GRAVITY 공통 뱃지/칩 컴포넌트 — wireframe-kit.tsx 기준.
// 닷(dot) 없이 통일(G2). 색상은 styles.css의 .g-badge--* 클래스로 관리.
import type { ReactNode } from "react";
import type { InquiryType, InquiryStatus, Urgency } from "./types";
import { TYPE_LABEL, STATUS_LABEL, URGENCY_LABEL } from "./types";

export type BadgeColor =
  | "default"
  | "info"
  | "success"
  | "warning"
  | "error"
  | "accent";

export function Badge({
  children,
  color = "default",
  testid,
}: {
  children: ReactNode;
  color?: BadgeColor;
  testid?: string;
}) {
  return (
    <span className={`g-badge g-badge--${color}`} data-testid={testid}>
      {children}
    </span>
  );
}

// 문의 유형 색상 매핑(L2): 결제=info / 아이템지급=accent / 계정=warning / 기타=default
const TYPE_COLOR: Record<InquiryType, BadgeColor> = {
  PAYMENT: "info",
  ITEM_DELIVERY: "accent",
  ACCOUNT: "warning",
  ETC: "default",
};

export function TypeBadge({ type }: { type: InquiryType }) {
  return (
    <Badge color={TYPE_COLOR[type]} testid={`type-${type}`}>
      {TYPE_LABEL[type]}
    </Badge>
  );
}

// 긴급도(HIGH=error / NORMAL=warning / LOW=default)
const URGENCY_COLOR: Record<Urgency, BadgeColor> = {
  HIGH: "error",
  NORMAL: "warning",
  LOW: "default",
};

export function UrgencyBadge({ urgency }: { urgency: Urgency }) {
  return (
    <Badge color={URGENCY_COLOR[urgency]} testid={`urgency-${urgency}`}>
      {URGENCY_LABEL[urgency]}
    </Badge>
  );
}

// 상태 칩(L3): 진행중=info / 완료=success / 대기=warning / 자동처리(접수·분석)=default
const STATUS_COLOR: Record<InquiryStatus, BadgeColor> = {
  RECEIVED: "default",
  AI_ANALYZING: "default",
  PENDING_ASSIGNMENT: "warning",
  OPERATOR_REVIEWING: "info",
  APPROVED: "success",
  SENT: "success",
  MANUAL_CLASSIFICATION_PENDING: "warning",
};

export function StatusBadge({ status }: { status: InquiryStatus }) {
  return (
    <Badge color={STATUS_COLOR[status]} testid={`status-${status}`}>
      {STATUS_LABEL[status]}
    </Badge>
  );
}
