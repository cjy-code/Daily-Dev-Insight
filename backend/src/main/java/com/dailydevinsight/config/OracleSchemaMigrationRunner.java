package com.dailydevinsight.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OracleSchemaMigrationRunner {

    private static final String TABLE_INSIGHT_COMMENT = "INSIGHT_COMMENT";
    private static final String COLUMN_PARENT_COMMENT_ID = "PARENT_COMMENT_ID";
    private static final String CONSTRAINT_PARENT_COMMENT = "FK_INSIGHT_COMMENT_PARENT";
    private static final String INDEX_PARENT_COMMENT = "IDX_INSIGHT_COMMENT_PARENT";
    private static final String INDEX_INSIGHT_COMMENT_CONTENT_LOOKUP = "IDX_INSIGHT_COMMENT_CONTENT";
    private static final String TABLE_TECH_NEWS = "TECH_NEWS";
    private static final String TABLE_DAILY_KNOWLEDGE = "DAILY_KNOWLEDGE";
    private static final String TABLE_PROMPT_TEMPLATE = "PROMPT_TEMPLATE";
    private static final String TABLE_GENERATION_SCHEDULE = "GENERATION_SCHEDULE";
    private static final String TABLE_GENERATION_HISTORY = "GENERATION_HISTORY";
    private static final String TABLE_CRAWL_SCHEDULE = "CRAWL_SCHEDULE";
    private static final String TABLE_CRAWL_HISTORY = "CRAWL_HISTORY";
    private static final String TABLE_CRAWL_CONDITION_PRESET = "CRAWL_CONDITION_PRESET";
    private static final String TABLE_WEEKLY_AI_INSIGHT = "WEEKLY_AI_INSIGHT";
    private static final String TABLE_DAILY_TREND_INSIGHT = "DAILY_TREND_INSIGHT";
    private static final String TABLE_DAILY_TREND_GENERATION_HISTORY = "DAILY_TREND_GENERATION_HISTORY";
    private static final String COLUMN_ATTACHMENT_IMAGE_PATH = "ATTACHMENT_IMAGE_PATH";
    private static final String COLUMN_ALLOW_DUPLICATE = "ALLOW_DUPLICATE";
    private static final String COLUMN_EXCLUDE_KEYWORDS = "EXCLUDE_KEYWORDS";
    private static final String COLUMN_INCLUDE_KEYWORD_OPERATORS = "INCLUDE_KEYWORD_OPERATORS";
    private static final String COLUMN_USED_TREND_ID = "USED_TREND_ID";
    private static final String SEQUENCE_PROMPT_TEMPLATE = "SEQ_PROMPT_TEMPLATE";
    private static final String SEQUENCE_GENERATION_HISTORY = "SEQ_GENERATION_HISTORY";
    private static final String SEQUENCE_CRAWL_HISTORY = "SEQ_CRAWL_HISTORY";
    private static final String SEQUENCE_CRAWL_CONDITION_PRESET = "SEQ_CRAWL_CONDITION_PRESET";
    private static final String SEQUENCE_WEEKLY_AI_INSIGHT = "SEQ_WEEKLY_AI_INSIGHT";
    private static final String SEQUENCE_DAILY_TREND_INSIGHT = "SEQ_DAILY_TREND_INSIGHT";
    private static final String SEQUENCE_DAILY_TREND_GENERATION_HISTORY = "SEQ_DAILY_TREND_GEN_HISTORY";
    private static final String INDEX_TECH_NEWS_URL = "IDX_TECH_NEWS_URL";
    private static final String DEFAULT_CRAWL_PRESET_NAME = "Default";
    private static final String AI_KEYWORD_CRAWL_PRESET_NAME = "AI 키워드 필터";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * @date 2026-04-15
     * @desc 애플리케이션 시작 시 Oracle 스키마에 필요한 컬럼, 제약 조건, 인덱스, 시퀀스를 보정합니다.
     */
    @PostConstruct
    public void ensureOracleSchemaMigrations() {
        if (!isOracleDatabase()) {
            return;
        }

        ensureParentCommentIdColumn();
        ensureParentCommentConstraint();
        ensureParentCommentIndex();
        ensureInsightCommentContentLookupIndex();
        ensureDailyKnowledgeAttachmentImagePathColumn();
        ensureTechNewsAttachmentImagePathColumn();
        ensureGenerationScheduleAllowDuplicateColumn();
        ensureGenerationHistoryStatusConstraint();
        ensureGenerationHistoryUsedTrendIdColumn();
        ensureTechNewsUrlIndex();
        ensureCrawlScheduleTable();
        ensureCrawlScheduleExtensionColumns();
        ensureCrawlHistoryTable();
        ensureCrawlHistorySequence();
        ensureCrawlConditionPresetTable();
        ensureCrawlConditionPresetColumns();
        ensureCrawlConditionPresetSequence();
        ensureDefaultCrawlConditionPreset();
        ensureAiKeywordCrawlConditionPreset();
        ensureWeeklyAiInsightTable();
        ensureWeeklyAiInsightSequence();
        ensureDailyTrendInsightTable();
        ensureDailyTrendInsightSequence();
        ensureDailyTrendGenerationHistoryTable();
        ensureDailyTrendGenerationHistorySequence();
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_PROMPT_TEMPLATE, TABLE_PROMPT_TEMPLATE);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_GENERATION_HISTORY, TABLE_GENERATION_HISTORY);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_CRAWL_HISTORY, TABLE_CRAWL_HISTORY);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_CRAWL_CONDITION_PRESET, TABLE_CRAWL_CONDITION_PRESET);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_WEEKLY_AI_INSIGHT, TABLE_WEEKLY_AI_INSIGHT);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_DAILY_TREND_INSIGHT, TABLE_DAILY_TREND_INSIGHT);
        ensureSequenceAlignedWithTableMaxId(
                SEQUENCE_DAILY_TREND_GENERATION_HISTORY,
                TABLE_DAILY_TREND_GENERATION_HISTORY
        );
        ensureSeedUserPasswordsHashed();
    }

    /**
     * @date 2026-08-10
     * @desc 시드 계정(user01/admin01)의 평문 비밀번호를 BCrypt 해시로 1회성 재기록합니다.
     */
    private void ensureSeedUserPasswordsHashed() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, password FROM users WHERE id IN (9001, 9002)");
        for (Map<String, Object> row : rows) {
            String storedPassword = (String) row.get("PASSWORD");
            if (storedPassword == null || storedPassword.startsWith("{")) {
                continue; // 이미 {id} 접두어가 붙은 인코딩 값(또는 이례적 null) — 멱등, 재작업 안 함
            }
            Number id = (Number) row.get("ID");
            jdbcTemplate.update(
                    "UPDATE users SET password = ?, updated_at = SYSTIMESTAMP WHERE id = ?",
                    passwordEncoder.encode(storedPassword), id.longValue()
            );
            log.info("Rehashed seed user password to BCrypt: id={}", id);
        }
    }

    /**
     * @date 2026-04-15
     * @desc insight_comment.parent_comment_id 컬럼이 없으면 추가합니다.
     */
    private void ensureParentCommentIdColumn() {
        if (existsColumn(TABLE_INSIGHT_COMMENT, COLUMN_PARENT_COMMENT_ID)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE insight_comment ADD (parent_comment_id NUMBER(19))");
        log.info("Applied schema migration: added {}.{}", TABLE_INSIGHT_COMMENT, COLUMN_PARENT_COMMENT_ID);
    }

    /**
     * @date 2026-04-15
     * @desc insight_comment 대댓글 부모 참조 FK 제약 조건이 없으면 추가합니다.
     */
    private void ensureParentCommentConstraint() {
        if (existsConstraint(TABLE_INSIGHT_COMMENT, CONSTRAINT_PARENT_COMMENT)) {
            return;
        }
        jdbcTemplate.execute(
                "ALTER TABLE insight_comment ADD CONSTRAINT fk_insight_comment_parent " +
                        "FOREIGN KEY (parent_comment_id) REFERENCES insight_comment(id)"
        );
        log.info("Applied schema migration: added constraint {}", CONSTRAINT_PARENT_COMMENT);
    }

    /**
     * @date 2026-04-15
     * @desc parent_comment_id 조회 성능을 위한 인덱스가 없으면 생성합니다.
     */
    private void ensureParentCommentIndex() {
        if (existsIndex(INDEX_PARENT_COMMENT)) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX idx_insight_comment_parent ON insight_comment (parent_comment_id)");
        log.info("Applied schema migration: added index {}", INDEX_PARENT_COMMENT);
    }

    /**
     * @date 2026-08-06
     * @desc 댓글 트리 조회(content_type/content_id/is_deleted 필터 + created_at 정렬) 성능을 위한 복합 인덱스가 없으면 생성합니다.
     */
    private void ensureInsightCommentContentLookupIndex() {
        if (existsIndex(INDEX_INSIGHT_COMMENT_CONTENT_LOOKUP)) {
            return;
        }
        jdbcTemplate.execute(
                "CREATE INDEX idx_insight_comment_content ON insight_comment (content_type, content_id, is_deleted, created_at)"
        );
        log.info("Applied schema migration: added index {}", INDEX_INSIGHT_COMMENT_CONTENT_LOOKUP);
    }

    /**
     * @date 2026-04-15
     * @desc daily_knowledge 첨부 이미지 경로 컬럼이 없으면 추가합니다.
     */
    private void ensureDailyKnowledgeAttachmentImagePathColumn() {
        if (existsColumn(TABLE_DAILY_KNOWLEDGE, COLUMN_ATTACHMENT_IMAGE_PATH)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE daily_knowledge ADD (attachment_image_path VARCHAR2(500))");
        log.info("Applied schema migration: added {}.{}", TABLE_DAILY_KNOWLEDGE, COLUMN_ATTACHMENT_IMAGE_PATH);
    }

    /**
     * @date 2026-04-15
     * @desc tech_news 첨부 이미지 경로 컬럼이 없으면 추가합니다.
     */
    private void ensureTechNewsAttachmentImagePathColumn() {
        if (existsColumn(TABLE_TECH_NEWS, COLUMN_ATTACHMENT_IMAGE_PATH)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE tech_news ADD (attachment_image_path VARCHAR2(500))");
        log.info("Applied schema migration: added {}.{}", TABLE_TECH_NEWS, COLUMN_ATTACHMENT_IMAGE_PATH);
    }

    /**
     * @date 2026-04-23
     * @desc generation_schedule 중복 생성 허용 여부 컬럼이 없으면 추가합니다.
     */
    private void ensureGenerationScheduleAllowDuplicateColumn() {
        if (existsColumn(TABLE_GENERATION_SCHEDULE, COLUMN_ALLOW_DUPLICATE)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE generation_schedule ADD (allow_duplicate NUMBER(1) DEFAULT 0 NOT NULL)");
        log.info("Applied schema migration: added {}.{}", TABLE_GENERATION_SCHEDULE, COLUMN_ALLOW_DUPLICATE);
    }

    /**
     * @date 2026-04-23
     * @desc generation_history 상태 제약 조건에 SKIPPED 상태를 포함하도록 갱신합니다.
     */
    private void ensureGenerationHistoryStatusConstraint() {
        if (!existsTable(TABLE_GENERATION_HISTORY)) {
            return;
        }
        if (existsConstraint(TABLE_GENERATION_HISTORY, "CK_GENERATION_HISTORY_STATUS")) {
            jdbcTemplate.execute("ALTER TABLE generation_history DROP CONSTRAINT ck_generation_history_status");
        }
        jdbcTemplate.execute(
                "ALTER TABLE generation_history ADD CONSTRAINT ck_generation_history_status " +
                        "CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED'))"
        );
        log.info("Applied schema migration: updated constraint ck_generation_history_status");
    }

    /**
     * @date 2026-08-13
     * @desc generation_history에 지식 생성 시 사용한 일일 트렌드 ID 컬럼이 없으면 추가합니다.
     */
    private void ensureGenerationHistoryUsedTrendIdColumn() {
        if (!existsTable(TABLE_GENERATION_HISTORY) || existsColumn(TABLE_GENERATION_HISTORY, COLUMN_USED_TREND_ID)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE generation_history ADD (used_trend_id NUMBER(19))");
        log.info("Applied schema migration: added {}.{}", TABLE_GENERATION_HISTORY, COLUMN_USED_TREND_ID);
    }

    /**
     * @date 2026-04-17
     * @desc tech_news.url 조회 성능을 위한 단일 컬럼 인덱스가 없으면 생성합니다.
     */
    private void ensureTechNewsUrlIndex() {
        if (existsIndex(INDEX_TECH_NEWS_URL) || existsSingleColumnIndex(TABLE_TECH_NEWS, "URL")) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX idx_tech_news_url ON tech_news (url)");
        log.info("Applied schema migration: added index {}", INDEX_TECH_NEWS_URL);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_schedule 테이블이 없으면 생성합니다.
     */
    private void ensureCrawlScheduleTable() {
        if (existsTable(TABLE_CRAWL_SCHEDULE)) {
            return;
        }
        jdbcTemplate.execute("""
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
                )
                """);
        log.info("Applied schema migration: created table {}", TABLE_CRAWL_SCHEDULE);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_schedule 확장 컬럼이 없으면 추가합니다.
     */
    private void ensureCrawlScheduleExtensionColumns() {
        ensureCrawlScheduleColumn(COLUMN_ALLOW_DUPLICATE, "ALTER TABLE crawl_schedule ADD (allow_duplicate NUMBER(1) DEFAULT 0 NOT NULL)");
        ensureCrawlScheduleColumn("KEYWORD_MATCH_TYPE", "ALTER TABLE crawl_schedule ADD (keyword_match_type VARCHAR2(10) DEFAULT 'OR' NOT NULL)");
        ensureCrawlScheduleColumn("INCLUDE_KEYWORDS", "ALTER TABLE crawl_schedule ADD (include_keywords VARCHAR2(2000))");
        ensureCrawlScheduleColumn(COLUMN_INCLUDE_KEYWORD_OPERATORS, "ALTER TABLE crawl_schedule ADD (include_keyword_operators VARCHAR2(2000))");
        ensureCrawlScheduleColumn(COLUMN_EXCLUDE_KEYWORDS, "ALTER TABLE crawl_schedule ADD (exclude_keywords VARCHAR2(2000))");
        ensureCrawlScheduleColumn("TARGET_DOMAINS", "ALTER TABLE crawl_schedule ADD (target_domains VARCHAR2(2000))");
        ensureCrawlScheduleColumn("CONNECT_TIMEOUT_SECONDS", "ALTER TABLE crawl_schedule ADD (connect_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL)");
        ensureCrawlScheduleColumn("READ_TIMEOUT_SECONDS", "ALTER TABLE crawl_schedule ADD (read_timeout_seconds NUMBER(10) DEFAULT 5 NOT NULL)");
        ensureCrawlScheduleColumn("RETRY_COUNT", "ALTER TABLE crawl_schedule ADD (retry_count NUMBER(10) DEFAULT 1 NOT NULL)");
    }

    /**
     * @date 2026-04-17
     * @desc crawl_schedule 단일 컬럼 존재 여부를 확인하고 필요한 ALTER 문을 실행합니다.
     */
    private void ensureCrawlScheduleColumn(String columnName, String alterSql) {
        if (existsColumn(TABLE_CRAWL_SCHEDULE, columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
        log.info("Applied schema migration: added {}.{}", TABLE_CRAWL_SCHEDULE, columnName);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_history 테이블이 없으면 생성합니다.
     */
    private void ensureCrawlHistoryTable() {
        if (existsTable(TABLE_CRAWL_HISTORY)) {
            return;
        }
        jdbcTemplate.execute("""
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
                )
                """);
        log.info("Applied schema migration: created table {}", TABLE_CRAWL_HISTORY);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_history ID 생성을 위한 시퀀스가 없으면 생성합니다.
     */
    private void ensureCrawlHistorySequence() {
        if (existsSequence(SEQUENCE_CRAWL_HISTORY)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_crawl_history START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_CRAWL_HISTORY);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_condition_preset 테이블이 없으면 생성합니다.
     */
    private void ensureCrawlConditionPresetTable() {
        if (existsTable(TABLE_CRAWL_CONDITION_PRESET)) {
            return;
        }
        jdbcTemplate.execute("""
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
                """);
        log.info("Applied schema migration: created table {}", TABLE_CRAWL_CONDITION_PRESET);
    }

    /**
     * @date 2026-04-17
     * @desc crawl_condition_preset 확장 컬럼이 없으면 추가합니다.
     */
    private void ensureCrawlConditionPresetColumns() {
        if (!existsColumn(TABLE_CRAWL_CONDITION_PRESET, COLUMN_INCLUDE_KEYWORD_OPERATORS)) {
            jdbcTemplate.execute("ALTER TABLE crawl_condition_preset ADD (include_keyword_operators VARCHAR2(2000))");
            log.info("Applied schema migration: added {}.{}", TABLE_CRAWL_CONDITION_PRESET, COLUMN_INCLUDE_KEYWORD_OPERATORS);
        }
        if (!existsColumn(TABLE_CRAWL_CONDITION_PRESET, COLUMN_EXCLUDE_KEYWORDS)) {
            jdbcTemplate.execute("ALTER TABLE crawl_condition_preset ADD (exclude_keywords VARCHAR2(2000))");
            log.info("Applied schema migration: added {}.{}", TABLE_CRAWL_CONDITION_PRESET, COLUMN_EXCLUDE_KEYWORDS);
        }
    }

    /**
     * @date 2026-04-17
     * @desc crawl_condition_preset ID 생성을 위한 시퀀스가 없으면 생성합니다.
     */
    private void ensureCrawlConditionPresetSequence() {
        if (existsSequence(SEQUENCE_CRAWL_CONDITION_PRESET)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_crawl_condition_preset START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_CRAWL_CONDITION_PRESET);
    }

    /**
     * @date 2026-08-23
     * @desc 새 환경에서도 크롤링 조건 프리셋이 비어 있지 않도록 'Default' 프리셋을 1회성으로 시드합니다.
     */
    private void ensureDefaultCrawlConditionPreset() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM crawl_condition_preset WHERE preset_name = ?",
                Integer.class,
                DEFAULT_CRAWL_PRESET_NAME
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO crawl_condition_preset (" +
                        "id, preset_name, source_name, source_url, max_articles, keyword_match_type, " +
                        "connect_timeout_seconds, read_timeout_seconds, retry_count, is_active, created_at, updated_at" +
                        ") VALUES (" +
                        "seq_crawl_condition_preset.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, 1, SYSTIMESTAMP, SYSTIMESTAMP)",
                DEFAULT_CRAWL_PRESET_NAME,
                "Hacker News",
                "https://hnrss.org/frontpage",
                20,
                "OR",
                5,
                5,
                1
        );
        log.info("Applied schema migration: seeded default crawl condition preset '{}'", DEFAULT_CRAWL_PRESET_NAME);
    }

    /**
     * @date 2026-08-23
     * @desc 키워드 필터가 적용된 예시 크롤링 조건 프리셋('AI 키워드 필터')을 1회성으로 시드합니다.
     */
    private void ensureAiKeywordCrawlConditionPreset() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM crawl_condition_preset WHERE preset_name = ?",
                Integer.class,
                AI_KEYWORD_CRAWL_PRESET_NAME
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO crawl_condition_preset (" +
                        "id, preset_name, source_name, source_url, max_articles, keyword_match_type, " +
                        "include_keywords, include_keyword_operators, exclude_keywords, " +
                        "connect_timeout_seconds, read_timeout_seconds, retry_count, is_active, created_at, updated_at" +
                        ") VALUES (" +
                        "seq_crawl_condition_preset.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, SYSTIMESTAMP, SYSTIMESTAMP)",
                AI_KEYWORD_CRAWL_PRESET_NAME,
                "Hacker News",
                "https://hnrss.org/frontpage",
                20,
                "OR",
                "AI,Security,Database",
                "OR,OR,OR",
                "Hiring",
                5,
                5,
                1
        );
        log.info("Applied schema migration: seeded crawl condition preset '{}'", AI_KEYWORD_CRAWL_PRESET_NAME);
    }

    /**
     * @date 2026-05-08
     * @desc weekly_ai_insight 테이블이 없으면 생성합니다.
     */
    private void ensureWeeklyAiInsightTable() {
        if (existsTable(TABLE_WEEKLY_AI_INSIGHT)) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE weekly_ai_insight (
                    id NUMBER(19) NOT NULL,
                    week_start_date DATE NOT NULL,
                    week_end_date DATE NOT NULL,
                    summary CLOB NOT NULL,
                    trend_analysis CLOB NOT NULL,
                    developer_view CLOB NOT NULL,
                    source_news_count NUMBER(10) NOT NULL,
                    is_visible NUMBER(1) DEFAULT 1 NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL,
                    updated_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT pk_weekly_ai_insight PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX ux_weekly_ai_insight_period " +
                        "ON weekly_ai_insight (week_start_date, week_end_date)"
        );
        log.info("Applied schema migration: created table {}", TABLE_WEEKLY_AI_INSIGHT);
    }

    /**
     * @date 2026-05-08
     * @desc weekly_ai_insight ID 생성을 위한 시퀀스가 없으면 생성합니다.
     */
    private void ensureWeeklyAiInsightSequence() {
        if (existsSequence(SEQUENCE_WEEKLY_AI_INSIGHT)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_weekly_ai_insight START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_WEEKLY_AI_INSIGHT);
    }

    /**
     * @date 2026-08-13
     * @desc daily_trend_insight 테이블과 기준일 고유 인덱스가 없으면 생성합니다.
     */
    private void ensureDailyTrendInsightTable() {
        if (existsTable(TABLE_DAILY_TREND_INSIGHT)) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE daily_trend_insight (
                    id NUMBER(19) NOT NULL,
                    trend_date DATE NOT NULL,
                    keywords VARCHAR2(500) NOT NULL,
                    summary VARCHAR2(1000) NOT NULL,
                    source_news_count NUMBER(10) NOT NULL,
                    is_visible NUMBER(1) DEFAULT 1 NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL,
                    updated_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT pk_daily_trend_insight PRIMARY KEY (id)
                )
                """);
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX ux_daily_trend_insight_date ON daily_trend_insight (trend_date)"
        );
        log.info("Applied schema migration: created table {}", TABLE_DAILY_TREND_INSIGHT);
    }

    /**
     * @date 2026-08-13
     * @desc daily_trend_insight ID 생성을 위한 시퀀스가 없으면 생성합니다.
     */
    private void ensureDailyTrendInsightSequence() {
        if (existsSequence(SEQUENCE_DAILY_TREND_INSIGHT)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_daily_trend_insight START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_DAILY_TREND_INSIGHT);
    }

    /**
     * @date 2026-08-13
     * @desc daily_trend_generation_history 테이블이 없으면 생성합니다.
     */
    private void ensureDailyTrendGenerationHistoryTable() {
        if (existsTable(TABLE_DAILY_TREND_GENERATION_HISTORY)) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE daily_trend_generation_history (
                    id NUMBER(19) NOT NULL,
                    trigger_type VARCHAR2(20) NOT NULL,
                    target_date DATE NOT NULL,
                    status VARCHAR2(20) NOT NULL,
                    source_news_count NUMBER(10),
                    created_trend_id NUMBER(19),
                    error_message VARCHAR2(1000),
                    created_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT pk_daily_trend_gen_history PRIMARY KEY (id),
                    CONSTRAINT ck_daily_trend_gen_history_status CHECK (status IN ('SUCCESS', 'FAILED'))
                )
                """);
        log.info("Applied schema migration: created table {}", TABLE_DAILY_TREND_GENERATION_HISTORY);
    }

    /**
     * @date 2026-08-13
     * @desc daily_trend_generation_history ID 생성을 위한 시퀀스가 없으면 생성합니다.
     */
    private void ensureDailyTrendGenerationHistorySequence() {
        if (existsSequence(SEQUENCE_DAILY_TREND_GENERATION_HISTORY)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_daily_trend_gen_history START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_DAILY_TREND_GENERATION_HISTORY);
    }

    /**
     * @date 2026-04-15
     * @desc 현재 연결된 데이터베이스가 Oracle인지 확인합니다.
     */
    private boolean isOracleDatabase() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("oracle");
        } catch (SQLException exception) {
            throw new IllegalStateException("데이터베이스 제품명 확인 중 오류가 발생했습니다.", exception);
        }
    }

    /**
     * @date 2026-04-15
     * @desc user_tab_columns 메타데이터에서 지정한 컬럼 존재 여부를 확인합니다.
     */
    private boolean existsColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    /**
     * @date 2026-04-17
     * @desc user_tables 메타데이터에서 지정한 테이블 존재 여부를 확인합니다.
     */
    private boolean existsTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    /**
     * @date 2026-04-15
     * @desc user_constraints 메타데이터에서 지정한 제약 조건 존재 여부를 확인합니다.
     */
    private boolean existsConstraint(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_constraints WHERE table_name = ? AND constraint_name = ?",
                Integer.class,
                tableName,
                constraintName
        );
        return count != null && count > 0;
    }

    /**
     * @date 2026-04-15
     * @desc user_indexes 메타데이터에서 지정한 인덱스 존재 여부를 확인합니다.
     */
    private boolean existsIndex(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_indexes WHERE index_name = ?",
                Integer.class,
                indexName
        );
        return count != null && count > 0;
    }

    /**
     * @date 2026-04-17
     * @desc 지정한 테이블과 컬럼으로 구성된 단일 컬럼 인덱스 존재 여부를 확인합니다.
     */
    private boolean existsSingleColumnIndex(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM user_indexes ui " +
                        "JOIN user_ind_columns uic ON ui.index_name = uic.index_name " +
                        "WHERE ui.table_name = ? " +
                        "AND uic.column_name = ? " +
                        "AND ui.index_type = 'NORMAL' " +
                        "AND uic.column_position = 1 " +
                        "AND NOT EXISTS ( " +
                        "  SELECT 1 FROM user_ind_columns uic2 " +
                        "  WHERE uic2.index_name = ui.index_name " +
                        "  AND uic2.column_position > 1" +
                        ")",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    /**
     * @date 2026-04-16
     * @desc 시퀀스의 다음 값이 대상 테이블의 최대 ID보다 작거나 같으면 다음 ID 이후로 보정합니다.
     */
    private void ensureSequenceAlignedWithTableMaxId(String sequenceName, String tableName) {
        if (!existsSequence(sequenceName)) {
            return;
        }

        Long maxId = jdbcTemplate.queryForObject(
                "SELECT NVL(MAX(id), 0) FROM " + tableName.toLowerCase(),
                Long.class
        );
        Long maxIdValue = maxId == null ? 0L : maxId;

        Long currentNextValue = jdbcTemplate.queryForObject(
                "SELECT " + sequenceName.toLowerCase() + ".NEXTVAL FROM dual",
                Long.class
        );

        if (currentNextValue == null || currentNextValue > maxIdValue) {
            return;
        }

        long increment = (maxIdValue + 1L) - currentNextValue;
        jdbcTemplate.execute("ALTER SEQUENCE " + sequenceName.toLowerCase() + " INCREMENT BY " + increment);
        jdbcTemplate.queryForObject("SELECT " + sequenceName.toLowerCase() + ".NEXTVAL FROM dual", Long.class);
        jdbcTemplate.execute("ALTER SEQUENCE " + sequenceName.toLowerCase() + " INCREMENT BY 1");

        log.info("Aligned sequence {} to table {} max id {}", sequenceName, tableName, maxIdValue);
    }

    /**
     * @date 2026-04-16
     * @desc user_sequences 메타데이터에서 지정한 시퀀스 존재 여부를 확인합니다.
     */
    private boolean existsSequence(String sequenceName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_sequences WHERE sequence_name = ?",
                Integer.class,
                sequenceName
        );
        return count != null && count > 0;
    }
}


