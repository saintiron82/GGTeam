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
import { UrgencyBadge } from "../common/UrgencyBadge";

// 칸반에 표시할 상태 컬럼 순서
const COLUMN_ORDER: InquiryStatus[] = [
  "RECEIVED",
  "AI_ANALYZING",
  "PENDING_ASSIGNMENT",
  "OPERATOR_REVIEWING",
  "APPROVED",
  "SENT",
];
const TYPE_OPTIONS: InquiryType[] = ["PAYMENT", "ITEM_DELIVERY", "ACCOUNT", "ETC"];
const URGENCY_OPTIONS: Urgency[] = ["HIGH", "NORMAL", "LOW"];

type ViewMode = "kanban" | "list";

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
  const [typeFilter, setTypeFilter] = useState<InquiryType | "">("");
  const [urgencyFilter, setUrgencyFilter] = useState<Urgency | "">("");
  const [appliedFilter, setAppliedFilter] = useState<DashboardFilter>({});

  const onCardClick = useCallback(
    (inquiryId: string) => navigate(`/inquiry/${inquiryId}`),
    [navigate],
  );

  const loadData = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const [notif] = await Promise.all([fetchNotifications()]);
      setNotifications(notif);
      if (view === "kanban") {
        setBoard(await fetchBoard(appliedFilter));
      } else {
        setList(await fetchInquiries({ ...appliedFilter, page: 0, size: 50 }));
      }
    } catch (err) {
      if (!silent) setError(extractErrorMessage(err, "보드를 불러오지 못했습니다."));
    } finally {
      if (!silent) setLoading(false);
    }
  }, [view, appliedFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // 3초 폴링: 새로고침 없이 새 문의가 보드에 자동 반영 (조용히 갱신 — 로딩 깜빡임 없음)
  useEffect(() => {
    const id = setInterval(() => loadData(true), 3000);
    return () => clearInterval(id);
  }, [loadData]);

  function applyFilters() {
    setAppliedFilter({
      keyword: keyword.trim() || undefined,
      type: typeFilter || undefined,
      urgency: urgencyFilter || undefined,
    });
  }

  function resetFilters() {
    setKeyword("");
    setTypeFilter("");
    setUrgencyFilter("");
    setAppliedFilter({});
  }

  return (
    <AppLayout>
      <div className="page">
        <div className="page-header">
          <div>
            <h1 className="page-title">문의 보드</h1>
            <p className="page-subtitle">상태별 문의 처리 흐름</p>
          </div>
        </div>
        {/* 알림 배지 */}
        <div
          style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 16 }}
          data-testid="notification-bar"
        >
          <span
            className="badge badge-urgent"
            data-testid="notif-unassigned"
            style={{ fontSize: 13, padding: "4px 12px" }}
          >
            미배정 {notifications?.unassignedCount ?? 0}건
          </span>
          <span
            className="badge badge-normal"
            data-testid="notif-urgent"
            style={{ fontSize: 13, padding: "4px 12px" }}
          >
            긴급 {notifications?.urgentCount ?? 0}건
          </span>
          <div style={{ flex: 1 }} />
          <button
            className={view === "kanban" ? "primary" : ""}
            onClick={() => setView("kanban")}
            data-testid="view-kanban"
          >
            칸반
          </button>
          <button
            className={view === "list" ? "primary" : ""}
            onClick={() => setView("list")}
            data-testid="view-list"
          >
            리스트
          </button>
        </div>

        {/* 필터/검색 바 */}
        <div
          className="card"
          style={{ display: "flex", gap: 12, alignItems: "flex-end", marginBottom: 16 }}
          data-testid="filter-bar"
        >
          <div style={{ flex: 2 }}>
            <label htmlFor="keyword">검색</label>
            <input
              id="keyword"
              data-testid="filter-keyword"
              value={keyword}
              placeholder="키워드 / 문의번호"
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && applyFilters()}
            />
          </div>
          <div style={{ flex: 1 }}>
            <label htmlFor="type">유형</label>
            <select
              id="type"
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
          <div style={{ flex: 1 }}>
            <label htmlFor="urgency">긴급도</label>
            <select
              id="urgency"
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
          <button className="primary" onClick={applyFilters} data-testid="filter-apply">
            적용
          </button>
          <button onClick={resetFilters} data-testid="filter-reset">
            초기화
          </button>
        </div>

        {error && (
          <div className="error-msg" data-testid="board-error" role="alert">
            {error}
          </div>
        )}
        {loading && <div data-testid="board-loading">불러오는 중...</div>}

        {/* 칸반 뷰 */}
        {!loading && view === "kanban" && board && (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: `repeat(${COLUMN_ORDER.length}, minmax(200px, 1fr))`,
              gap: 12,
              overflowX: "auto",
            }}
            data-testid="kanban-board"
          >
            {COLUMN_ORDER.map((status) => {
              const cards = board[status] ?? [];
              return (
                <div
                  key={status}
                  data-testid={`column-${status}`}
                  className="kanban-column"
                >
                  <div className="kanban-column-head">
                    <span>{STATUS_LABEL[status]}</span>
                    <span className="kanban-column-count">{cards.length}</span>
                  </div>
                  {cards.map((card) => (
                    <InquiryCardItem
                      key={card.inquiryId}
                      card={card}
                      onClick={onCardClick}
                    />
                  ))}
                </div>
              );
            })}
          </div>
        )}

        {/* 리스트 뷰 */}
        {!loading && view === "list" && list && (
          <div className="card" data-testid="list-view" style={{ padding: 0, overflow: "hidden" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>문의번호</th>
                  <th>유형</th>
                  <th>긴급도</th>
                  <th>상태</th>
                  <th>담당자</th>
                  <th>요약</th>
                </tr>
              </thead>
              <tbody>
                {list.data.map((card) => (
                  <tr
                    key={card.inquiryId}
                    data-testid={`list-row-${card.inquiryId}`}
                    onClick={() => onCardClick(card.inquiryId)}
                    style={{
                      cursor: "pointer",
                      background:
                        card.urgency === "HIGH" ? "var(--urgent-bg)" : undefined,
                    }}
                  >
                    <td style={{ fontWeight: 600, fontFamily: "ui-monospace, monospace", fontSize: 12 }}>
                      {card.inquiryId.slice(0, 8)}
                    </td>
                    <td>{TYPE_LABEL[card.aiType ?? card.customerType]}</td>
                    <td>
                      <UrgencyBadge urgency={card.urgency} />
                    </td>
                    <td>{STATUS_LABEL[card.status]}</td>
                    <td>{card.assignedOperator ?? "-"}</td>
                    <td style={{ color: "var(--color-muted)" }}>
                      {card.summary ?? (card.content ? card.content.slice(0, 40) : "분석 대기 중")}
                    </td>
                  </tr>
                ))}
                {list.data.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ padding: 32, textAlign: "center", color: "var(--color-muted)" }}>
                      조건에 맞는 문의가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
