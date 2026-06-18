-- V2: 결제 시스템 데모 더미 데이터 (PaymentQueryStrategy 조회 근거, US-09)
-- 운영 시스템 연동 시 제거. 특정 userId로 문의하면 해당 결제/지급 이력이 진단 근거가 된다.

-- 시나리오 A) 결제 성공했으나 아이템 미지급 (대표 데모: "결제했는데 아이템이 안 와요")
INSERT INTO payment (id, user_id, amount, status, error_log, created_at) VALUES
  (gen_random_uuid(), 'demo-pay-ok', 9900, 'SUCCESS', NULL, now());
INSERT INTO item_delivery (id, payment_id, user_id, item_id, status, created_at) VALUES
  (gen_random_uuid(), NULL, 'demo-pay-ok', 'item-sword-100', 'NOT_DELIVERED', now());

-- 시나리오 B) 결제 실패 (한도 초과)
INSERT INTO payment (id, user_id, amount, status, error_log, created_at) VALUES
  (gen_random_uuid(), 'demo-pay-fail', 4900, 'FAILED', 'CARD_LIMIT_EXCEEDED: 카드 한도 초과', now());

-- 시나리오 C) 중복 결제 (동일 유저 2건 성공, 1건 지급)
INSERT INTO payment (id, user_id, amount, status, error_log, created_at) VALUES
  (gen_random_uuid(), 'demo-dup', 9900, 'SUCCESS', NULL, now() - interval '5 minutes'),
  (gen_random_uuid(), 'demo-dup', 9900, 'SUCCESS', NULL, now());
INSERT INTO item_delivery (id, payment_id, user_id, item_id, status, created_at) VALUES
  (gen_random_uuid(), NULL, 'demo-dup', 'item-gold-500', 'DELIVERED', now());
