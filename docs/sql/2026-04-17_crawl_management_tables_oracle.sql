-- @date 2026-04-17
-- @desc 크롤링 관리 기능용 스케줄/이력 테이블 및 시퀀스를 생성합니다.

CREATE TABLE crawl_schedule (
    id NUMBER(19) NOT NULL,
    is_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    allow_duplicate NUMBER(1) DEFAULT 0 NOT NULL,
    cron_expression VARCHAR2(120) NOT NULL,
    source_name VARCHAR2(100) NOT NULL,
    source_url VARCHAR2(500) NOT NULL,
    max_articles NUMBER(10) DEFAULT 20 NOT NULL,
    keyword_match_type VARCHAR2(10) DEFAULT 'OR' NOT NULL,
    include_keywords VARCHAR2(2000),
    include_keyword_operators VARCHAR2(2000),
    exclude_keywords VARCHAR2(2000),
    target_domains VARCHAR2(2000),
    connect_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL,
    read_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL,
    retry_count NUMBER(10) DEFAULT 1 NOT NULL,
    last_executed_at TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_crawl_schedule PRIMARY KEY (id)
);

CREATE TABLE crawl_history (
    id NUMBER(19) NOT NULL,
    trigger_type VARCHAR2(20) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR2(20) NOT NULL,
    source_name VARCHAR2(100) NOT NULL,
    requested_count NUMBER(10) NOT NULL,
    collected_count NUMBER(10) NOT NULL,
    inserted_count NUMBER(10) NOT NULL,
    error_message VARCHAR2(1000),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_crawl_history PRIMARY KEY (id)
);

CREATE SEQUENCE seq_crawl_history START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE crawl_condition_preset (
    id NUMBER(19) NOT NULL,
    preset_name VARCHAR2(100) NOT NULL,
    source_name VARCHAR2(100) NOT NULL,
    source_url VARCHAR2(500) NOT NULL,
    max_articles NUMBER(10) NOT NULL,
    keyword_match_type VARCHAR2(10) DEFAULT 'OR' NOT NULL,
    include_keywords VARCHAR2(2000),
    include_keyword_operators VARCHAR2(2000),
    exclude_keywords VARCHAR2(2000),
    target_domains VARCHAR2(2000),
    connect_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL,
    read_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL,
    retry_count NUMBER(10) DEFAULT 1 NOT NULL,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_crawl_condition_preset PRIMARY KEY (id)
);

CREATE SEQUENCE seq_crawl_condition_preset START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE INDEX idx_tech_news_url ON tech_news (url);
