-- @date 2026-04-23
-- @desc generation 예약 중복 정책(allow_duplicate) 및 SKIPPED 이력 상태 지원 마이그레이션

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'GENERATION_SCHEDULE'
       AND column_name = 'ALLOW_DUPLICATE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE generation_schedule ADD (allow_duplicate NUMBER(1) DEFAULT 0 NOT NULL)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = 'GENERATION_SCHEDULE'
       AND constraint_name = 'CK_GENERATION_SCHEDULE_ALLOW_DUPLICATE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE generation_schedule ADD CONSTRAINT ck_generation_schedule_allow_duplicate CHECK (allow_duplicate IN (0, 1))';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = 'GENERATION_HISTORY'
       AND constraint_name = 'CK_GENERATION_HISTORY_STATUS';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE generation_history DROP CONSTRAINT ck_generation_history_status';
    END IF;
END;
/

ALTER TABLE generation_history
    ADD CONSTRAINT ck_generation_history_status
    CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED'));

COMMIT;
