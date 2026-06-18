import { Box, Btn, Field, ScreenTitle, colors } from "../wireframe-kit";

// US-01, US-02 / POST /api/v1/auth/login
export default function LoginScreen() {
  return (
    <div>
      <ScreenTitle api="POST /api/v1/auth/login">운영자 로그인</ScreenTitle>
      <div style={{ display: "flex", justifyContent: "center", paddingTop: 40 }}>
        <Box style={{ width: 360 }}>
          <div
            style={{
              textAlign: "center",
              marginBottom: 24,
              fontSize: 20,
              fontWeight: 600,
              color: colors.primary,
            }}
          >
            AI CS 문의 처리 에이전트
          </div>
          <Field label="아이디" placeholder="username" required />
          <Field label="비밀번호" placeholder="••••••••" required />
          <div style={{ marginTop: 16 }}>
            <Btn primary>로그인</Btn>
          </div>
          <div style={{ marginTop: 12, textAlign: "right" }}>
            <span
              style={{
                fontSize: 12,
                color: colors.primary,
                cursor: "pointer",
                textDecoration: "underline",
                textDecorationColor: colors.accent,
              }}
            >
              비밀번호 찾기
            </span>
          </div>
          <div style={{ fontSize: 11, color: colors.error, marginTop: 12, lineHeight: 1.6 }}>
            * 실패 시: "아이디 또는 비밀번호가 올바르지 않습니다" / 5회 실패 시 계정 잠금
          </div>
        </Box>
      </div>
    </div>
  );
}
