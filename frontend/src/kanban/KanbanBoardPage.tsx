import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  fetchBoard,
  fetchNotifications,
  fetchInquiries,
  extractErrorMessage,
} from "../common/api";
import type {
  KanbanBoard,
  NotificationSummary,
  DashboardFilter,
  PagedInquiries,
} from "../common/apiTypes";
import type { InquiryStatus, InquiryType, Urgency } from "../common/types";
import { STATUS_LABEL, TYPE_LABEL, URGENCY_LABEL } from "../common/types";
import { AppLayout } from "../common/AppLayout";
import { InquiryCardItem } from "./InquiryCardItem";
import { Badge, TypeBadge, StatusBadge, UrgencyBadge } from "../common/badges";

// K1: RECEIVED + AI_ANALYZING은 "접수·분석중" 한 컬럼으로 표시 그룹핑 (백엔드 enum은 불변,
// 프론트가 board 응답의 두 그룹을 합쳐 렌더링). 담당자배정대기부터는 분리 유지 → 5컬럼.
const COLUMNS: { key: string; label: string; states: InquiryStatus[] }[] = [
  { key: "INTAKE", label: "접수·분석중", states: ["RECEIVED", "AI_ANALYZING"] },
  { key: "PENDING_ASSIGNMENT", label: "담당자배정대기", states: ["PENDING_ASSIGNMENT"] },
  { key: "OPERATOR_REVIEWING", label: "운영자확인중", states: ["OPERATOR_REVIEWING"] },
  { key: "APPROVED", label: "승인완료", states: ["APPROVED"] },
  { key: "SENT", label: "발송완료", states: ["SENT"] },
];

const STATUS_OPTIONS: InquiryStatus[] = [
  "RECEIVED",
  "AI_ANALYZING",
  "PENDING_ASSIGNMENT",
  "OPERATOR_REVIEWING",
  "APPROVED",
  "SENT",
  "MANUAL_CLASSIFICATION_PENDING",
];
const TYPE_OPTIONS: InquiryType[] = ["PAYMENT", "ITEM_DELIVERY", "ACCOUNT", "ETC"];
const URGENCY_OPTIONS: Urgency[] = ["HIGH", "NORMAL", "LOW"];
const PAGE_SIZE = 20;

type ViewMode = "kanban" | "list";

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

export function KanbanBoardPage() {
  const navigate = useNavigate();
  const [view, setView] = useState<ViewMode>("kanban");
  const [board, setBoard] = useState<KanbanBoard | null>(null);
  const [list, setList] = useState<PagedInquiries | null>(null);
  const [notifications, setNotifications] = useState<NotificationSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 필터 상태
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<InquiryStatus | "">("");
  const [typeFilter, setTypeFilter] = useState<InquiryType | "">("");
  const [urgencyFilter, setUrgencyFilter] = useState<Urgency | "">("");
  const [assigneeFilter, setAssigneeFilter] = useState("");
  const [appliedFilter, setAppliedFilter] = useState<DashboardFilter>({});
  const [page, setPage] = useState(0);

  const onCardClick = useCallback(
    (inquiryId: string) => navigate(`/inquiry/${inquiryId}`),
    [navigate],
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const notif = await fetchNotifications();
      setNotifications(notif);
      if (view === "kanban") {
        setBoard(await fetchBoard(appliedFilter));
      } else {
        setList(await fetchInquiries({ ...appliedFilter, page, size: PAGE_SIZE }));
      }
    } catch (err) {
      setError(extractErrorMessage(err, "보드를 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }, [view, appliedFilter, page]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  function applyFilters() {
    setPage(0);
    setAppliedFilter({
      keyword: keyword.trim() || undefined,
      status: statusFilter || undefined,
      type: typeFilter || undefined,
      urgency: urgencyFilter || undefined,
      assignee: assigneeFilter.trim() || undefined,
    });
  }

  function resetFilters() {
    setKeyword("");
    setStatusFilter("");
    setTypeFilter("");
    setUrgencyFilter("");
    setAssigneeFilter("");
    setPage(0);
    setAppliedFilter({});
  }

  // 컬럼별 카드 집계 (INTAKE는 두 상태 병합)
  function cardsFor(states: InquiryStatus[]) {
    if (!board) return [];
    return states.flatMap((s) => board[s] ?? []);
  }

  return (
    <AppLayout>
      <div className="page">
        {/* 상단 바: 뷰 토글 + 알림 칩 */}
        <div
          style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 16 }}
          data-testid="notification-bar"
        >
          <div className="seg">
            <button
              className={view === "kanban" ? "active" : ""}
              onClick={() => setView("kanban")}
              data-testid="view-kanban"
            >
              칸반
            </button>
            <button
              className={view === "list" ? "active" : ""}
              onClick={() => setView("list")}
              data-testid="view-list"
            >
              리스트
            </button>
          </div>
          <Badge color="error" testid="notif-unassigned">
            미배정 {notifications?.unassignedCount ?? 0}건
          </Badge>
          <Badge color="warning" testid="notif-urgent">
            긴급 {notifications?.urgentCount ?? 0}건
          </Badge>
        </div>

        {/* 필터/검색 바 */}
        <div
          className="card filter-bar"
          style={{ marginBottom: 16 }}
          data-testid="filter-bar"
        >
          <div className="filter-item">
            <label htmlFor="f-status">상태</label>
            <select
              id="f-status"
              data-testid="filter-status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as InquiryStatus)}
            >
              <option value="">전체</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-item">
            <label htmlFor="f-urgency">긴급도</label>
            <select
              id="f-urgency"
              data-testid="filter-urgency"
              value={urgencyFilter}
              onChange={(e) => setUrgencyFilter(e.target.value as Urgency)}
            >
              <option value="">전체</option>
              {URGENCY_OPTIONS.map((u) => (
                <option key={u} value={u}>
                  {URGENCY_LABEL[u]}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-item">
            <label htmlFor="f-type">유형</label>
            <select
              id="f-type"
              data-testid="filter-type"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as InquiryType)}
            >
              <option value="">전체</option>
              {TYPE_OPTIONS.map((t) => (
                <option key={t} value={t}>
                  {TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-item">
            <label htmlFor="f-assignee">담당자</label>
            <input
              id="f-assignee"
              data-testid="filter-assignee"
              value={assigneeFilter}
              placeholder="전체"
              onChange={(e) => setAssigneeFilter(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && applyFilters()}
            />
          </div>
          <div className="filter-item" style={{ flex: 1, minWidth: 180 }}>
            <label htmlFor="keyword">검색</label>
            <input
              id="keyword"
              data-testid="filter-keyword"
              value={keyword}
              placeholder="🔍 문의 ID·내용 검색"
              style={{ width: "100%" }}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && applyFilters()}
            />
          </div>
          <button className="primary" onClick={applyFilters} data-testid="filter-apply">
            적용
          </button>
          <button className="ghost" onClick={resetFilters} data-testid="filter-reset">
            초기화
          </button>
        </div>

        {error && (
          <div className="error-msg" data-testid="board-error" role="alert">
            {error}
          </div>
        )}
        {loading && <div data-testid="board-loading">불러오는 중...</div>}

        {/* 칸반 뷰 (5컬럼) */}
        {!loading && view === "kanban" && board && (
          <div className="kanban" data-testid="kanban-board">
            {COLUMNS.map((col) => {
              const cards = cardsFor(col.states);
              return (
                <div key={col.key} className="kanban-col" data-testid={`column-${col.key}`}>
                  <div className="kanban-col-head">
                    <span>{col.label}</span>
                    <span style={{ color: "var(--color-muted)" }}>{cards.length}</span>
                  </div>
                  <div className="kanban-col-body">
                    {cards.map((card) => (
                      <InquiryCardItem
                        key={card.inquiryId}
                        card={card}
                        onClick={onCardClick}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* 리스트 뷰 */}
        {!loading && view === "list" && list && (
          <div className="card" data-testid="list-view">
            <table className="g-table">
              <thead>
                <tr>
                  <th>문의 ID</th>
                  <th>유형 (고객 → AI)</th>
                  <th>긴급도</th>
                  <th>상태</th>
                  <th>담당자</th>
                  <th>요약</th>
                  <th>접수시각</th>
                  <th>완료시각</th>
                </tr>
              </thead>
              <tbody>
                {list.data.map((card) => (
                  <tr
                    key={card.inquiryId}
                    className="g-row--clickable"
                    data-testid={`list-row-${card.inquiryId}`}
                    onClick={() => onCardClick(card.inquiryId)}
                  >
                    <td
                      className="mono"
                      style={{ color: "var(--color-primary)", fontWeight: 600 }}
                    >
                      {card.inquiryId}
                    </td>
                    <td>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                        <span style={{ color: "var(--color-muted)" }}>
                          {TYPE_LABEL[card.customerType]}
                        </span>
                        <span style={{ color: "var(--color-muted)" }}>→</span>
                        <TypeBadge type={card.aiType ?? card.customerType} />
                      </span>
                    </td>
                    <td>
                      <UrgencyBadge urgency={card.urgency} />
                    </td>
                    <td>
                      <StatusBadge status={card.status} />
                    </td>
                    <td style={{ color: card.assignedOperator ? undefined : "var(--color-muted)" }}>
                      {card.assignedOperator ?? "-"}
                    </td>
                    <td style={{ color: "var(--color-muted)" }}>{card.summary ?? "-"}</td>
                    <td className="mono" style={{ color: "var(--color-muted)" }}>
                      {formatTime(card.createdAt)}
                    </td>
                    <td
                      className="mono"
                      style={{ color: card.completedAt ? "var(--color-text)" : "var(--color-muted)" }}
                    >
                      {card.completedAt ? formatTime(card.completedAt) : "-"}
                    </td>
                  </tr>
                ))}
                {list.data.length === 0 && (
                  <tr>
                    <td colSpan={8} style={{ padding: 24, textAlign: "center" }}>
                      조건에 맞는 문의가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            {/* 페이지네이션 (L4) */}
            {list.totalPages > 0 && (
              <div className="pagination" data-testid="pagination">
                <div className="pages">
                  <button
                    className="pg"
                    disabled={page <= 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    data-testid="page-prev"
                  >
                    ◀
                  </button>
                  {Array.from({ length: list.totalPages }, (_, i) => i).map((p) => (
                    <button
                      key={p}
                      className={`pg ${p === list.page ? "current" : ""}`}
                      onClick={() => setPage(p)}
                      data-testid={`page-${p}`}
                    >
                      {p + 1}
                    </button>
                  ))}
                  <button
                    className="pg"
                    disabled={page >= list.totalPages - 1}
                    onClick={() => setPage((p) => Math.min(list.totalPages - 1, p + 1))}
                    data-testid="page-next"
                  >
                    ▶
                  </button>
                </div>
                <span className="summary">
                  총 {list.totalElements}건 · 페이지당 {list.size}건
                </span>
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
