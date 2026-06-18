import { Box, Btn, Field, ScreenTitle, colors } from "../wireframe-kit";

// US-03~05 / POST /api/v1/inquiries (인증 불필요, 고객용)
export default function InquiryFormScreen() {
  return (
    <div>
      <ScreenTitle api="POST /api/v1/inquiries (인증 불필요)">고객 문의 접수 폼</ScreenTitle>
      <div style={{ display: "flex", justifyContent: "center", paddingTop: 20 }}>
        <Box style={{ width: 480 }}>
          <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 20 }}>문의하기</div>

          {/* 게임 로그인 상태에서 진입 → 유저 정보 자동 입력(읽기 전용) */}
          <div
            style={{
              display: "flex",
              gap: 16,
              padding: "10px 12px",
              background: colors.box,
              border: `1px solid ${colors.line}`,
              borderRadius: 4,
              marginBottom: 16,
              fontSize: 13,
            }}
          >
            <div>
              <div style={{ fontSize: 11, color: colors.muted, marginBottom: 2 }}>유저 ID</div>
              <div style={{ fontWeight: 600 }}>user12345</div>
            </div>
            <div style={{ width: 1, background: colors.line }} />
            <div>
              <div style={{ fontSize: 11, color: colors.muted, marginBottom: 2 }}>닉네임</div>
              <div style={{ fontWeight: 600 }}>홍길동</div>
            </div>
            <div style={{ flex: 1 }} />
            <span style={{ alignSelf: "center", fontSize: 11, color: colors.muted }}>🔒 로그인 정보 자동 입력</span>
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 4 }}>
              문의 유형 <span style={{ color: colors.error }}>*</span>
            </div>
            <div
              style={{
                border: `1px solid ${colors.lineInput}`,
                borderRadius: 4,
                padding: "8px 12px",
                fontSize: 13,
                color: colors.muted,
                background: colors.bgSurface,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
              }}
            >
              <span>결제 / 아이템지급 / 계정 / 기타</span>
              <span>▾</span>
            </div>
          </div>
          <Field
            label="문의 내용"
            required
            textarea
            placeholder="결제했는데 아이템이 지급되지 않았습니다..."
            helper="최소 10자 이상 입력해 주세요"
          />
          <div style={{ marginTop: 8 }}>
            <Btn primary>문의 제출</Btn>
          </div>
          <div style={{ fontSize: 11, color: colors.muted, marginTop: 12 }}>
            * 제출 성공 시: 문의 ID 표시 + "접수 완료" 메시지
          </div>
        </Box>
      </div>
    </div>
  );
}
