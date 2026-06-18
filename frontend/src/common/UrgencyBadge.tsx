import type { Urgency } from "./types";
import { URGENCY_LABEL } from "./types";

const CLASS: Record<Urgency, string> = {
  HIGH: "badge badge-urgent",
  NORMAL: "badge badge-normal",
  LOW: "badge badge-low",
};

export function UrgencyBadge({ urgency }: { urgency: Urgency }) {
  return (
    <span className={CLASS[urgency]} data-testid={`urgency-${urgency}`}>
      {URGENCY_LABEL[urgency]}
    </span>
  );
}
