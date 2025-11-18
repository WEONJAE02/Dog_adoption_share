package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Dog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DogRepository {

    private final JdbcTemplate jdbcTemplate;

    // ✅ RowMapper
    private final RowMapper<Dog> dogRowMapper = new RowMapper<>() {
        @Override
        public Dog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Dog.builder()
                    .dogId(rs.getLong("dog_id"))
                    .name(rs.getString("name"))
                    .breed(rs.getString("breed"))
                    .age(rs.getInt("age"))
                    .status(rs.getString("status"))
                    .imageName(rs.getString("image_name"))
                    .gender(rs.getString("gender"))
                    .regDate(rs.getTimestamp("reg_date") != null
                            ? rs.getTimestamp("reg_date").toLocalDateTime()
                            : null)
                    .build();
        }
    };

    // ✅ 등록
    public int save(Dog dog) {
        String sql = "INSERT INTO dog (name, breed, age, status, image_name, gender, reg_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                dog.getName(),
                dog.getBreed(),
                dog.getAge(),
                dog.getStatus(),
                dog.getImageName(),
                dog.getGender(),
                dog.getRegDate() != null ? dog.getRegDate() : LocalDateTime.now()
        );
    }

    // ✅ 최근 삽입된 PK 반환
    public Long getLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ✅ 검색 + 정렬 + 필터
    public List<Dog> findAllWithConditions(String search, String status, String sort) {
        StringBuilder sql = new StringBuilder("SELECT * FROM dog WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // 검색 (이름 / 품종 / 구조장소)
        if (search != null && !search.isEmpty()) {
            sql.append("AND (name LIKE ? OR breed LIKE ? OR dog_id IN (SELECT dog_id FROM dog_detail WHERE found_place LIKE ?)) ");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        // 상태 필터
        if (status != null && !status.isEmpty()) {
            sql.append("AND status = ? ");
            params.add(status);
        }

        // 정렬 기준
        switch (sort) {
            case "name" -> sql.append("ORDER BY name ASC ");
            case "age" -> sql.append("ORDER BY age ASC ");
            default -> sql.append("ORDER BY dog_id DESC ");
        }

        return jdbcTemplate.query(sql.toString(), dogRowMapper, params.toArray());
    }

    // ✅ 상세 조회
    public Optional<Dog> findById(Long id) {
        String sql = "SELECT * FROM dog WHERE dog_id = ?";
        List<Dog> result = jdbcTemplate.query(sql, dogRowMapper, id);
        return result.stream().findFirst();
    }

    // ✅ 수정
    public int update(Long id, Dog dog) {
        String sql = "UPDATE dog SET name=?, breed=?, age=?, status=?, image_name=?, gender=? WHERE dog_id=?";
        return jdbcTemplate.update(sql,
                dog.getName(),
                dog.getBreed(),
                dog.getAge(),
                dog.getStatus(),
                dog.getImageName(),
                dog.getGender(),
                id);
    }

    // ✅ 삭제
    public int delete(Long id) {
        String sql = "DELETE FROM dog WHERE dog_id = ?";
        return jdbcTemplate.update(sql, id);
    }

    // ✅ 상태별 통계 (대시보드)
    public List<Map<String, Object>> getStatusStats() {
        String sql = "SELECT status, COUNT(*) AS count FROM dog GROUP BY status";
        return jdbcTemplate.queryForList(sql);
    }

    // ✅ 품종별 통계 (대시보드)
    public List<Map<String, Object>> getBreedStats() {
        String sql = "SELECT breed, COUNT(*) AS count FROM dog GROUP BY breed";
        return jdbcTemplate.queryForList(sql);
    }

}
