-- date: 2026-04-15
-- desc: daily_knowledge attachment_image_path column migration + today dummy data upsert (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'DAILY_KNOWLEDGE'
       AND column_name = 'ATTACHMENT_IMAGE_PATH';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE daily_knowledge ADD (attachment_image_path VARCHAR2(500))';
    END IF;
END;
/

DECLARE
    v_today_id NUMBER;
BEGIN
    SELECT MAX(id)
      INTO v_today_id
      FROM daily_knowledge
     WHERE knowledge_date = TRUNC(SYSDATE);

    IF v_today_id IS NULL THEN
        INSERT INTO daily_knowledge (
            id,
            knowledge_date,
            category,
            title,
            attachment_image_path,
            summary,
            detail,
            view_count,
            created_at
        )
        VALUES (
            990001,
            TRUNC(SYSDATE),
            'Backend',
            'Redis Basics: Why an in-memory data store matters',
            '/images/knowledge/redis.png',
            'Redis provides extremely fast reads and writes in memory, making it useful for caching, session storage, realtime counters, and queue-style workloads.',
            'Redis is more than a key-value cache. It supports multiple data structures such as String, Hash, List, Set, Sorted Set, and Stream, which helps model data for specific use cases.\n\nIn production, teams commonly use Redis to reduce database load, store auth/session tokens, and handle burst traffic with queue patterns.\n\nFor stable operation, tune expiration policy, memory limits, and persistence strategy (RDB/AOF) based on workload characteristics. Also design for hot-key and cache-stampede prevention.',
            250,
            SYSTIMESTAMP
        );
    ELSE
        UPDATE daily_knowledge
           SET category = 'Backend',
               title = 'Redis Basics: Why an in-memory data store matters',
               attachment_image_path = '/images/knowledge/redis.png',
               summary = 'Redis provides extremely fast reads and writes in memory, making it useful for caching, session storage, realtime counters, and queue-style workloads.',
               detail = 'Redis is more than a key-value cache. It supports multiple data structures such as String, Hash, List, Set, Sorted Set, and Stream, which helps model data for specific use cases.\n\nIn production, teams commonly use Redis to reduce database load, store auth/session tokens, and handle burst traffic with queue patterns.\n\nFor stable operation, tune expiration policy, memory limits, and persistence strategy (RDB/AOF) based on workload characteristics. Also design for hot-key and cache-stampede prevention.',
               view_count = NVL(view_count, 0) + 1,
               created_at = NVL(created_at, SYSTIMESTAMP)
         WHERE id = v_today_id;
    END IF;
END;
/

COMMIT;
