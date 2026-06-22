import type { InquiryCard } from "../common/types";
import { TYPE_LABEL } from "../common/types";
import { UrgencyBadge } from "../common/UrgencyBadge";

interface Props {
  card: InquiryCard;
  onClick: (inquiryId: string) => void;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("ko-KR", {
      timeZone: "Asia/Seoul",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export function InquiryCardItem({ card, onClick }: Props) {
  const isUrgent = card.urgency === "HIGH";
  const title =
    card.summary?.trim() ||
    (card.content ? card.content.trim().slice(0, 60) : "");
  const isPending = !title;
  return (
    <div
      className="card kanban-card"
      data-testid={`inquiry-card-${card.inquiryId}`}
      onClick={() => onClick(card.inquiryId)}
      style={{
        borderLeft: isUrgent ? "3px solid var(--urgent-border)" : undefined,
        background: isUrgent ? "var(--urgent-bg)" : undefined,
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 6,
        }}
      >
        <span style={{ fontWeight: 700, fontSize: 11, color: "var(--color-muted)", fontFamily: "ui-monospace, monospace" }}>
          {card.inquiryId.slice(0, 8)}
        </span>
        <UrgencyBadge urgency={card.urgency} />
      </div>
      <div
        style={{
          fontSize: 13,
          marginBottom: 10,
          lineHeight: 1.45,
          color: isPending ? "var(--color-muted)" : "var(--color-text)",
          fontStyle: isPending ? "italic" : "normal",
        }}
      >
        {isPending ? "⏳ AI 분석 대기 중" : title}
      </div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontSize: 11,
          color: "var(--color-muted)",
        }}
      >
        <span>
          {TYPE_LABEL[card.aiType ?? card.customerType]}
          {card.assignedOperator ? ` · ${card.assignedOperator}` : ""}
        </span>
        <span>{formatTime(card.createdAt)}</span>
      </div>
    </div>
  );
}
