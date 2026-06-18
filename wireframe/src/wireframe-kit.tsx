import React from "react";

/**
 * GRAVITY 디자인 가이드를 적용한 와이어프레임 공통 UI 키트.
 * 토큰 출처: wireframe/sample/gravity_*.html (디자인 토큰 / Atomic 컴포넌트 스펙)
 *
 * - 컬러: Primary #05539A, Accent #06ADEC(강조 한정), Neutral Gray
 * - 타이포: Pretendard(국문) / DM Sans(영문·숫자) — index.html에서 로드
 * - Radius: sm 4px(버튼·입력·뱃지), lg 8px(카드)
 * - Elevation: Level1(카드), Level2(드롭다운)
 */

// === Semantic 토큰 (Light Mode) ===
export const colors = {
  // 브랜드
  primary: "#05539A",
  primaryHover: "#043E75",
  primaryActive: "#022B52",
  primary50: "#EDF3FA", // 호버 배경, 선택 행
  accent: "#06ADEC", // 뱃지·링크·하이라이트 한정

  // 텍스트
  text: "#242021",
  muted: "#6B7280",
  inverse: "#FFFFFF",

  // 보더 / 배경
  line: "#DDE2EA", // 기본 보더 (이전 #bbb 대체)
  lineInput: "#C8CDD7", // 입력 필드 보더
  bgPage: "#F8F9FA",
  bgSurface: "#FFFFFF",
  box: "#F1F3F6", // 연한 영역 배경 (이전 #f0f0f0 대체)
  boxAlt: "#E2E6EC", // 테이블 헤더/컬럼 헤더 (이전 #e4e4e4 대체)

  // 상태 (semantic)
  success: "#10B981",
  successBg: "#ECFDF5",
  successText: "#065F46",
  warning: "#F59E0B",
  warningBg: "#FFFBEB",
  warningText: "#92400E",
  error: "#EF4444",
  errorBg: "#FEF2F2",
  errorText: "#991B1B",

  // 긴급도 매핑 (HIGH=error, NORMAL=warning, LOW=neutral)
  high: "#EF4444",
  normal: "#F59E0B",
  low: "#9AA1AF",
};

export const elevation = {
  level1: "0 1px 3px rgba(0,0,0,.08), 0 1px 2px rgba(0,0,0,.05)",
  level2: "0 4px 12px rgba(0,0,0,.1), 0 2px 4px rgba(0,0,0,.06)",
};

export const radius = { sm: 4, md: 6, lg: 8, full: 9999 };

export function Box({
  children,
  style,
  label,
}: {
  children?: React.ReactNode;
  style?: React.CSSProperties;
  label?: string;
}) {
  return (
    <div
      className="g-card"
      style={{
        border: `0.5px solid ${colors.line}`,
        background: colors.bgSurface,
        borderRadius: radius.lg,
        boxShadow: elevation.level1,
        padding: 16,
        position: "relative",
        ...style,
      }}
    >
      {label && (
        <div
          style={{
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: ".03em",
            textTransform: "uppercase",
            color: colors.muted,
            marginBottom: 10,
          }}
        >
          {label}
        </div>
      )}
      {children}
    </div>
  );
}

export function Btn({
  children,
  primary,
  disabled,
  danger,
  secondary,
}: {
  children: React.ReactNode;
  primary?: boolean;
  disabled?: boolean;
  danger?: boolean;
  secondary?: boolean;
}) {
  // variant 결정: primary > danger > secondary > ghost(기본)
  const variant = primary
    ? "primary"
    : danger
      ? "danger"
      : secondary
        ? "secondary"
        : "ghost";

  const base: React.CSSProperties = {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    borderRadius: radius.sm,
    padding: "7px 16px",
    fontSize: 13,
    fontWeight: 600,
    lineHeight: 1,
    marginRight: 8,
    cursor: disabled ? "not-allowed" : "pointer",
    border: "1px solid transparent",
    opacity: disabled ? 0.38 : 1,
    pointerEvents: disabled ? "none" : "auto",
  };

  const variantStyle: Record<string, React.CSSProperties> = {
    primary: { background: colors.primary, color: colors.inverse, borderColor: colors.primary },
    secondary: { background: colors.bgSurface, color: colors.primary, borderColor: colors.primary },
    ghost: { background: "transparent", color: colors.text, borderColor: colors.line },
    danger: { background: colors.error, color: colors.inverse, borderColor: colors.error },
  };

  return (
    <button
      type="button"
      disabled={disabled}
      className={`g-btn g-btn--${variant}`}
      style={{ ...base, ...variantStyle[variant] }}
    >
      {children}
    </button>
  );
}

export function Field({
  label,
  placeholder,
  required,
  helper,
  error,
  textarea,
}: {
  label: string;
  placeholder?: string;
  required?: boolean;
  helper?: string;
  error?: string;
  textarea?: boolean;
}) {
  return (
    <div style={{ marginBottom: 16 }}>
      <div style={{ fontSize: 12, fontWeight: 600, color: colors.text, marginBottom: 4 }}>
        {label}
        {required && <span style={{ color: colors.error, marginLeft: 2 }}>*</span>}
      </div>
      <div
        className="g-field"
        tabIndex={0}
        style={{
          border: `1px solid ${error ? colors.error : colors.lineInput}`,
          borderRadius: radius.sm,
          padding: textarea ? 10 : "8px 12px",
          minHeight: textarea ? 100 : undefined,
          fontSize: 13,
          color: colors.muted,
          background: colors.bgSurface,
          outline: "none",
        }}
      >
        {placeholder || "________"}
      </div>
      {error ? (
        <div style={{ fontSize: 11, color: colors.error, marginTop: 4 }}>⚠ {error}</div>
      ) : helper ? (
        <div style={{ fontSize: 11, color: colors.muted, marginTop: 4 }}>{helper}</div>
      ) : null}
    </div>
  );
}

type BadgeColor = "default" | "info" | "success" | "warning" | "error" | "accent";

export function Badge({
  children,
  color = "default",
  dot,
}: {
  children: React.ReactNode;
  color?: BadgeColor;
  dot?: boolean;
}) {
  const map: Record<BadgeColor, { bg: string; fg: string; border?: string; dot: string }> = {
    default: { bg: colors.box, fg: colors.muted, border: colors.line, dot: colors.muted },
    info: { bg: "rgba(5,83,154,.1)", fg: colors.primary, dot: colors.primary },
    success: { bg: colors.successBg, fg: colors.successText, dot: colors.success },
    warning: { bg: colors.warningBg, fg: colors.warningText, dot: colors.warning },
    error: { bg: colors.errorBg, fg: colors.errorText, dot: colors.error },
    accent: { bg: "rgba(6,173,236,.12)", fg: "#0588BF", dot: colors.accent },
  };
  const m = map[color];
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 4,
        background: m.bg,
        color: m.fg,
        border: m.border ? `0.5px solid ${m.border}` : undefined,
        borderRadius: radius.full,
        padding: "2px 8px",
        fontSize: 11,
        fontWeight: 600,
      }}
    >
      {dot && (
        <span style={{ width: 6, height: 6, borderRadius: "50%", background: m.dot, display: "inline-block" }} />
      )}
      {children}
    </span>
  );
}

export function UrgencyBadge({ level }: { level: "HIGH" | "NORMAL" | "LOW" }) {
  const map = {
    HIGH: { color: "error" as const, t: "긴급" },
    NORMAL: { color: "warning" as const, t: "보통" },
    LOW: { color: "default" as const, t: "낮음" },
  };
  const m = map[level];
  // 상태칩은 닷 없이 통일
  return <Badge color={m.color}>{m.t}</Badge>;
}

// === 문의 유형 칩 (고객 선택 / AI 분류 공용) ===
// 유형별 색상을 구분해 한눈에 식별되도록 매핑
const TYPE_COLOR: Record<string, BadgeColor> = {
  결제: "info",
  아이템지급: "accent",
  아이템: "accent",
  계정: "warning",
  기타: "default",
};

export function TypeBadge({ type }: { type: string }) {
  return <Badge color={TYPE_COLOR[type] ?? "default"}>{type}</Badge>;
}

// === 상태 칩 (한글 라벨 기준 색상 매핑) ===
// 진행중=info(Primary), 완료=success, 대기=warning, 자동처리=default
const STATUS_COLOR: Record<string, BadgeColor> = {
  "접수": "default",
  "접수·분석중": "default",
  "AI분석중": "default",
  "담당자배정대기": "warning",
  "운영자확인중": "info",
  "승인완료": "success",
  "발송완료": "success",
  "수동분류대기": "warning",
};

export function StatusBadge({ status }: { status: string }) {
  return <Badge color={STATUS_COLOR[status] ?? "default"}>{status}</Badge>;
}

// === Select (드롭다운) — 디자인 가이드 Input 스타일 + chevron ===
export function Select({
  label,
  value,
  placeholder,
  width,
}: {
  label?: string;
  value?: string;
  placeholder?: string;
  width?: number;
}) {
  const filled = !!value;
  return (
    <div
      className="g-field"
      tabIndex={0}
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: 8,
        border: `1px solid ${colors.lineInput}`,
        borderRadius: radius.sm,
        padding: "6px 10px",
        fontSize: 12,
        background: colors.bgSurface,
        color: filled ? colors.text : colors.muted,
        minWidth: width ?? 110,
        outline: "none",
        cursor: "pointer",
      }}
    >
      <span>
        {label && <span style={{ color: colors.muted, marginRight: 4 }}>{label}</span>}
        {value || placeholder}
      </span>
      <span style={{ color: colors.muted, fontSize: 10 }}>▾</span>
    </div>
  );
}

// === 검색 인풋 (prefix search icon) ===
export function SearchBox({ placeholder, width }: { placeholder?: string; width?: number }) {
  return (
    <div
      className="g-field"
      tabIndex={0}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        border: `1px solid ${colors.lineInput}`,
        borderRadius: radius.sm,
        padding: "6px 10px",
        fontSize: 12,
        background: colors.bgSurface,
        color: colors.muted,
        width: width ?? 200,
        outline: "none",
      }}
    >
      <span style={{ fontSize: 13 }}>🔍</span>
      <span>{placeholder || "검색"}</span>
    </div>
  );
}

// === 페이지네이션 (디자인 가이드 .pg — 현재 페이지 Primary) ===
export function Pagination({
  current = 1,
  total = 3,
  totalElements,
  pageSize = 20,
}: {
  current?: number;
  total?: number;
  totalElements?: number;
  pageSize?: number;
}) {
  const pages = Array.from({ length: total }, (_, i) => i + 1);
  const pg: React.CSSProperties = {
    width: 30,
    height: 30,
    borderRadius: radius.sm,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 12,
    fontWeight: 500,
    cursor: "pointer",
    border: `0.5px solid ${colors.line}`,
    background: colors.bgSurface,
    color: colors.muted,
  };
  const cur: React.CSSProperties = {
    ...pg,
    background: colors.primary,
    color: colors.inverse,
    borderColor: colors.primary,
  };
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 12, marginTop: 14 }}>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <span style={pg}>◀</span>
        {pages.map((p) => (
          <span key={p} style={p === current ? cur : pg}>
            {p}
          </span>
        ))}
        <span style={pg}>▶</span>
      </div>
      {totalElements != null && (
        <span style={{ fontSize: 12, color: colors.muted }}>
          총 {totalElements}건 · 페이지당 {pageSize}건
        </span>
      )}
    </div>
  );
}

export function ScreenTitle({ children, api }: { children: React.ReactNode; api?: string }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, letterSpacing: "-0.01em", color: colors.text }}>
        {children}
      </h1>
      {api && (
        <div
          style={{
            display: "inline-block",
            fontSize: 11,
            color: colors.primary,
            marginTop: 6,
            fontFamily: "'DM Sans', monospace",
            background: colors.primary50,
            padding: "2px 8px",
            borderRadius: radius.sm,
          }}
        >
          API: {api}
        </div>
      )}
    </div>
  );
}
