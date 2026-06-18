-- V1 베이스라인 (플레이스홀더)
-- 실제 스키마는 백엔드 A가 도메인 엔티티 확정 후 작성한다.
-- domain-entities.md 기준: inquiry, ai_analysis, diagnosis, draft_response,
--   approval_history, operator, payment(더미), item_delivery(더미), account(더미)
-- 모든 timestamp 컬럼은 timestamptz (KST 운영, BR-41).

-- 예시(주석): 백엔드 A가 구체화
-- CREATE TABLE operator ( ... );
-- CREATE TABLE inquiry ( ... );

SELECT 1;
