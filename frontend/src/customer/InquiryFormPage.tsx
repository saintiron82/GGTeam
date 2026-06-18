import { useState, FormEvent } from "react";
import { createInquiry, extractErrorMessage } from "../common/api";
import type { InquiryType } from "../common/types";
import { TYPE_LABEL } from "../common/types";

const TYPE_OPTIONS: InquiryType[] = ["PAYMENT", "ITEM_DELIVERY", "ACCOUNT", "ETC"];
const MIN_CONTENT_LENGTH = 10;

export function InquiryFormPage() {
  const [userId, setUserId] = useState("");
  const [nickname, setNickname] = useState("");
  const [customerType, setCustomerType] = useState<InquiryType | "">("");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submittedId, setSubmittedId] = useState<string | null>(null);

  function validate(): string | null {
    if (!userId.trim()) return "사용자 ID를 입력하세요.";
    if (!customerType) return "문의 유형을 선택하세요.";
    if (content.trim().length < MIN_CONTENT_LENGTH)
      return `문의 내용은 최소 ${MIN_CONTENT_LENGTH}자 이상 입력하세요.`;
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setSubmitting(true);
    try {
      const res = await createInquiry({
        customerInfo: {
          userId: userId.trim(),
          nickname: nickname.trim() || "고객",
          channel: "WEB",
        },
        customerType: customerType as InquiryType,
        content: content.trim(),
      });
      setSubmittedId(res.inquiryId);
    } catch (err) {
      setError(extractErrorMessage(err, "문의 접수에 실패했습니다."));
    } finally {
      setSubmitting(false);
    }
  }

  function resetForm() {
    setUserId("");
    setNickname("");
    setCustomerType("");
    setContent("");
    setSubmittedId(null);
    setError(null);
  }

  if (submittedId) {
    return (
      <div className="center-screen">
        <div className="card" style={{ width: 420 }} data-testid="inquiry-success">
          <h1 style={{ fontSize: 20, marginTop: 0 }}>문의가 접수되었습니다</h1>
          <p style={{ color: "var(--color-muted)" }}>
            아래 문의 번호로 처리 상태를 확인할 수 있습니다.
          </p>
          <div
            className="card"
            style={{ background: "#f4f5f7", fontWeight: 700, fontSize: 16 }}
            data-testid="inquiry-id"
          >
            {submittedId}
          </div>
          <button
            className="primary"
            style={{ width: "100%", marginTop: 16 }}
            onClick={resetForm}
            data-testid="inquiry-new"
          >
            새 문의 작성
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="center-screen">
      <form
        className="card"
        style={{ width: 480 }}
        onSubmit={handleSubmit}
        data-testid="inquiry-form"
      >
        <h1 style={{ fontSize: 20, marginTop: 0, marginBottom: 4 }}>고객 문의 접수</h1>
        <p style={{ color: "var(--color-muted)", marginTop: 0, marginBottom: 20 }}>
          문의 유형과 내용을 입력해 주세요.
        </p>

        <div className="field">
          <label htmlFor="userId">사용자 ID *</label>
          <input
            id="userId"
            data-testid="inquiry-userId"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            disabled={submitting}
          />
        </div>

        <div className="field">
          <label htmlFor="nickname">닉네임 (선택)</label>
          <input
            id="nickname"
            data-testid="inquiry-nickname"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            disabled={submitting}
          />
        </div>

        <div className="field">
          <label htmlFor="customerType">문의 유형 *</label>
          <select
            id="customerType"
            data-testid="inquiry-type"
            value={customerType}
            onChange={(e) => setCustomerType(e.target.value as InquiryType)}
            disabled={submitting}
          >
            <option value="">유형 선택</option>
            {TYPE_OPTIONS.map((t) => (
              <option key={t} value={t}>
                {TYPE_LABEL[t]}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="content">문의 내용 * (최소 {MIN_CONTENT_LENGTH}자)</label>
          <textarea
            id="content"
            data-testid="inquiry-content"
            rows={6}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={submitting}
          />
          <div style={{ color: "var(--color-muted)", fontSize: 12, marginTop: 4 }}>
            {content.trim().length} / {MIN_CONTENT_LENGTH}자
          </div>
        </div>

        {error && (
          <div className="error-msg" data-testid="inquiry-error" role="alert">
            {error}
          </div>
        )}

        <button
          type="submit"
          className="primary"
          style={{ width: "100%", marginTop: 8 }}
          disabled={submitting}
          data-testid="inquiry-submit"
        >
          {submitting ? "접수 중..." : "문의 접수"}
        </button>
      </form>
    </div>
  );
}
