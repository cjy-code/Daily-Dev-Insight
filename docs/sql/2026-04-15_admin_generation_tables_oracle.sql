-- date: 2026-04-15
-- desc: admin tables + sequences + triggers + seed data (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'PROMPT_TEMPLATE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE prompt_template (
                id NUMBER(19) NOT NULL,
                name VARCHAR2(100) NOT NULL,
                description VARCHAR2(500),
                template_content CLOB NOT NULL,
                is_active NUMBER(1) DEFAULT 0 NOT NULL,
                created_at TIMESTAMP(6) NOT NULL,
                updated_at TIMESTAMP(6) NOT NULL,
                CONSTRAINT pk_prompt_template PRIMARY KEY (id),
                CONSTRAINT ck_prompt_template_active CHECK (is_active IN (0, 1))
            )';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_sequences WHERE sequence_name = 'SEQ_PROMPT_TEMPLATE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_prompt_template START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_prompt_template_bi
BEFORE INSERT ON prompt_template
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := seq_prompt_template.NEXTVAL;
    END IF;
    IF :NEW.created_at IS NULL THEN
        :NEW.created_at := SYSTIMESTAMP;
    END IF;
    IF :NEW.updated_at IS NULL THEN
        :NEW.updated_at := SYSTIMESTAMP;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_prompt_template_bu
BEFORE UPDATE ON prompt_template
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'GENERATION_SCHEDULE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE generation_schedule (
                id NUMBER(19) NOT NULL,
                is_enabled NUMBER(1) DEFAULT 0 NOT NULL,
                cron_expression VARCHAR2(120) NOT NULL,
                category VARCHAR2(50) NOT NULL,
                tone VARCHAR2(50) NOT NULL,
                difficulty VARCHAR2(50) NOT NULL,
                last_executed_at TIMESTAMP(6),
                updated_at TIMESTAMP(6) NOT NULL,
                CONSTRAINT pk_generation_schedule PRIMARY KEY (id),
                CONSTRAINT ck_generation_schedule_enabled CHECK (is_enabled IN (0, 1))
            )';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_generation_schedule_bu
BEFORE UPDATE ON generation_schedule
FOR EACH ROW
BEGIN
    :NEW.updated_at := SYSTIMESTAMP;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'GENERATION_HISTORY';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE generation_history (
                id NUMBER(19) NOT NULL,
                trigger_type VARCHAR2(20) NOT NULL,
                target_date DATE NOT NULL,
                status VARCHAR2(20) NOT NULL,
                prompt_template_id NUMBER(19),
                created_knowledge_id NUMBER(19),
                title VARCHAR2(255),
                prompt_snapshot CLOB,
                error_message VARCHAR2(1000),
                created_at TIMESTAMP(6) NOT NULL,
                CONSTRAINT pk_generation_history PRIMARY KEY (id),
                CONSTRAINT ck_generation_history_trigger CHECK (trigger_type IN (''MANUAL'', ''SCHEDULED'')),
                CONSTRAINT ck_generation_history_status CHECK (status IN (''SUCCESS'', ''FAILED''))
            )';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_sequences WHERE sequence_name = 'SEQ_GENERATION_HISTORY';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_generation_history START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_generation_history_bi
BEFORE INSERT ON generation_history
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        :NEW.id := seq_generation_history.NEXTVAL;
    END IF;
    IF :NEW.created_at IS NULL THEN
        :NEW.created_at := SYSTIMESTAMP;
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_GENERATION_HISTORY_CREATED_AT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_generation_history_created_at ON generation_history (created_at DESC)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'FK_GEN_HISTORY_PROMPT_TEMPLATE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE generation_history ADD CONSTRAINT fk_gen_history_prompt_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_template(id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'FK_GEN_HISTORY_DAILY_KNOWLEDGE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE generation_history ADD CONSTRAINT fk_gen_history_daily_knowledge FOREIGN KEY (created_knowledge_id) REFERENCES daily_knowledge(id)';
    END IF;
END;
/

INSERT INTO prompt_template (
    id, name, description, template_content, is_active, created_at, updated_at
)
SELECT
    1,
    'Daily Knowledge Default Template',
    'Default template used for admin manual/scheduled generation',
    q'~You are a senior developer educator.
Create one daily development knowledge article for ${date}.
Category: ${category}
Tone: ${tone}
Difficulty: ${difficulty}
Respond format:
TITLE:
SUMMARY:
DETAIL:~',
    1,
    SYSTIMESTAMP,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM prompt_template WHERE id = 1);

INSERT INTO generation_schedule (
    id, is_enabled, cron_expression, category, tone, difficulty, last_executed_at, updated_at
)
SELECT
    1,
    0,
    '0 0 9 * * *',
    'Backend',
    'Practical',
    'Intermediate',
    NULL,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM generation_schedule WHERE id = 1);

INSERT INTO generation_history (
    id, trigger_type, target_date, status, prompt_template_id, created_knowledge_id, title, prompt_snapshot, error_message, created_at
)
SELECT
    1,
    'MANUAL',
    TRUNC(SYSDATE),
    'SUCCESS',
    1,
    NULL,
    'Sample generation result',
    'Rendered prompt snapshot sample',
    NULL,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM generation_history WHERE id = 1);

COMMIT;
