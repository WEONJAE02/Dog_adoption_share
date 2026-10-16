package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(String targetType, Long targetId, Long reporterId, String reason, String detail) {
        jdbcTemplate.update(
                "INSERT INTO report (target_type, target_id, reporter_id, reason, detail, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                targetType, targetId, reporterId, reason, detail, Timestamp.valueOf(LocalDateTime.now()));
    }

    public int countReporters(String targetType, Long targetId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT reporter_id) FROM report WHERE target_type = ? AND target_id = ?",
                Integer.class, targetType, targetId);
        return count != null ? count : 0;
    }

    // ✅ 신고 대상의 작성자 (대상이 없으면 empty, 작성자가 없는 기존 글이면 Optional.of(null) 대신 -1)
    public Optional<Long> findOwner(String targetType, Long targetId) {
        String sql = switch (targetType) {
            case "dog" -> "SELECT COALESCE(user_id, -1) FROM dog WHERE dog_id = ?";
            case "post" -> "SELECT user_id FROM board_post WHERE post_id = ?";
            case "comment" -> "SELECT user_id FROM board_comment WHERE comment_id = ? AND deleted = 0";
            case "item" -> "SELECT user_id FROM shop_item WHERE item_id = ?";
            default -> throw new IllegalArgumentException(targetType);
        };
        List<Long> owners = jdbcTemplate.queryForList(sql, Long.class, targetId);
        return owners.stream().findFirst();
    }

    // ✅ 신고 누적 시 숨김
    public void hide(String targetType, Long targetId) {
        String sql = switch (targetType) {
            case "dog" -> "UPDATE dog SET hidden = 1 WHERE dog_id = ?";
            case "post" -> "UPDATE board_post SET hidden = 1 WHERE post_id = ?";
            case "comment" -> "UPDATE board_comment SET hidden = 1 WHERE comment_id = ?";
            case "item" -> "UPDATE shop_item SET hidden = 1 WHERE item_id = ?";
            default -> throw new IllegalArgumentException(targetType);
        };
        jdbcTemplate.update(sql, targetId);
    }

    public void deleteByReporterId(Long reporterId) {
        jdbcTemplate.update("DELETE FROM report WHERE reporter_id = ?", reporterId);
    }
}
