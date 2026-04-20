-- @date 2026-04-17
-- @desc 기존 크롤링 관리 스키마에 조건 프리셋/필터 옵션 컬럼을 확장합니다.

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'KEYWORD_MATCH_TYPE';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE q'[ALTER TABLE crawl_schedule ADD (keyword_match_type VARCHAR2(10) DEFAULT 'OR' NOT NULL)]';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'INCLUDE_KEYWORD_OPERATORS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (include_keyword_operators VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'EXCLUDE_KEYWORDS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (exclude_keywords VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'INCLUDE_KEYWORDS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (include_keywords VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'TARGET_DOMAINS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (target_domains VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'CONNECT_TIMEOUT_SECONDS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (connect_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL)';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'READ_TIMEOUT_SECONDS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (read_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL)';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_SCHEDULE' AND column_name = 'RETRY_COUNT';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_schedule ADD (retry_count NUMBER(10) DEFAULT 1 NOT NULL)';
    END IF;
END;
/

DECLARE
    v_table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_table_count FROM user_tables WHERE table_name = 'CRAWL_CONDITION_PRESET';

    IF v_table_count = 0 THEN
        EXECUTE IMMEDIATE q'[
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
            )
        ]';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_CONDITION_PRESET' AND column_name = 'EXCLUDE_KEYWORDS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_condition_preset ADD (exclude_keywords VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'CRAWL_CONDITION_PRESET' AND column_name = 'INCLUDE_KEYWORD_OPERATORS';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE crawl_condition_preset ADD (include_keyword_operators VARCHAR2(2000))';
    END IF;
END;
/

DECLARE
    v_sequence_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_sequence_count FROM user_sequences WHERE sequence_name = 'SEQ_CRAWL_CONDITION_PRESET';

    IF v_sequence_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_crawl_condition_preset START WITH 1 INCREMENT BY 1 NOCACHE';
    END IF;
END;
/

DECLARE
    v_index_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_index_count
    FROM user_ind_columns
    WHERE table_name = 'TECH_NEWS'
      AND column_name = 'URL';

    IF v_index_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tech_news_url ON tech_news (url)';
    END IF;
END;
/
