package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.HealthCheck;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HealthCheckRepository {

    // 반려동물 이름·종류, 첫 프레임을 함께 조회
    private static final String SELECT_CHECK =
            "SELECT h.*, p.name AS pet_name, p.species AS pet_species, " +
            "(SELECT f.file_name FROM health_check_frame f WHERE f.check_id = h.check_id ORDER BY f.seq LIMIT 1) AS thumbnail " +
            "FROM health_check h JOIN my_pet p ON p.pet_id = h.pet_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<HealthCheck> checkRowMapper = (rs, rowNum) -> HealthCheck.builder()
            .checkId(rs.getLong("check_id"))
            .petId(rs.getLong("pet_id"))
            .userId(rs.getLong("user_id"))
            .bodyPart(rs.getString("body_part"))
            .source(rs.getString("source"))
            .symptoms(rs.getString("symptoms"))
            .memo(rs.getString("memo"))
            .urgency(rs.getString("urgency"))
            .specialty(rs.getString("specialty"))
            .summary(rs.getString("summary"))
            .resultJson(rs.getString("result_json"))
            .analyzer(rs.getString("analyzer"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .petName(rs.getString("pet_name"))
            .petSpecies(rs.getString("pet_species"))
            .thumbnail(rs.getString("thumbnail"))
            .build();

    // ✅ 등록 (생성된 PK 반환)
    public Long save(HealthCheck check) {
        String sql = "INSERT INTO health_check (pet_id, user_id, body_part, source, symptoms, memo, urgency, specialty, summary, result_json, analyzer, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, check.getPetId());
            ps.setLong(2, check.getUserId());
            ps.setString(3, check.getBodyPart());
            ps.setString(4, check.getSource());
            ps.setString(5, check.getSymptoms());
            ps.setString(6, check.getMemo());
            ps.setString(7, check.getUrgency());
            ps.setString(8, check.getSpecialty());
            ps.setString(9, check.getSummary());
            ps.setString(10, check.getResultJson());
            ps.setString(11, check.getAnalyzer());
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<HealthCheck> findById(Long checkId) {
        return jdbcTemplate.query(SELECT_CHECK + "WHERE h.check_id = ?", checkRowMapper, checkId).stream().findFirst();
    }

    // ✅ 내 체크 기록 (최신순, 반려동물·부위로 거를 수 있음)
    public List<HealthCheck> findByUser(Long userId, Long petId, String bodyPart, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_CHECK + "WHERE h.user_id = ? ");
        List<Object> params = new ArrayList<>(List.of(userId));
        if (petId != null) {
            sql.append("AND h.pet_id = ? ");
            params.add(petId);
        }
        if (bodyPart != null) {
            sql.append("AND h.body_part = ? ");
            params.add(bodyPart);
        }
        sql.append("ORDER BY h.check_id DESC LIMIT ?");
        params.add(limit);
        return jdbcTemplate.query(sql.toString(), checkRowMapper, params.toArray());
    }

    // ✅ 같은 반려동물·같은 부위의 바로 이전 체크
    public Optional<HealthCheck> findPrevious(Long petId, String bodyPart, Long beforeCheckId) {
        return jdbcTemplate.query(SELECT_CHECK + "WHERE h.pet_id = ? AND h.body_part = ? AND h.check_id < ? ORDER BY h.check_id DESC LIMIT 1",
                checkRowMapper, petId, bodyPart, beforeCheckId).stream().findFirst();
    }

    // 같은 반려동물·같은 부위에서 몇 번째 체크인지 (이 체크 포함)
    public int countUpTo(Long petId, String bodyPart, Long checkId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_check WHERE pet_id = ? AND body_part = ? AND check_id <= ?",
                Integer.class, petId, bodyPart, checkId);
        return count != null ? count : 0;
    }

    // ✅ 사용 횟수 (하루 제한). 체크 기록과 따로 남겨서, 기록을 지워도 횟수는 줄지 않는다
    public void recordUsage(Long userId) {
        jdbcTemplate.update("INSERT INTO health_check_usage (user_id, created_at) VALUES (?, ?)",
                userId, Timestamp.valueOf(LocalDateTime.now()));
    }

    public int countToday(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_check_usage WHERE user_id = ? AND created_at >= ?",
                Integer.class, userId, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
        return count != null ? count : 0;
    }

    public void deleteUsageByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM health_check_usage WHERE user_id = ?", userId);
    }

    public List<Long> findIdsByPetId(Long petId) {
        return jdbcTemplate.queryForList("SELECT check_id FROM health_check WHERE pet_id = ?", Long.class, petId);
    }

    // ✅ 프레임
    public void saveFrame(Long checkId, int seq, String fileName) {
        jdbcTemplate.update("INSERT INTO health_check_frame (check_id, seq, file_name) VALUES (?, ?, ?)", checkId, seq, fileName);
    }

    public List<String> findFrames(Long checkId) {
        return jdbcTemplate.queryForList(
                "SELECT file_name FROM health_check_frame WHERE check_id = ? ORDER BY seq", String.class, checkId);
    }

    // ✅ 삭제 (프레임 행 포함, 파일은 서비스에서 지운다)
    public void delete(Long checkId) {
        jdbcTemplate.update("DELETE FROM health_check_frame WHERE check_id = ?", checkId);
        jdbcTemplate.update("DELETE FROM health_check WHERE check_id = ?", checkId);
    }
}
