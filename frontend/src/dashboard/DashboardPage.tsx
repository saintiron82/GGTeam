import { useEffect, useState } from "react";
import { AppLayout } from "../common/AppLayout";
import { fetchStats, extractErrorMessage } from "../common/api";
import { fetchOperatorStats, type OperatorStat } from "./operatorStatsApi";
import type { DashboardStats } from "../common/types";
import { STATUS_LABEL, TYPE_LABEL } from "../common/types";
import type { InquiryStatus, InquiryType } from "../common/types";

const STATUS_ORDER: InquiryStatus[] = [
  "RECEIVED",
  "AI_ANALYZING",
  "PENDING_ASSIGNMENT",
  "OPERATOR_REVIEWING",
  "APPROVED",
  "SENT",
  "MANUAL_CLASSIFICATION_PENDING",
];
const TYPE_ORDER: InquiryType[] = ["PAYMENT", "ITEM_DELIVERY", "ACCOUNT", "ETC"];

interface StatCardDef {
  label: string;
  value: number;
  accent: "indigo" | "red" | "amber" | "green" | "slate";
  unit?: string;
}

export function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [opStats, setOpStats] = useState<OperatorStat[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    const load = async (silent: boolean) => {
      try {
        const [s, ops] = await Promise.all([fetchStats(), fetchOperatorStats()]);
        if (!alive) return;
        setStats(s);
        setOpStats(ops);
        if (!silent) setLoading(false);
      } catch (err) {
        if (!alive || silent) return; // 폴링 실패는 조용히 무시
        setError(extractErrorMessage(err, "통계를 불러오지 못했습니다."));
        setLoading(false);
      }
    };
    load(false);
    // 3초 폴링: 새로고침 없이 통계가 자동 갱신
    const id = setInterval(() => load(true), 3000);
    return () => {
      alive = false;
      clearInterval(id);
    };
  }, []);

  const cards: StatCardDef[] = stats
    ? [
        { label: "전체 문의", value: stats.total, accent: "indigo" },
        { label: "오늘 접수", value: stats.todayCount, accent: "slate" },
        { label: "미배정", value: stats.unassigned, accent: "amber" },
        { label: "긴급", value: stats.urgent, accent: "red" },
        { label: "처리 중", value: stats.inProgress, accent: "indigo" },
        { label: "발송 완료", value: stats.completed, accent: "green" },
      ]
    : [];

  const statusMax = stats
    ? Math.max(1, ...STATUS_ORDER.map((s) => stats.statusCounts[s] ?? 0))
    : 1;
  const typeMax = stats
    ? Math.max(1, ...TYPE_ORDER.map((t) => stats.typeCounts[t] ?? 0))
    : 1;

  return (
    <AppLayout>
      <div className="page">
        <div className="page-header">
          <div>
            <h1 className="page-title">대시보드</h1>
            <p className="page-subtitle">CS 문의 처리 현황 한눈에 보기</p>
          </div>
        </div>

        {error && <div className="error-msg" role="alert">{error}</div>}
        {loading && <div data-testid="stats-loading">불러오는 중...</div>}

        {stats && (
          <>
            {/* 핵심 지표 카드 */}
            <div className="stat-grid" data-testid="stat-grid">
              {cards.map((c) => (
                <div className="stat-card" key={c.label}>
                  <span className={`stat-accent ${c.accent}`} />
                  <div className="stat-label">{c.label}</div>
                  <div className="stat-value">
                    {c.value}
                    <span className="stat-unit">건</span>
                  </div>
                </div>
              ))}
            </div>

            {/* 분포 차트 2단 */}
            <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr", gap: 14 }}>
              <div className="section-card">
                <h2 className="section-title">상태별 분포</h2>
                <div className="bar-chart">
                  {STATUS_ORDER.map((s) => {
                    const v = stats.statusCounts[s] ?? 0;
                    const cls =
                      s === "SENT" ? "green" : s === "MANUAL_CLASSIFICATION_PENDING" ? "red" : s === "PENDING_ASSIGNMENT" ? "amber" : "";
                    return (
                      <div className="bar-row" key={s}>
                        <span className="bar-label">{STATUS_LABEL[s]}</span>
                        <div className="bar-track">
                          <div
                            className={`bar-fill ${cls}`}
                            style={{ width: `${(v / statusMax) * 100}%` }}
                          />
                        </div>
                        <span className="bar-value">{v}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="section-card">
                <h2 className="section-title">유형별 분포</h2>
                <div className="bar-chart">
                  {TYPE_ORDER.map((t) => {
                    const v = stats.typeCounts[t] ?? 0;
                    return (
                      <div className="bar-row" key={t}>
                        <span className="bar-label">{TYPE_LABEL[t]}</span>
                        <div className="bar-track">
                          <div
                            className="bar-fill"
                            style={{ width: `${(v / typeMax) * 100}%` }}
                          />
                        </div>
                        <span className="bar-value">{v}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* 운영자별 처리 현황 */}
            <div className="section-card" style={{ marginTop: 14 }}>
              <h2 className="section-title">운영자별 처리 현황</h2>
              {opStats.length === 0 ? (
                <div style={{ color: "var(--color-muted)", fontSize: 13 }}>
                  운영자 데이터가 없습니다.
                </div>
              ) : (
                <table
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}
                  data-testid="operator-stats"
                >
                  <thead>
                    <tr style={{ textAlign: "left", color: "var(--color-muted)", fontSize: 12 }}>
                      <th style={{ padding: "6px 8px" }}>운영자</th>
                      <th style={{ padding: "6px 8px" }}>권한</th>
                      <th style={{ padding: "6px 8px", textAlign: "right" }}>담당 중</th>
                      <th style={{ padding: "6px 8px", textAlign: "right" }}>승인 완료</th>
                      <th style={{ padding: "6px 8px", textAlign: "right" }}>총 처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {opStats.map((o) => (
                      <tr key={o.operatorId} style={{ borderTop: "1px solid #eee" }}>
                        <td style={{ padding: "6px 8px", fontWeight: 600 }}>{o.username}</td>
                        <td style={{ padding: "6px 8px" }}>{o.role}</td>
                        <td style={{ padding: "6px 8px", textAlign: "right" }}>{o.assigned}</td>
                        <td style={{ padding: "6px 8px", textAlign: "right" }}>{o.approved}</td>
                        <td style={{ padding: "6px 8px", textAlign: "right" }}>{o.actions}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
}
