import { useState, useMemo } from "react";
import { saveDraft, approve, reject, extractErrorMessage } from "../common/api";
import type { InquiryDetail } from "../common/apiTypes";

const REGENERATION_LIMIT = 3;

interface Props {
  inquiryId: string;
  draft: NonNullable<InquiryDetail["currentDraft"]>;
  // 액션 가능 여부 (OPERATOR_REVIEWING 상태에서만 true)
  editable: boolean;
  // 액션 성공 후 상세 새로고침
  onChanged: () => void;
  // 재분석 (상세에서 주입) — AI_ANALYZING으로 되돌림
  onReanalyze?: () => void;
}

// 간단한 단어 단위 diff — 원문 대비 추가/변경된 토큰을 강조
function renderDiff(original: string, edited: string) {
  const origTokens = original.split(/(\s+)/);
  const editTokens = edited.split(/(\s+)/);
  const origSet = new Set(origTokens);
  return editTokens.map((tok, i) => {
    const changed = tok.trim() !== "" && !origSet.has(tok);
    return changed ? (
      <mark
        key={i}
        data-testid="draft-diff-changed"
        style={{ background: "var(--color-warning-bg)", color: "var(--color-warning-text)", padding: "0 1px" }}
      >
        {tok}
      </mark>
    ) : (
      <span key={i}>{tok}</span>
    );
  });
}

export function DraftEditor({ inquiryId, draft, editable, onChanged, onReanalyze }: Props) {
  const [content, setContent] = useState(draft.content);
  const [editing, setEditing] = useState(false);
  const [showReject, setShowReject] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const original = draft.content;
  const isDirty = content !== original;
  const regenCount = draft.regenerationCount ?? 0;
  const regenExhausted = regenCount >= REGENERATION_LIMIT;

  const diffView = useMemo(() => renderDiff(original, content), [original, content]);

  async function run(fn: () => Promise<unknown>, errFallback: string) {
    setBusy(true);
    setError(null);
    try {
      await fn();
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err, errFallback));
    } finally {
      setBusy(false);
    }
  }

  async function handleSave() {
    await run(() => saveDraft(inquiryId, content), "초안 저장에 실패했습니다.");
    setEditing(false);
  }

  async function handleApprove() {
    await run(
      () => approve(inquiryId, isDirty ? content : undefined),
      "승인에 실패했습니다.",
    );
  }

  async function handleReject() {
    if (!rejectReason.trim()) {
      setError("반려 사유를 입력하세요.");
      return;
    }
    await run(() => reject(inquiryId, rejectReason.trim()), "반려에 실패했습니다.");
    setShowReject(false);
    setRejectReason("");
  }

  return (
    <div className="card" data-testid="draft-editor">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 12,
        }}
      >
        <div className="card-label" style={{ marginBottom: 0 }}>
          AI 답변 초안
        </div>
        <span style={{ fontSize: 12, color: "var(--color-muted)" }} data-testid="draft-status">
          상태: {draft.status} · 재생성 {regenCount}/{REGENERATION_LIMIT}
        </span>
      </div>

      {editing ? (
        <textarea
          data-testid="draft-textarea"
          rows={10}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={busy}
        />
      ) : (
        <div
          data-testid="draft-content"
          style={{
            whiteSpace: "pre-wrap",
            lineHeight: 1.7,
            background: "var(--color-surface)",
            border: "1px solid var(--color-border-input)",
            borderRadius: "var(--radius-sm)",
            padding: 12,
            minHeight: 120,
            fontSize: 13,
          }}
        >
          {isDirty ? diffView : content}
        </div>
      )}

      {error && (
        <div className="error-msg" data-testid="draft-error" role="alert">
          {error}
        </div>
      )}

      {!editable && (
        <p className="helper" data-testid="draft-readonly">
          현재 상태에서는 편집/승인할 수 없습니다. (담당자 확인중 상태에서만 가능)
        </p>
      )}

      {editable && (
        <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
          {editing ? (
            <>
              <button className="primary" onClick={handleSave} disabled={busy} data-testid="draft-save">
                저장
              </button>
              <button
                className="ghost"
                onClick={() => {
                  setContent(original);
                  setEditing(false);
                }}
                disabled={busy}
                data-testid="draft-cancel-edit"
              >
                편집 취소
              </button>
            </>
          ) : (
            <button
              className="secondary"
              onClick={() => setEditing(true)}
              disabled={busy}
              data-testid="draft-edit"
            >
              수정
            </button>
          )}

          <button className="primary" onClick={handleApprove} disabled={busy} data-testid="draft-approve">
            {isDirty ? "수정 후 승인" : "승인"}
          </button>

          <button
            className="danger"
            onClick={() => setShowReject((v) => !v)}
            disabled={busy || regenExhausted}
            data-testid="draft-reject-toggle"
            title={regenExhausted ? "재생성 한도(3회)를 초과했습니다." : undefined}
          >
            반려 및 재생성
          </button>

          {onReanalyze && (
            <button className="ghost" onClick={onReanalyze} disabled={busy} data-testid="draft-reanalyze">
              재분석
            </button>
          )}
        </div>
      )}

      {showReject && editable && (
        <div style={{ marginTop: 12 }} data-testid="draft-reject-panel">
          <label htmlFor="reject-reason">반려 사유 (필수)</label>
          <textarea
            id="reject-reason"
            data-testid="draft-reject-reason"
            rows={3}
            placeholder="예: 보상 금액 안내가 빠졌습니다. 지급 아이템 수량을 명시해 주세요."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            disabled={busy}
          />
          <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
            <button className="danger" onClick={handleReject} disabled={busy} data-testid="draft-reject-submit">
              반려 후 재생성
            </button>
            <button className="ghost" onClick={() => setShowReject(false)} disabled={busy}>
              취소
            </button>
          </div>
          <div className="helper" style={{ color: "var(--color-danger-text)" }}>
            * 사유 없이 반려 불가 (REASON_REQUIRED)
          </div>
        </div>
      )}
    </div>
  );
}
