import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// 기획자 전용 와이어프레임 프로젝트.
// 개발자 frontend(5173)와 충돌하지 않도록 포트 5174 사용.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
  },
});
