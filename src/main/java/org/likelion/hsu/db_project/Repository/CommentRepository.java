package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private static final String SELECT_COMMENT =
            "SELECT c.*, u.nickname FROM board_comment c JOIN users u ON u.user_id = c.user_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> Comment.builder()
            .commentId(rs.getLong("comment_id"))
            .postId(rs.getLong("post_id"))
            .userId(rs.getLong("user_id"))
            .parentId(rs.getObject("parent_id") != null ? rs.getLong("parent_id") : null)
            .content(rs.getString("content"))
            .deleted(rs.getBoolean("deleted"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime()
                    : null)
            .nickname(rs.getString("nickname"))
            .build();

    // ✅ 등록
    public void save(Comment comment) {
        String sql = "INSERT INTO board_comment (post_id, user_id, parent_id, content, created_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, comment.getPostId(), comment.getUserId(), comment.getParentId(),
                comment.getContent(), Timestamp.valueOf(LocalDateTime.now()));
    }

    public Optional<Comment> findById(Long id) {
        List<Comment> result = jdbcTemplate.query(SELECT_COMMENT + "WHERE c.comment_id = ?", commentRowMapper, id);
        return result.stream().findFirst();
    }

    // ✅ 게시글의 댓글 전체 (작성 순)
    public List<Comment> findByPostId(Long postId) {
        return jdbcTemplate.query(SELECT_COMMENT + "WHERE c.post_id = ? ORDER BY c.comment_id", commentRowMapper, postId);
    }

    public int countReplies(Long commentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM board_comment WHERE parent_id = ?", Integer.class, commentId);
        return count != null ? count : 0;
    }

    // ✅ 답글이 남아 있는 댓글은 내용만 가린다
    public void markDeleted(Long id) {
        jdbcTemplate.update("UPDATE board_comment SET deleted = 1 WHERE comment_id = ?", id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM board_comment WHERE comment_id = ?", id);
    }

    public void deleteByPostId(Long postId) {
        jdbcTemplate.update("DELETE FROM board_comment WHERE post_id = ?", postId);
    }
}
