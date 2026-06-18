import { useState, FormEvent } from "react";
import { createInquiry, extractErrorMessage } from "../common/api";
import type { InquiryType } from "../common/types";
import { TYPE_LABEL } from "../common/types";

const TYPE_OPTIONS: InquiryType[] = ["PAYMENT", "ITEM_DELIVERY", "ACCOUNT", "ETC"];
const MIN_CONTENT_LENGTH = 10;

// F1 / §4-D: 고객은 게임 로그인 상태에서 문의 페이지로 진입하므로 유저 ID·닉네임은
// 입력이 아니라 자동 입력(읽기 전용)이다. 실제 연동 시 게임 클라이언트 컨텍스트(토큰/세션)에서
// 받아야 하나 전달 방식 미확정 → 데모에서는 고정 샘플 사용.
const AUTH_CUSTOMER = { userId: "user12345", nickname: "홍길동", channel: "WEB" };

export function InquiryFormPage() {
  const [customerType, setCustomerType] = useState<InquiryType | "">("");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submittedId, setSubmittedId] = useState<string | null>(null);

  function validate(): string | null {
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
        customerInfo: { ...AUTH_CUSTOMER },
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
    setCustomerType("");
    setContent("");
    setSubmittedId(null);
    setError(null);
  }

  if (submittedId) {
    return (
      <div className="center-screen">
        <div className="card" style={{ width: 440 }} data-testid="inquiry-success">
          <h1 style={{ fontSize: 20, marginTop: 0 }}>문의가 접수되었습니다</h1>
          <p style={{ color: "var(--color-muted)" }}>
            아래 문의 번호로 처리 상태를 확인할 수 있습니다.
          </p>
          <div
            className="mono"
            style={{
              background: "var(--color-box)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-sm)",
              padding: 12,
              fontWeight: 700,
              fontSize: 16,
              color: "var(--color-primary)",
            }}
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
        <h1 style={{ fontSize: 20, marginTop: 0, marginBottom: 4 }}>문의하기</h1>
        <p style={{ color: "var(--color-muted)", marginTop: 0, marginBottom: 20 }}>
          문의 유형과 내용을 입력해 주세요.
        </p>

        {/* 게임 로그인 상태에서 진입 → 유저 정보 자동 입력(읽기 전용) */}
        <div
          data-testid="inquiry-customer-info"
          style={{
            display: "flex",
            gap: 16,
            alignItems: "center",
            padding: "10px 12px",
            background: "var(--color-box)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-sm)",
            marginBottom: 16,
            fontSize: 13,
          }}
        >
          <div>
            <div style={{ fontSize: 11, color: "var(--color-muted)", marginBottom: 2 }}>
              유저 ID
            </div>
            <div style={{ fontWeight: 600 }} data-testid="inquiry-userId">
              {AUTH_CUSTOMER.userId}
            </div>
          </div>
          <div style={{ width: 1, alignSelf: "stretch", background: "var(--color-border)" }} />
          <div>
            <div style={{ fontSize: 11, color: "var(--color-muted)", marginBottom: 2 }}>
              닉네임
            </div>
            <div style={{ fontWeight: 600 }} data-testid="inquiry-nickname">
              {AUTH_CUSTOMER.nickname}
            </div>
          </div>
          <div style={{ flex: 1 }} />
          <span style={{ fontSize: 11, color: "var(--color-muted)" }}>
            🔒 로그인 정보 자동 입력
          </span>
        </div>

        <div className="field">
          <label htmlFor="customerType">
            문의 유형 <span style={{ color: "var(--color-danger)" }}>*</span>
          </label>
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
          <label htmlFor="content">
            문의 내용 <span style={{ color: "var(--color-danger)" }}>*</span>
          </label>
          <textarea
            id="content"
            data-testid="inquiry-content"
            rows={6}
            placeholder="결제했는데 아이템이 지급되지 않았습니다..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={submitting}
          />
          <div className="helper">
            {content.trim().length} / 최소 {MIN_CONTENT_LENGTH}자
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
          {submitting ? "접수 중..." : "문의 제출"}
        </button>
      </form>
    </div>
  );
}
