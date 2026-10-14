package org.likelion.hsu.db_project.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 로그인 도입 전에 만들어진 dog 테이블에 작성자 컬럼(user_id)을 추가한다.
 * (MySQL은 ADD COLUMN IF NOT EXISTS를 지원하지 않아 schema.sql 대신 여기서 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'dog' AND column_name = 'user_id'",
                Integer.class);

        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE dog ADD COLUMN user_id BIGINT NULL");
            log.info("[Schema] dog.user_id 컬럼 추가");
        }
    }
}
