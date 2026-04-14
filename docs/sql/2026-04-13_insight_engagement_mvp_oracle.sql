-- @date 2026-04-13
-- @desc Insight 상세 MVP(좋아요/북마크/댓글/조회수)용 Oracle DDL

--------------------------------------------------------------------------------
-- 1) tech_news.view_count 추가 (조회수 집계용)
--------------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'TECH_NEWS'
       AND column_name = 'VIEW_COUNT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE tech_news ADD (view_count NUMBER(19) DEFAULT 0 NOT NULL)';
    END IF;
END;
/

--------------------------------------------------------------------------------
-- 2) insight_like 테이블
--------------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tables
     WHERE table_name = 'INSIGHT_LIKE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE insight_like (
                id            NUMBER(19)     NOT NULL,
                content_type  VARCHAR2(20)   NOT NULL,
                content_id    NUMBER(19)     NOT NULL,
                user_id       NUMBER(19)     NOT NULL,
                created_at    TIMESTAMP(6)   NOT NULL,
                CONSTRAINT pk_insight_like PRIMARY KEY (id),
                CONSTRAINT ck_insight_like_type CHECK (content_type IN (''KNOWLEDGE'', ''NEWS'')),
                CONSTRAINT fk_insight_like_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
        ';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_sequences
     WHERE sequence_name = 'SEQ_INSIGHT_LIKE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_insight_like START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_insight_like_bi
BEFORE INSERT ON insight_like
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := seq_insight_like.NEXTVAL;
    END IF;
    IF :NEW.created_at IS NULL THEN
        :NEW.created_at := SYSTIMESTAMP;
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = 'INSIGHT_LIKE'
       AND constraint_name = 'UK_INSIGHT_LIKE_TARGET_USER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE insight_like ADD CONSTRAINT uk_insight_like_target_user UNIQUE (content_type, content_id, user_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_indexes
     WHERE index_name = 'IDX_INSIGHT_LIKE_TARGET';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_insight_like_target ON insight_like (content_type, content_id)';
    END IF;
END;
/

--------------------------------------------------------------------------------
-- 3) insight_bookmark 테이블
--------------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tables
     WHERE table_name = 'INSIGHT_BOOKMARK';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE insight_bookmark (
                id            NUMBER(19)     NOT NULL,
                content_type  VARCHAR2(20)   NOT NULL,
                content_id    NUMBER(19)     NOT NULL,
                user_id       NUMBER(19)     NOT NULL,
                created_at    TIMESTAMP(6)   NOT NULL,
                CONSTRAINT pk_insight_bookmark PRIMARY KEY (id),
                CONSTRAINT ck_insight_bookmark_type CHECK (content_type IN (''KNOWLEDGE'', ''NEWS'')),
                CONSTRAINT fk_insight_bookmark_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
        ';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_sequences
     WHERE sequence_name = 'SEQ_INSIGHT_BOOKMARK';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_insight_bookmark START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_insight_bookmark_bi
BEFORE INSERT ON insight_bookmark
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := seq_insight_bookmark.NEXTVAL;
    END IF;
    IF :NEW.created_at IS NULL THEN
        :NEW.created_at := SYSTIMESTAMP;
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = 'INSIGHT_BOOKMARK'
       AND constraint_name = 'UK_INSIGHT_BOOKMARK_TARGET_USER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE insight_bookmark ADD CONSTRAINT uk_insight_bookmark_target_user UNIQUE (content_type, content_id, user_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_indexes
     WHERE index_name = 'IDX_INSIGHT_BOOKMARK_USER_CREATED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_insight_bookmark_user_created ON insight_bookmark (user_id, created_at)';
    END IF;
END;
/

--------------------------------------------------------------------------------
-- 4) insight_comment 테이블
--------------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tables
     WHERE table_name = 'INSIGHT_COMMENT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE insight_comment (
                id            NUMBER(19)     NOT NULL,
                content_type  VARCHAR2(20)   NOT NULL,
                content_id    NUMBER(19)     NOT NULL,
                user_id       NUMBER(19)     NOT NULL,
                content       CLOB           NOT NULL,
                is_deleted    NUMBER(1)      DEFAULT 0 NOT NULL,
                created_at    TIMESTAMP(6)   NOT NULL,
                updated_at    TIMESTAMP(6)   NOT NULL,
                CONSTRAINT pk_insight_comment PRIMARY KEY (id),
                CONSTRAINT ck_insight_comment_type CHECK (content_type IN (''KNOWLEDGE'', ''NEWS'')),
                CONSTRAINT ck_insight_comment_deleted CHECK (is_deleted IN (0, 1)),
                CONSTRAINT fk_insight_comment_user FOREIGN KEY (user_id) REFERENCES users(id)
            )
        ';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_sequences
     WHERE sequence_name = 'SEQ_INSIGHT_COMMENT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_insight_comment START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_insight_comment_bi
BEFORE INSERT ON insight_comment
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := seq_insight_comment.NEXTVAL;
    END IF;
    IF :NEW.created_at IS NULL THEN
        :NEW.created_at := SYSTIMESTAMP;
    END IF;
    IF :NEW.updated_at IS NULL THEN
        :NEW.updated_at := SYSTIMESTAMP;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_insight_comment_bu
BEFORE UPDATE ON insight_comment
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_indexes
     WHERE index_name = 'IDX_INSIGHT_COMMENT_TARGET_CREATED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_insight_comment_target_created ON insight_comment (content_type, content_id, created_at)';
    END IF;
END;
/
