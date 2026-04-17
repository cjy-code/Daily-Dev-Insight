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
    private static final String TABLE_GENERATION_HISTORY = "GENERATION_HISTORY";
    private static final String TABLE_CRAWL_SCHEDULE = "CRAWL_SCHEDULE";
    private static final String TABLE_CRAWL_HISTORY = "CRAWL_HISTORY";
    private static final String COLUMN_ATTACHMENT_IMAGE_PATH = "ATTACHMENT_IMAGE_PATH";
    private static final String SEQUENCE_PROMPT_TEMPLATE = "SEQ_PROMPT_TEMPLATE";
    private static final String SEQUENCE_GENERATION_HISTORY = "SEQ_GENERATION_HISTORY";
    private static final String SEQUENCE_CRAWL_HISTORY = "SEQ_CRAWL_HISTORY";
    private static final String INDEX_TECH_NEWS_URL = "IDX_TECH_NEWS_URL";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @date 2026-04-15
     * @desc 애플리케이션 시작 시 Oracle 스키마의 대댓글 컬럼/제약/인덱스를 보정합니다.
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
        ensureTechNewsUrlIndex();
        ensureCrawlScheduleTable();
        ensureCrawlHistoryTable();
        ensureCrawlHistorySequence();
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_PROMPT_TEMPLATE, TABLE_PROMPT_TEMPLATE);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_GENERATION_HISTORY, TABLE_GENERATION_HISTORY);
        ensureSequenceAlignedWithTableMaxId(SEQUENCE_CRAWL_HISTORY, TABLE_CRAWL_HISTORY);
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
     * @desc insight_comment 자기참조 FK가 없으면 추가합니다.
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
     * @desc parent_comment_id 조회 성능을 위해 인덱스가 없으면 생성합니다.
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
                    cron_expression VARCHAR2(120) NOT NULL,
                    source_name VARCHAR2(100) NOT NULL,
                    source_url VARCHAR2(500) NOT NULL,
                    max_articles NUMBER(10) DEFAULT 20 NOT NULL,
                    last_executed_at TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT pk_crawl_schedule PRIMARY KEY (id)
                )
                """);
        log.info("Applied schema migration: created table {}", TABLE_CRAWL_SCHEDULE);
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
     * @desc crawl_history ID 시퀀스가 없으면 생성합니다.
     */
    private void ensureCrawlHistorySequence() {
        if (existsSequence(SEQUENCE_CRAWL_HISTORY)) {
            return;
        }
        jdbcTemplate.execute("CREATE SEQUENCE seq_crawl_history START WITH 1 INCREMENT BY 1 NOCACHE");
        log.info("Applied schema migration: created sequence {}", SEQUENCE_CRAWL_HISTORY);
    }

    /**
     * @date 2026-04-15
     * @desc 현재 데이터베이스가 Oracle인지 메타데이터로 판별합니다.
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
            throw new IllegalStateException("데이터베이스 메타데이터 조회에 실패했습니다.", exception);
        }
    }

    /**
     * @date 2026-04-15
     * @desc user_tab_columns 기준으로 지정 컬럼 존재 여부를 조회합니다.
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
     * @desc user_tables 기준으로 대상 테이블 존재 여부를 확인합니다.
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
     * @desc user_constraints 기준으로 지정 제약조건 존재 여부를 조회합니다.
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
     * @desc user_indexes 기준으로 지정 인덱스 존재 여부를 조회합니다.
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
     * @desc 지정한 테이블/컬럼으로 구성된 단일 컬럼 인덱스 존재 여부를 조회합니다.
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
     * @desc 시퀀스 다음 값이 테이블 최대 ID보다 작거나 같으면 시작값을 자동 보정합니다.
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
     * @desc user_sequences 기준으로 지정 시퀀스 존재 여부를 조회합니다.
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
