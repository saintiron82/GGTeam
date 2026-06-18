import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  fetchDetail,
  pullInquiry,
  reanalyze,
  extractErrorMessage,
} from "../common/api";
import type { InquiryDetail } from "../common/apiTypes";
import { AppLayout } from "../common/AppLayout";
import { Badge, TypeBadge, StatusBadge, UrgencyBadge } from "../common/badges";
import { DraftEditor } from "../editor/DraftEditor";

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" });
  } catch {
    return iso;
  }
}

function Card({
  label,
  children,
  testid,
  style,
}: {
  label: string;
  children: React.ReactNode;
  testid?: string;
  style?: React.CSSProperties;
}) {
  return (
    <div className="card" style={{ marginBottom: 12, ...style }} data-testid={testid}>
      <div className="card-label">{label}</div>
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
      navigate(`/inquiry/${pulled.inquiry.inquiryId}`);
      setDetail(pulled);
    } catch (err) {
      setError(extractErrorMessage(err, "담당하기에 실패했습니다."));
    }
  }

  const handleReanalyze = useCallback(async () => {
    if (!id) return;
    setError(null);
    try {
      await reanalyze(id);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err, "재분석 요청에 실패했습니다."));
    }
  }, [id, load]);

  const ci = (detail?.inquiry.customerInfo ?? {}) as {
    userId?: string;
    nickname?: string;
  };

  return (
    <AppLayout>
      <div className="page" style={{ maxWidth: 1100 }}>
        <button className="ghost" onClick={() => navigate("/board")} data-testid="detail-back">
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
              style={{
                marginBottom: 12,
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <div>
                <div className="mono" style={{ fontWeight: 700, fontSize: 18 }} data-testid="detail-id">
                  {detail.inquiry.inquiryId}
                </div>
                <div style={{ color: "var(--color-muted)", fontSize: 13, marginTop: 4 }}>
                  접수: {formatTime(detail.inquiry.createdAt)}
                  {detail.inquiry.assignedOperator
                    ? ` · 담당: ${detail.inquiry.assignedOperator}`
                    : " · 미배정"}
                </div>
              </div>
              <div style={{ display: "flex", gap: 8, alignItems: "center" }} data-testid="detail-status">
                {detail.analysis && <UrgencyBadge urgency={detail.analysis.urgency} />}
                <StatusBadge status={detail.inquiry.status} />
              </div>
            </div>

            {/* 2단 레이아웃 */}
            <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
              {/* 좌측: 본문 + 분석 + 진단 */}
              <div style={{ flex: 2, minWidth: 0 }}>
                <Card label="원본 문의" testid="detail-original">
                  <div
                    style={{
                      fontSize: 12,
                      color: "var(--color-muted)",
                      marginBottom: 8,
                      display: "flex",
                      gap: 6,
                      alignItems: "center",
                      flexWrap: "wrap",
                    }}
                  >
                    <span>고객선택:</span>
                    <TypeBadge type={detail.inquiry.customerType} />
                    {ci.userId && <span>· {ci.userId}</span>}
                    {ci.nickname && <span>({ci.nickname})</span>}
                  </div>
                  <div style={{ whiteSpace: "pre-wrap", lineHeight: 1.6, fontSize: 13 }}>
                    {detail.inquiry.content}
                  </div>
                </Card>

                <Card label="AI 분석 결과" testid="detail-analysis">
                  {detail.analysis ? (
                    <div style={{ display: "grid", gap: 8, fontSize: 13 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap" }}>
                        <span>AI 유형:</span>
                        <TypeBadge type={detail.analysis.aiType} />
                        {detail.analysis.subCategory && (
                          <span style={{ color: "var(--color-muted)" }}>
                            / {detail.analysis.subCategory}
                          </span>
                        )}
                        <UrgencyBadge urgency={detail.analysis.urgency} />
                      </div>
                      <div>요약: {detail.analysis.summary ?? "-"}</div>
                      {detail.analysis.keywords && detail.analysis.keywords.length > 0 && (
                        <div style={{ display: "flex", alignItems: "center", gap: 4, flexWrap: "wrap" }}>
                          <span>키워드:</span>
                          {detail.analysis.keywords.map((k) => (
                            <Badge key={k} color="default">
                              {k}
                            </Badge>
                          ))}
                        </div>
                      )}
                      {detail.analysis.failureType && (
                        <div className="error-msg">분석 실패: {detail.analysis.failureType}</div>
                      )}
                      {detail.analysis.systemQueryResult && (
                        <pre
                          className="mono"
                          style={{
                            background: "var(--color-box)",
                            padding: 10,
                            borderRadius: "var(--radius-sm)",
                            fontSize: 12,
                            overflowX: "auto",
                          }}
                        >
                          {JSON.stringify(detail.analysis.systemQueryResult, null, 2)}
                        </pre>
                      )}
                    </div>
                  ) : (
                    <div style={{ color: "var(--color-muted)" }}>아직 분석 결과가 없습니다.</div>
                  )}
                </Card>

                {detail.diagnosis && (
                  <Card label="AI 진단" testid="detail-diagnosis">
                    <div style={{ display: "grid", gap: 6, fontSize: 13, lineHeight: 1.7 }}>
                      <div>원인: {detail.diagnosis.cause}</div>
                      <div>처리 방향: {detail.diagnosis.suggestedDirection}</div>
                      <div>신뢰도: {(detail.diagnosis.confidence * 100).toFixed(0)}%</div>
                    </div>
                  </Card>
                )}
              </div>

              {/* 우측: 처리 이력 타임라인 + 액션 */}
              <div style={{ flex: 1, minWidth: 260 }}>
                <Card label="처리 이력 (타임라인)" testid="detail-history">
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
                          }}
                        >
                          <div style={{ fontWeight: 600, fontSize: 13 }}>
                            {h.action}{" "}
                            <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>
                              · {h.operator}
                            </span>
                          </div>
                          <div className="mono" style={{ fontSize: 11, color: "var(--color-muted)" }}>
                            {formatTime(h.timestamp)}
                          </div>
                          {h.reason && (
                            <div style={{ fontSize: 12, marginTop: 2 }}>사유: {h.reason}</div>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </Card>

                {detail.inquiry.status === "PENDING_ASSIGNMENT" && (
                  <button
                    className="primary"
                    style={{ width: "100%" }}
                    onClick={handleClaim}
                    data-testid="detail-claim"
                  >
                    담당하기 (Pull)
                  </button>
                )}
                {detail.inquiry.status === "MANUAL_CLASSIFICATION_PENDING" && (
                  <button
                    className="secondary"
                    style={{ width: "100%" }}
                    onClick={handleReanalyze}
                    data-testid="detail-reanalyze"
                  >
                    재분석 요청
                  </button>
                )}
              </div>
            </div>

            {/* 하단: 답변 편집기 영역 (담당하기 후 펼쳐짐) */}
            <div style={{ marginTop: 8 }}>
              {detail.currentDraft ? (
                <DraftEditor
                  inquiryId={detail.inquiry.inquiryId}
                  draft={detail.currentDraft}
                  editable={detail.inquiry.status === "OPERATOR_REVIEWING"}
                  onChanged={load}
                  onReanalyze={
                    detail.inquiry.status === "OPERATOR_REVIEWING" ? handleReanalyze : undefined
                  }
                />
              ) : detail.inquiry.status === "PENDING_ASSIGNMENT" ? (
                <div
                  data-testid="detail-editor-hint"
                  style={{
                    border: "1px dashed var(--color-border-input)",
                    borderRadius: "var(--radius-lg)",
                    padding: 20,
                    textAlign: "center",
                    color: "var(--color-muted)",
                    background: "var(--color-box)",
                    fontSize: 13,
                    lineHeight: 1.7,
                  }}
                >
                  ⬇ <b style={{ color: "var(--color-text)" }}>담당하기(Pull)</b> 클릭 시 상태가{" "}
                  <b style={{ color: "var(--color-primary)" }}>운영자확인중</b>으로 바뀌고, 이 영역에{" "}
                  <b style={{ color: "var(--color-text)" }}>답변 편집기</b>가 펼쳐집니다.
                </div>
              ) : (
                <Card label="답변 초안" testid="detail-no-draft">
                  <div style={{ color: "var(--color-muted)" }}>아직 생성된 초안이 없습니다.</div>
                </Card>
              )}
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
