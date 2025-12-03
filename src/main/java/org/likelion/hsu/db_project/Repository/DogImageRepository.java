package org.likelion.hsu.db_project.Repository;
import lombok.RequiredArgsConstructor;

import org.likelion.hsu.db_project.Entity.DogImage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DogImageRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DogImage> imageRowMapper = new RowMapper<>() {
        @Override
        public DogImage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DogImage.builder()
                    .id(rs.getLong("id"))
                    .fileName(rs.getString("file_name"))
                    .filePath(rs.getString("file_path"))
                    .uploadDate(rs.getTimestamp("upload_date") != null
                            ? rs.getTimestamp("upload_date").toLocalDateTime()
                            : null)
                    .build();
        }
    };

    // ✅ 이미지 등록 (업로드일 자동 입력)
    public void save(DogImage image, Long dogId) {
        String sql = "INSERT INTO dog_image (dog_id, file_name, file_path, upload_date) VALUES (?, ?, ?, ?)";

        // ✅ image.getUploadDate()가 null이면 현재 시간(LocalDateTime.now())으로 자동 설정
        LocalDateTime uploadTime = (image.getUploadDate() != null)
                ? image.getUploadDate()
                : LocalDateTime.now();

        jdbcTemplate.update(sql,
                dogId,
                image.getFileName(),
                image.getFilePath(),
                uploadTime);
    }

    // ✅ 특정 유기견의 이미지 목록 조회
    public List<DogImage> findByDogId(Long dogId) {
        String sql = "SELECT * FROM dog_image WHERE dog_id = ?";
        return jdbcTemplate.query(sql, imageRowMapper, dogId);
    }

    // ✅ 유기견별 이미지 전체 삭제
    public void deleteByDogId(Long dogId) {
        String sql = "DELETE FROM dog_image WHERE dog_id = ?";
        jdbcTemplate.update(sql, dogId);
    }
}
