package com.dailydevinsight.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OracleSchemaMigrationRunner {

    private static final String TABLE_INSIGHT_COMMENT = "INSIGHT_COMMENT";
    private static final String COLUMN_PARENT_COMMENT_ID = "PARENT_COMMENT_ID";
    private static final String CONSTRAINT_PARENT_COMMENT = "FK_INSIGHT_COMMENT_PARENT";
    private static final String INDEX_PARENT_COMMENT = "IDX_INSIGHT_COMMENT_PARENT";
    private static final String TABLE_TECH_NEWS = "TECH_NEWS";
    private static final String TABLE_DAILY_KNOWLEDGE = "DAILY_KNOWLEDGE";
    private static final String TABLE_PROMPT_TEMPLATE = "PROMPT_TEMPLATE";
    private static final String TABLE_GENERATION_SCHEDULE = "GENERATION_SCHEDULE";
    private static final String TABLE_GENERATION_HISTORY = "GENERATION_HISTORY";
    private static final String TABLE_CRAWL_SCHEDULE = "CRAWL_SCHEDULE";
    private static final String TABLE_CRAWL_HISTORY = "CRAWL_HISTORY";
    private static final String TABLE_CRAWL_CONDITION_PRESET = "CRAWL_CONDITION_PRESET";
    private static final String COLUMN_ATTACHMENT_IMAGE_PATH = "ATTACHMENT_IMAGE_PATH";
    private static final String COLUMN_ALLOW_DUPLICATE = "ALLOW_DUPLICATE";
    private static final String COLUMN_EXCLUDE_KEYWORDS = "EXCLUDE_KEYWORDS";
    private static final String COLUMN_INCLUDE_KEYWORD_OPERATORS = "INCLUDE_KEYWORD_OPERATORS";
    private static final String SEQUENCE_PROMPT_TEMPLATE = "SEQ_PROMPT_TEMPLATE";
    private static final String SEQUENCE_GENERATION_HISTORY = "SEQ_GENERATION_HISTORY";
    private static final String SEQUENCE_CRAWL_HISTORY = "SEQ_CRAWL_HISTORY";
    private static final String SEQUENCE_CRAWL_CONDITION_PRESET = "SEQ_CRAWL_CONDITION_PRESET";
    private static final String INDEX_TECH_NEWS_URL = "IDX_TECH_NEWS_URL";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @date 2026-04-15
     * @desc ?좏뵆由ъ??댁뀡 ?쒖옉 ??Oracle ?ㅽ궎留덉쓽 ??볤? 而щ읆/?쒖빟/?몃뜳?ㅻ? 蹂댁젙?⑸땲??
     */
    @PostConstruct
    public void ensureOracleSchemaMigrations() {
        if (!isOracleDatabase()) {
            return;
        }

        ensureParentCommentIdColumn();
        ensureParentCommentConstraint();
        ensureParentCommentIndex();
        ensureDailyKnowledgeAttachmentImagePathColumn();
        ensureTechNewsAttachmentImagePathColumn();
        ensureGenerationScheduleAllowDuplicateColumn();
        ensureGenerationHistoryStatusConstraint();
        ensureTechNewsUrlIndex();
        ensureCrawlScheduleTable();
        ensureCrawlScheduleExtensionColumns();
        ensureCrawlHistoryTable();
        ensureCrawlHistorySequence();
        ensureCrawlConditionPresetTable();
        ensureCrawlConditionPresetColumns();
        ensureCrawlConditionPresetSequence();
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_PROMPT_TEMPLATE, TABLE_PROMPT_TEMPLATE);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_GENERATION_HISTORY, TABLE_GENERATION_HISTORY);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_CRAWL_HISTORY, TABLE_CRAWL_HISTORY);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_CRAWL_CONDITION_PRESET, TABLE_CRAWL_CONDITION_PRESET);
    }

    /**
     * @date 2026-04-15
     * @desc insight_comment.parent_comment_id 而щ읆???놁쑝硫?異붽??⑸땲??
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
     * @desc insight_comment ?먭린李몄“ FK媛 ?놁쑝硫?異붽??⑸땲??
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
     * @desc parent_comment_id 議고쉶 ?깅뒫???꾪빐 ?몃뜳?ㅺ? ?놁쑝硫??앹꽦?⑸땲??
     */
    private void ensureParentCommentIndex() {
        if (existsIndex(INDEX_PARENT_COMMENT)) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX idx_insight_comment_parent ON insight_comment (parent_comment_id)");
        log.info("Applied schema migration: added index {}", INDEX_PARENT_COMMENT);
    }

    /**
     * @date 2026-04-15
     * @desc daily_knowledge 泥⑤? ?대?吏 寃쎈줈 而щ읆???놁쑝硫?異붽??⑸땲??
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
     * @desc tech_news 泥⑤? ?대?吏 寃쎈줈 而щ읆???놁쑝硫?異붽??⑸땲??
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
     * @desc generation_schedule 以묐났 ?덉슜 ?ㅼ젙 而щ읆???놁쑝硫?異붽??⑸땲??
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
     * @desc generation_history ?곹깭 ?쒖빟??SKIPPED ?곹깭瑜??ы븿?섎룄濡?蹂댁젙?⑸땲??
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
     * @date 2026-04-17
     * @desc tech_news.url 조회 성능을 위해 인덱스가 없으면 생성합니다.
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
     * @desc crawl_schedule ?뚯씠釉붿씠 ?놁쑝硫??앹꽦?⑸땲??
     */
    private void ensureCrawlScheduleTable() {
        if (existsTable(TABLE_CRAWL_SCHEDULE)) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE crawl_schedule (
                    id NUMBER(19) NOT NULL,
                    is_enabled NUMBER(1) DEFAULT 0 NOT NULL,
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
     * @desc crawl_schedule ?뺤옣 而щ읆(?ㅼ썙???꾨찓????꾩븘?????꾨씫 ??蹂댁젙?⑸땲??
     */
    private void ensureCrawlScheduleExtensionColumns() {
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
     * @desc crawl_schedule ?⑥씪 而щ읆 ?꾨씫 ??ALTER瑜??곸슜?⑸땲??
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
     * @desc crawl_history ?뚯씠釉붿씠 ?놁쑝硫??앹꽦?⑸땲??
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
     * @desc crawl_history ID ?쒗?ㅺ? ?놁쑝硫??앹꽦?⑸땲??
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
     * @desc crawl_condition_preset ?뚯씠釉붿씠 ?놁쑝硫??앹꽦?⑸땲??
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
     * @desc crawl_condition_preset ?뺤옣 而щ읆 ?꾨씫 ??蹂댁젙?⑸땲??
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
     * @desc crawl_condition_preset ID ?쒗?ㅺ? ?놁쑝硫??앹꽦?⑸땲??
     */
    private void ensureCrawlConditionPresetSequence() {
        if (existsSequence(SEQUENCE_CRAWL_CONDITION_PRESET)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_crawl_condition_preset START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_CRAWL_CONDITION_PRESET);
    }

    /**
     * @date 2026-04-15
     * @desc ?꾩옱 ?곗씠?곕쿋?댁뒪媛 Oracle?몄? 硫뷀??곗씠?곕줈 ?먮퀎?⑸땲??
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
            throw new IllegalStateException("?곗씠?곕쿋?댁뒪 硫뷀??곗씠??議고쉶???ㅽ뙣?덉뒿?덈떎.", exception);
        }
    }

    /**
     * @date 2026-04-15
     * @desc user_tab_columns 湲곗??쇰줈 吏??而щ읆 議댁옱 ?щ?瑜?議고쉶?⑸땲??
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
     * @desc user_tables 湲곗??쇰줈 ????뚯씠釉?議댁옱 ?щ?瑜??뺤씤?⑸땲??
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
     * @desc user_constraints 湲곗??쇰줈 吏???쒖빟議곌굔 議댁옱 ?щ?瑜?議고쉶?⑸땲??
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
     * @desc user_indexes 湲곗??쇰줈 吏???몃뜳??議댁옱 ?щ?瑜?議고쉶?⑸땲??
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
     * @desc 吏?뺥븳 ?뚯씠釉?而щ읆?쇰줈 援ъ꽦???⑥씪 而щ읆 ?몃뜳??議댁옱 ?щ?瑜?議고쉶?⑸땲??
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
     * @desc ?쒗???ㅼ쓬 媛믪씠 ?뚯씠釉?理쒕? ID蹂대떎 ?묎굅??媛숈쑝硫??쒖옉媛믪쓣 ?먮룞 蹂댁젙?⑸땲??
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
     * @desc user_sequences 湲곗??쇰줈 吏???쒗??議댁옱 ?щ?瑜?議고쉶?⑸땲??
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


