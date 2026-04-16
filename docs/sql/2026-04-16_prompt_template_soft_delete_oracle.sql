-- date: 2026-04-16
-- desc: add soft delete column for prompt_template (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'PROMPT_TEMPLATE'
      AND column_name = 'IS_DELETED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE prompt_template ADD is_deleted NUMBER(1) DEFAULT 0 NOT NULL';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'CK_PROMPT_TEMPLATE_DELETED';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE prompt_template ADD CONSTRAINT ck_prompt_template_deleted CHECK (is_deleted IN (0, 1))';
    END IF;
END;
/

UPDATE prompt_template
SET is_deleted = 0
WHERE is_deleted IS NULL;

COMMIT;
