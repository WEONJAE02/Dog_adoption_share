package org.likelion.hsu.db_project.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelion.hsu.db_project.Service.ImageStorage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 비공개 폴더 도입 전에 공개 폴더(uploads/)에 저장된 내 반려동물 사진·건강체크 장면을 비공개 폴더로 옮긴다.
 * 이미 옮긴 파일은 건너뛰므로 서버를 시작할 때마다 실행해도 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateImageMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ImageStorage imageStorage;

    @Override
    public void run(ApplicationArguments args) {
        List<String> files = jdbcTemplate.queryForList(
                "SELECT image_name FROM my_pet WHERE image_name IS NOT NULL UNION SELECT file_name FROM health_check_frame",
                String.class);
        int moved = 0;
        for (String file : files) {
            try {
                if (imageStorage.moveToPrivate(file)) {
                    moved++;
                }
            } catch (IOException e) {
                log.warn("[Upload] 비공개 폴더로 옮기지 못했습니다: {}", file, e);
            }
        }
        if (moved > 0) {
            log.info("[Upload] 반려동물·건강체크 사진 {}개를 비공개 폴더로 옮겼습니다.", moved);
        }
    }
}
