-- date: 2026-04-15
-- desc: insight_comment 대댓글(parent_comment_id) 컬럼 추가 (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'INSIGHT_COMMENT'
       AND column_name = 'PARENT_COMMENT_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE insight_comment ADD (parent_comment_id NUMBER(19))';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_constraints
     WHERE table_name = 'INSIGHT_COMMENT'
       AND constraint_name = 'FK_INSIGHT_COMMENT_PARENT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE insight_comment ADD CONSTRAINT fk_insight_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES insight_comment(id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_indexes
     WHERE index_name = 'IDX_INSIGHT_COMMENT_PARENT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_insight_comment_parent ON insight_comment (parent_comment_id)';
    END IF;
END;
/
