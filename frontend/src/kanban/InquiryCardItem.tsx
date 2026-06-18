import type { InquiryCard } from "../common/types";
import { TypeBadge, UrgencyBadge } from "../common/badges";

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
      className="g-row--clickable"
      data-testid={`inquiry-card-${card.inquiryId}`}
      onClick={() => onClick(card.inquiryId)}
      style={{
        background: "var(--color-surface)",
        border: `1px solid ${isUrgent ? "var(--urgent-border)" : "var(--color-border)"}`,
        borderLeft: isUrgent ? "3px solid var(--urgent-border)" : "1px solid var(--color-border)",
        borderRadius: "var(--radius)",
        boxShadow: "0 1px 2px rgba(0,0,0,.05)",
        padding: 10,
        marginBottom: 8,
        fontSize: 12,
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
        <span className="mono" style={{ color: "var(--color-muted)", fontSize: 11 }}>
          {card.inquiryId}
        </span>
        <UrgencyBadge urgency={card.urgency} />
      </div>
      <div style={{ marginBottom: 6 }}>
        <TypeBadge type={card.aiType ?? card.customerType} />
      </div>
      <div style={{ color: "var(--color-text)", lineHeight: 1.5, marginBottom: 6 }}>
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
        <span>{card.assignedOperator ? `담당: ${card.assignedOperator}` : "미배정"}</span>
        <span className="mono">{formatTime(card.createdAt)}</span>
      </div>
    </div>
  );
}
