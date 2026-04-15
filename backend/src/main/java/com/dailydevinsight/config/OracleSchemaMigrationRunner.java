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

    private final JdbcTemplate jdbcTemplate;

    /**
     * @date 2026-04-15
     * @desc 애플리케이션 시작 시 Oracle 스키마의 대댓글 컬럼/제약/인덱스를 보정합니다.
     */
    @PostConstruct
    public void ensureInsightCommentReplySchema() {
        if (!isOracleDatabase()) {
            return;
        }

        ensureParentCommentIdColumn();
        ensureParentCommentConstraint();
        ensureParentCommentIndex();
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
}
