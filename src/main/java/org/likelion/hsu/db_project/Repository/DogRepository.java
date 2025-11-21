package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Dog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class DogRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Dog> rowMapper = (rs, n) -> Dog.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .breed(rs.getString("breed"))
            .gender(rs.getString("gender"))          // ✅ 성별 매핑
            .foundPlace(rs.getString("found_place"))
            .age(rs.getObject("age") == null ? null : rs.getObject("age", Integer.class))
            .status(rs.getString("status"))
            .memo(rs.getString("memo"))
            .imageUrl(rs.getString("image_url"))
            .regDate(rs.getTimestamp("reg_date") == null ? null : rs.getTimestamp("reg_date").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    // ✅ INSERT: 컬럼·파라미터 순서 정확히 일치
    public int save(Dog d) {
        String sql = """
            INSERT INTO pet (name, breed, gender, found_place, age, status, memo, image_url, reg_date, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """;
        return jdbcTemplate.update(sql,
                d.getName(),
                d.getBreed(),
                d.getGender(),      // ✅ 누락됐던 gender 파라미터 추가
                d.getFoundPlace(),
                d.getAge(),
                d.getStatus(),
                d.getMemo(),
                d.getImageUrl());   // ✅ image_url 자리 맞춤
    }

    public Long getLastInsertId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Dog> findAllWithConditions(String search, String status, String sort) {
        StringBuilder sql = new StringBuilder("SELECT * FROM pet WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("AND (name LIKE ? OR breed LIKE ? OR found_place LIKE ?) ");
            String like = "%" + search + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND status = ? ");
            params.add(status);
        }

        if ("name".equalsIgnoreCase(sort)) sql.append("ORDER BY name ASC ");
        else if ("age".equalsIgnoreCase(sort)) sql.append("ORDER BY age ASC ");
        else sql.append("ORDER BY id DESC ");

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public Optional<Dog> findById(Long id) {
        List<Dog> list = jdbcTemplate.query("SELECT * FROM pet WHERE id = ?", rowMapper, id);
        return list.stream().findFirst();
    }

    // ✅ UPDATE에도 gender 반영(이미 반영되어 있었음)
    public int update(Long id, Dog d) {
        String sql = """
            UPDATE pet
               SET name=?, breed=?, gender=?, found_place=?, age=?, status=?, memo=?, image_url=?, updated_at=NOW()
             WHERE id=?
        """;
        return jdbcTemplate.update(sql,
                d.getName(), d.getBreed(), d.getGender(), d.getFoundPlace(), d.getAge(),
                d.getStatus(), d.getMemo(), d.getImageUrl(), id);
    }

    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM pet WHERE id = ?", id);
    }

    public List<Map<String, Object>> getStatusStats() {
        return jdbcTemplate.queryForList("SELECT status, COUNT(*) AS count FROM pet GROUP BY status");
    }

    public List<Map<String, Object>> getBreedStats() {
        return jdbcTemplate.queryForList("SELECT breed, COUNT(*) AS count FROM pet GROUP BY breed");
    }
}