package org.likelion.hsu.db_project.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * schema.sql 의 CREATE TABLE IF NOT EXISTS 는 이미 있는 테이블을 고치지 않으므로,
 * 나중에 추가된 컬럼을 기존 DB에 넣어 준다.
 * (MySQL은 ADD COLUMN IF NOT EXISTS를 지원하지 않아 schema.sql 대신 여기서 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("dog", "user_id", "BIGINT NULL");
        addColumnIfMissing("dog", "hidden", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("board_post", "hidden", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("board_comment", "hidden", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("users", "terms_agreed_at", "DATETIME NULL");
        addColumnIfMissing("users", "deleted_at", "DATETIME NULL");
    }

    // table, column, definition 은 위의 고정값만 들어온다
    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);

        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("[Schema] {}.{} 컬럼 추가", table, column);
        }
    }
}
