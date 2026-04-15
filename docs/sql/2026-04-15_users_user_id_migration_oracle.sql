-- date: 2026-04-15
-- desc: users.user_id column migration + dummy data seed (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'USERS'
       AND column_name = 'USER_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE users ADD (user_id VARCHAR2(100))';
    END IF;
END;
/

UPDATE users
   SET user_id = NVL(
       user_id,
       CASE
           WHEN email IS NOT NULL AND INSTR(email, '@') > 1 THEN LOWER(SUBSTR(email, 1, INSTR(email, '@') - 1))
           ELSE 'user' || TO_CHAR(id)
       END
   )
 WHERE user_id IS NULL;

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_constraints
     WHERE constraint_name = 'UK_USERS_USER_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE users ADD CONSTRAINT uk_users_user_id UNIQUE (user_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'USERS'
       AND column_name = 'USER_ID'
       AND nullable = 'N';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE users MODIFY (user_id VARCHAR2(100) NOT NULL)';
    END IF;
END;
/

MERGE INTO users target
USING (
    SELECT 9001 AS id, 'user01' AS user_id, 'user01@daily.com' AS email, '1234' AS password, 'Normal User' AS name, 'USER' AS role, 'ACTIVE' AS status
    FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.user_id = source.user_id,
        target.email = source.email,
        target.password = source.password,
        target.name = source.name,
        target.role = source.role,
        target.status = source.status,
        target.updated_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, user_id, email, password, name, role, status, created_at, updated_at)
    VALUES (source.id, source.user_id, source.email, source.password, source.name, source.role, source.status, SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO users target
USING (
    SELECT 9002 AS id, 'admin01' AS user_id, 'admin01@daily.com' AS email, '1234' AS password, 'Admin User' AS name, 'ADMIN' AS role, 'ACTIVE' AS status
    FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.user_id = source.user_id,
        target.email = source.email,
        target.password = source.password,
        target.name = source.name,
        target.role = source.role,
        target.status = source.status,
        target.updated_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, user_id, email, password, name, role, status, created_at, updated_at)
    VALUES (source.id, source.user_id, source.email, source.password, source.name, source.role, source.status, SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;
