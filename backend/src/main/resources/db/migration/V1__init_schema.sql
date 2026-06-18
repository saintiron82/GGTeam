-- =====================================================================
-- V1: 초기 스키마 (AI 기반 CS 문의 처리 에이전트)
-- =====================================================================
-- 기준: domain-entities.md §2~3, business-rules.md
-- 원칙:
--   * 모든 timestamp 컬럼은 timestamptz (KST 운영, BR-41)
--   * UUID PK (애플리케이션 생성, GenerationType.UUID)
--   * enum은 문자열(varchar) 저장 (@Enumerated(STRING))
--   * JSON 컬럼은 jsonb
--   * JPA ddl-auto=validate 와 1:1 일치 (엔티티 변경 시 본 파일도 동기화)
-- =====================================================================

-- ---------------------------------------------------------------------
-- operator : 운영자(상담원/관리자) — domain §2.6
-- ---------------------------------------------------------------------
CREATE TABLE operator (
    id                  uuid          NOT NULL,
    username            varchar(100)  NOT NULL,
    password_hash       varchar(255)  NOT NULL,
    role                varchar(20)   NOT NULL,
    failed_login_count  integer       NOT NULL DEFAULT 0,
    locked              boolean       NOT NULL DEFAULT false,
    CONSTRAINT pk_operator PRIMARY KEY (id),
    CONSTRAINT uq_operator_username UNIQUE (username)
);

-- ---------------------------------------------------------------------
-- inquiry : 고객 문의 루트 — domain §2.1
-- ---------------------------------------------------------------------
CREATE TABLE inquiry (
    id                    uuid          NOT NULL,
    customer_info         jsonb         NOT NULL,
    customer_type         varchar(20)   NOT NULL,
    content               text          NOT NULL,
    status                varchar(40)   NOT NULL,
    created_at            timestamptz   NOT NULL,
    assigned_operator_id  uuid          NULL,
    CONSTRAINT pk_inquiry PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_operator FOREIGN KEY (assigned_operator_id) REFERENCES operator (id)
);

-- Pull 배정 후보 조회(status) 및 운영자별 조회 인덱스
CREATE INDEX idx_inquiry_status ON inquiry (status);
CREATE INDEX idx_inquiry_assigned_operator ON inquiry (assigned_operator_id);

-- ---------------------------------------------------------------------
-- ai_analysis : AI 분석 결과 — domain §2.2 (문의당 1건, BR-39)
-- ---------------------------------------------------------------------
CREATE TABLE ai_analysis (
    id                    uuid          NOT NULL,
    inquiry_id            uuid          NOT NULL,
    ai_type               varchar(20)   NOT NULL,
    sub_category          varchar(100)  NULL,
    urgency               varchar(20)   NOT NULL,
    summary               text          NULL,
    keywords              jsonb         NULL,
    system_query_result   jsonb         NULL,
    analyzed_at           timestamptz   NULL,
    failure_type          varchar(20)   NULL,
    CONSTRAINT pk_ai_analysis PRIMARY KEY (id),
    CONSTRAINT uq_ai_analysis_inquiry UNIQUE (inquiry_id),
    CONSTRAINT fk_ai_analysis_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id)
);

-- ---------------------------------------------------------------------
-- diagnosis : 진단 — domain §2.3 (문의당 1건, BR-39)
-- ---------------------------------------------------------------------
CREATE TABLE diagnosis (
    id                    uuid              NOT NULL,
    inquiry_id            uuid              NOT NULL,
    cause                 text              NOT NULL,
    suggested_direction   text              NOT NULL,
    confidence            double precision  NOT NULL,
    CONSTRAINT pk_diagnosis PRIMARY KEY (id),
    CONSTRAINT uq_diagnosis_inquiry UNIQUE (inquiry_id),
    CONSTRAINT fk_diagnosis_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id)
);

-- ---------------------------------------------------------------------
-- draft_response : 답변 초안 — domain §2.4 (문의당 1:N, 최신=current)
-- ---------------------------------------------------------------------
CREATE TABLE draft_response (
    id                    uuid          NOT NULL,
    inquiry_id            uuid          NOT NULL,
    content               text          NOT NULL,
    status                varchar(20)   NOT NULL,
    regeneration_count    integer       NOT NULL DEFAULT 0,
    created_at            timestamptz   NOT NULL,
    CONSTRAINT pk_draft_response PRIMARY KEY (id),
    CONSTRAINT fk_draft_response_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id)
);

CREATE INDEX idx_draft_response_inquiry ON draft_response (inquiry_id);

-- ---------------------------------------------------------------------
-- approval_history : 승인/처리 이력 — domain §2.5 (append-only, BR-40)
-- ---------------------------------------------------------------------
CREATE TABLE approval_history (
    id            uuid          NOT NULL,
    inquiry_id    uuid          NOT NULL,
    action        varchar(20)   NOT NULL,
    operator_id   uuid          NOT NULL,
    reason        text          NULL,
    timestamp     timestamptz   NOT NULL,
    CONSTRAINT pk_approval_history PRIMARY KEY (id),
    CONSTRAINT fk_approval_history_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiry (id),
    CONSTRAINT fk_approval_history_operator FOREIGN KEY (operator_id) REFERENCES operator (id)
);

CREATE INDEX idx_approval_history_inquiry ON approval_history (inquiry_id);

-- =====================================================================
-- 데모 더미(Mock) 시스템 테이블 — domain §3
-- 운영 시스템 연동 시 대체. userId 기반 논리 연결.
-- =====================================================================

-- ---------------------------------------------------------------------
-- payment : 결제(더미) — domain §3.1
-- ---------------------------------------------------------------------
CREATE TABLE payment (
    id            uuid           NOT NULL,
    user_id       varchar(100)   NOT NULL,
    amount        numeric(19, 2) NOT NULL,
    status        varchar(20)    NOT NULL,
    error_log     text           NULL,
    created_at    timestamptz    NOT NULL,
    CONSTRAINT pk_payment PRIMARY KEY (id)
);

CREATE INDEX idx_payment_user ON payment (user_id);

-- ---------------------------------------------------------------------
-- item_delivery : 아이템 지급(더미) — domain §3.2
-- ---------------------------------------------------------------------
CREATE TABLE item_delivery (
    id            uuid          NOT NULL,
    payment_id    uuid          NULL,
    user_id       varchar(100)  NOT NULL,
    item_id       varchar(100)  NOT NULL,
    status        varchar(20)   NOT NULL,
    created_at    timestamptz   NOT NULL,
    CONSTRAINT pk_item_delivery PRIMARY KEY (id),
    CONSTRAINT fk_item_delivery_payment FOREIGN KEY (payment_id) REFERENCES payment (id)
);

CREATE INDEX idx_item_delivery_user ON item_delivery (user_id);
CREATE INDEX idx_item_delivery_payment ON item_delivery (payment_id);

-- ---------------------------------------------------------------------
-- account : 계정(더미) — domain §3.3
-- ---------------------------------------------------------------------
CREATE TABLE account (
    id            uuid          NOT NULL,
    user_id       varchar(100)  NOT NULL,
    status        varchar(20)   NOT NULL,
    last_login    timestamptz   NULL,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uq_account_user UNIQUE (user_id)
);
