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
  return (
    <div
      className="card"
      data-testid={`inquiry-card-${card.inquiryId}`}
      onClick={() => onClick(card.inquiryId)}
      style={{
        marginBottom: 10,
        cursor: "pointer",
        borderLeft: isUrgent ? "4px solid var(--urgent-border)" : undefined,
        background: isUrgent ? "var(--urgent-bg)" : undefined,
        padding: 12,
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
        <span style={{ fontWeight: 700, fontSize: 12, color: "var(--color-muted)" }}>
          {card.inquiryId}
        </span>
        <UrgencyBadge urgency={card.urgency} />
      </div>
      <div style={{ fontSize: 13, marginBottom: 8, lineHeight: 1.4 }}>
        {card.summary ?? "(요약 없음)"}
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
