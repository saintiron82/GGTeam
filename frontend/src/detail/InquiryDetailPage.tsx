import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  fetchDetail,
  pullInquiry,
  reanalyze,
  extractErrorMessage,
} from "../common/api";
import type { InquiryDetail } from "../common/apiTypes";
import { STATUS_LABEL, TYPE_LABEL } from "../common/types";
import { AppLayout } from "../common/AppLayout";
import { UrgencyBadge } from "../common/UrgencyBadge";
import { DraftEditor } from "../editor/DraftEditor";

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" });
  } catch {
    return iso;
  }
}

function Section({ title, children, testid }: { title: string; children: React.ReactNode; testid?: string }) {
  return (
    <div className="card" style={{ marginBottom: 16 }} data-testid={testid}>
      <h3 style={{ marginTop: 0, fontSize: 15 }}>{title}</h3>
      {children}
    </div>
  );
}

export function InquiryDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      setDetail(await fetchDetail(id));
    } catch (err) {
      setError(extractErrorMessage(err, "문의를 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleClaim() {
    setActionMsg(null);
    setError(null);
    try {
      const pulled = await pullInquiry();
      if (!pulled) {
        setActionMsg("배정 가능한 미배정 문의가 없습니다.");
        return;
      }
      // pull은 미배정 1건을 자동 배정 → 해당 문의 상세로 이동
      navigate(`/inquiry/${pulled.inquiry.inquiryId}`);
      setDetail(pulled);
    } catch (err) {
      setError(extractErrorMessage(err, "담당하기에 실패했습니다."));
    }
  }

  async function handleReanalyze() {
    if (!id) return;
    setError(null);
    try {
      await reanalyze(id);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err, "재분석 요청에 실패했습니다."));
    }
  }

  return (
    <AppLayout>
      <div className="page" style={{ maxWidth: 1000 }}>
        <button onClick={() => navigate("/board")} data-testid="detail-back">
          ← 보드로
        </button>

        {loading && <div data-testid="detail-loading" style={{ marginTop: 16 }}>불러오는 중...</div>}
        {error && (
          <div className="error-msg" data-testid="detail-error" role="alert" style={{ marginTop: 16 }}>
            {error}
          </div>
        )}
        {actionMsg && (
          <div data-testid="detail-action-msg" style={{ marginTop: 16, color: "var(--color-muted)" }}>
            {actionMsg}
          </div>
        )}

        {!loading && detail && (
          <div style={{ marginTop: 16 }} data-testid="detail-content">
            {/* 헤더 */}
            <div
              className="card"
              style={{ marginBottom: 16, display: "flex", justifyContent: "space-between", alignItems: "center" }}
            >
              <div>
                <div style={{ fontWeight: 700, fontSize: 18 }} data-testid="detail-id">
                  {detail.inquiry.inquiryId}
                </div>
                <div style={{ color: "var(--color-muted)", fontSize: 13, marginTop: 4 }}>
                  접수: {formatTime(detail.inquiry.createdAt)}
                  {detail.inquiry.assignedOperator ? ` · 담당: ${detail.inquiry.assignedOperator}` : " · 미배정"}
                </div>
              </div>
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                {detail.analysis && <UrgencyBadge urgency={detail.analysis.urgency} />}
                <span className="badge badge-normal" data-testid="detail-status">
                  {STATUS_LABEL[detail.inquiry.status]}
                </span>
              </div>
            </div>

            {/* 액션: 담당하기 / 재분석 */}
            <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
              {detail.inquiry.status === "PENDING_ASSIGNMENT" && (
                <button className="primary" onClick={handleClaim} data-testid="detail-claim">
                  담당하기 (Pull)
                </button>
              )}
              {(detail.inquiry.status === "OPERATOR_REVIEWING" ||
                detail.inquiry.status === "MANUAL_CLASSIFICATION_PENDING") && (
                <button onClick={handleReanalyze} data-testid="detail-reanalyze">
                  재분석 요청
                </button>
              )}
            </div>

            {/* 원본 문의 */}
            <Section title="원본 문의" testid="detail-original">
              <div style={{ marginBottom: 8 }}>
                <span className="badge badge-normal">{TYPE_LABEL[detail.inquiry.customerType]}</span>
              </div>
              <div style={{ whiteSpace: "pre-wrap", lineHeight: 1.6 }}>
                {detail.inquiry.content}
              </div>
              {Object.keys(detail.inquiry.customerInfo ?? {}).length > 0 && (
                <pre
                  style={{ background: "#f4f5f7", padding: 10, borderRadius: 6, marginTop: 10, fontSize: 12 }}
                >
                  {JSON.stringify(detail.inquiry.customerInfo, null, 2)}
                </pre>
              )}
            </Section>

            {/* AI 분석 */}
            <Section title="AI 분석" testid="detail-analysis">
              {detail.analysis ? (
                <div style={{ display: "grid", gap: 6 }}>
                  <div>유형: {TYPE_LABEL[detail.analysis.aiType]}{detail.analysis.subCategory ? ` / ${detail.analysis.subCategory}` : ""}</div>
                  <div>요약: {detail.analysis.summary ?? "-"}</div>
                  {detail.analysis.keywords && detail.analysis.keywords.length > 0 && (
                    <div>키워드: {detail.analysis.keywords.join(", ")}</div>
                  )}
                  {detail.analysis.failureType && (
                    <div className="error-msg">분석 실패: {detail.analysis.failureType}</div>
                  )}
                  {detail.analysis.systemQueryResult && (
                    <pre style={{ background: "#f4f5f7", padding: 10, borderRadius: 6, fontSize: 12 }}>
                      {JSON.stringify(detail.analysis.systemQueryResult, null, 2)}
                    </pre>
                  )}
                </div>
              ) : (
                <div style={{ color: "var(--color-muted)" }}>아직 분석 결과가 없습니다.</div>
              )}
            </Section>

            {/* 진단 */}
            {detail.diagnosis && (
              <Section title="진단" testid="detail-diagnosis">
                <div style={{ display: "grid", gap: 6 }}>
                  <div>원인: {detail.diagnosis.cause}</div>
                  <div>제안 방향: {detail.diagnosis.suggestedDirection}</div>
                  <div>신뢰도: {(detail.diagnosis.confidence * 100).toFixed(0)}%</div>
                </div>
              </Section>
            )}

            {/* 답변 초안 (DraftEditor) */}
            {detail.currentDraft ? (
              <div style={{ marginBottom: 16 }}>
                <DraftEditor
                  inquiryId={detail.inquiry.inquiryId}
                  draft={detail.currentDraft}
                  editable={detail.inquiry.status === "OPERATOR_REVIEWING"}
                  onChanged={load}
                />
              </div>
            ) : (
              <Section title="답변 초안" testid="detail-no-draft">
                <div style={{ color: "var(--color-muted)" }}>아직 생성된 초안이 없습니다.</div>
              </Section>
            )}

            {/* 처리 이력 타임라인 */}
            <Section title="처리 이력" testid="detail-history">
              {detail.history.length === 0 ? (
                <div style={{ color: "var(--color-muted)" }}>이력이 없습니다.</div>
              ) : (
                <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                  {detail.history.map((h, i) => (
                    <li
                      key={i}
                      data-testid={`history-item-${i}`}
                      style={{
                        borderLeft: "2px solid var(--color-border)",
                        paddingLeft: 12,
                        paddingBottom: 12,
                        position: "relative",
                      }}
                    >
                      <div style={{ fontWeight: 600 }}>
                        {h.action} <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>· {h.operator}</span>
                      </div>
                      <div style={{ fontSize: 12, color: "var(--color-muted)" }}>{formatTime(h.timestamp)}</div>
                      {h.reason && <div style={{ fontSize: 13, marginTop: 2 }}>사유: {h.reason}</div>}
                    </li>
                  ))}
                </ul>
              )}
            </Section>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
