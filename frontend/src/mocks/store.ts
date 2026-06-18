// MSW 목업용 인메모리 데이터 스토어 — 01-api-contract 기준 샘플 데이터.
import type { InquiryDetail } from "../common/apiTypes";

export interface StoredInquiry extends InquiryDetail {}

function nowKst(offsetMin = 0): string {
  const d = new Date(Date.now() - offsetMin * 60000);
  return d.toISOString();
}

// 데모용 샘플 문의들 (다양한 상태 커버)
export const inquiries: Record<string, StoredInquiry> = {
  "INQ-1001": {
    inquiry: {
      inquiryId: "INQ-1001",
      customerInfo: { userId: "user01", nickname: "홍길동", channel: "WEB" },
      customerType: "PAYMENT",
      content: "결제했는데 아이템이 지급되지 않았어요. 환불 가능한가요?",
      status: "PENDING_ASSIGNMENT",
      createdAt: nowKst(15),
    },
    analysis: {
      aiType: "PAYMENT",
      subCategory: "결제후미지급",
      urgency: "HIGH",
      summary: "결제 완료 후 아이템 미지급 — 환불 문의",
      keywords: ["결제", "미지급", "환불"],
      systemQueryResult: { orderId: "ORD-555", paid: true, delivered: false },
      failureType: null,
    },
    diagnosis: {
      cause: "지급 배치 지연으로 인한 일시적 미지급",
      suggestedDirection: "재지급 처리 안내 또는 환불 절차 안내",
      confidence: 0.82,
    },
    currentDraft: {
      draftId: "DRF-1001",
      content:
        "안녕하세요, 고객님. 결제 후 아이템이 지급되지 않아 불편을 드려 죄송합니다. 확인 결과 일시적인 지급 지연으로 보이며, 즉시 재지급 처리해 드리겠습니다. 잠시만 기다려 주세요.",
      status: "GENERATED",
      regenerationCount: 0,
    },
    history: [
      { action: "ASSIGN", operator: "system", timestamp: nowKst(14) },
    ],
  },
  "INQ-1002": {
    inquiry: {
      inquiryId: "INQ-1002",
      customerInfo: { userId: "user02", nickname: "김철수", channel: "APP" },
      customerType: "ACCOUNT",
      content: "계정 로그인이 안 됩니다. 비밀번호 재설정도 메일이 안 와요.",
      status: "OPERATOR_REVIEWING",
      createdAt: nowKst(40),
      assignedOperator: "operator1",
    },
    analysis: {
      aiType: "ACCOUNT",
      subCategory: "로그인불가",
      urgency: "NORMAL",
      summary: "로그인 불가 + 비밀번호 재설정 메일 미수신",
      keywords: ["로그인", "비밀번호", "메일"],
      failureType: null,
    },
    diagnosis: {
      cause: "메일 발송 큐 지연 또는 스팸 처리 가능성",
      suggestedDirection: "스팸함 확인 안내 및 수동 재설정 링크 발송",
      confidence: 0.74,
    },
    currentDraft: {
      draftId: "DRF-1002",
      content:
        "안녕하세요. 비밀번호 재설정 메일이 스팸함으로 분류되었을 수 있습니다. 스팸함을 확인해 주시고, 그래도 없으시면 수동으로 재설정 링크를 보내드리겠습니다.",
      status: "GENERATED",
      regenerationCount: 1,
    },
    history: [
      { action: "ASSIGN", operator: "operator1", timestamp: nowKst(38) },
      { action: "REJECT", operator: "operator1", reason: "톤이 너무 딱딱함", timestamp: nowKst(30) },
      { action: "REGENERATE", operator: "system", timestamp: nowKst(29) },
    ],
  },
  "INQ-1003": {
    inquiry: {
      inquiryId: "INQ-1003",
      customerInfo: { userId: "user03", nickname: "이영희", channel: "WEB" },
      customerType: "ITEM_DELIVERY",
      content: "이벤트 보상 아이템을 아직 못 받았습니다. 언제 지급되나요?",
      status: "SENT",
      createdAt: nowKst(120),
      assignedOperator: "operator2",
    },
    analysis: {
      aiType: "ITEM_DELIVERY",
      subCategory: "이벤트보상",
      urgency: "LOW",
      summary: "이벤트 보상 아이템 지급 일정 문의",
      keywords: ["이벤트", "보상", "지급"],
      failureType: null,
    },
    diagnosis: {
      cause: "이벤트 보상 일괄 지급 일정 전",
      suggestedDirection: "지급 예정일 안내",
      confidence: 0.9,
    },
    currentDraft: {
      draftId: "DRF-1003",
      content:
        "안녕하세요. 이벤트 보상은 종료 후 3일 이내 일괄 지급됩니다. 곧 지급될 예정이니 조금만 기다려 주세요. 감사합니다.",
      status: "APPROVED",
      regenerationCount: 0,
    },
    history: [
      { action: "ASSIGN", operator: "operator2", timestamp: nowKst(115) },
      { action: "APPROVE", operator: "operator2", timestamp: nowKst(110) },
    ],
  },
  "INQ-1004": {
    inquiry: {
      inquiryId: "INQ-1004",
      customerInfo: { userId: "user04", nickname: "박민수", channel: "APP" },
      customerType: "ETC",
      content: "이거 뭔가 이상한데 잘 모르겠어요 도와주세요 분류가 애매합니다.",
      status: "MANUAL_CLASSIFICATION_PENDING",
      createdAt: nowKst(8),
    },
    analysis: {
      aiType: "ETC",
      urgency: "NORMAL",
      summary: "분류 불명확 — 수동 분류 필요",
      keywords: [],
      failureType: "API_ERROR",
    },
    diagnosis: null,
    currentDraft: null,
    history: [
      { action: "REANALYZE", operator: "system", reason: "자동 분류 신뢰도 낮음", timestamp: nowKst(7) },
    ],
  },
};

let seq = 2000;
export function nextInquiryId(): string {
  seq += 1;
  return `INQ-${seq}`;
}

let draftSeq = 2000;
export function nextDraftId(): string {
  draftSeq += 1;
  return `DRF-${draftSeq}`;
}

export { nowKst };
