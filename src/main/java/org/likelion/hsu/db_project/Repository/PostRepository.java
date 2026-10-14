package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Post;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    // 작성자 닉네임, 댓글 수, 대표 이미지를 함께 조회
    private static final String SELECT_POST =
            "SELECT p.*, u.nickname, " +
            "(SELECT COUNT(*) FROM board_comment c WHERE c.post_id = p.post_id AND c.deleted = 0) AS comment_count, " +
            "(SELECT i.file_name FROM board_post_image i WHERE i.post_id = p.post_id ORDER BY i.id LIMIT 1) AS thumbnail " +
            "FROM board_post p JOIN users u ON u.user_id = p.user_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> Post.builder()
            .postId(rs.getLong("post_id"))
            .userId(rs.getLong("user_id"))
            .category(rs.getString("category"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .viewCount(rs.getInt("view_count"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .nickname(rs.getString("nickname"))
            .commentCount(rs.getInt("comment_count"))
            .thumbnail(rs.getString("thumbnail"))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ✅ 등록 (생성된 PK 반환)
    public Long save(Post post) {
        String sql = "INSERT INTO board_post (user_id, category, title, content, created_at) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, post.getUserId());
            ps.setString(2, post.getCategory());
            ps.setString(3, post.getTitle());
            ps.setString(4, post.getContent());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    // ✅ 상세 조회
    public Optional<Post> findById(Long id) {
        List<Post> result = jdbcTemplate.query(SELECT_POST + "WHERE p.post_id = ?", postRowMapper, id);
        return result.stream().findFirst();
    }

    // ✅ 목록 (말머리 + 검색 + 페이지)
    public List<Post> findPage(String category, String search, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        String sql = SELECT_POST + where(category, search, params) + "ORDER BY p.post_id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql, postRowMapper, params.toArray());
    }

    public long count(String category, String search) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM board_post p " + where(category, search, params);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private String where(String category, String search, List<Object> params) {
        StringBuilder sql = new StringBuilder("WHERE 1=1 ");
        if (category != null && !category.isEmpty()) {
            sql.append("AND p.category = ? ");
            params.add(category);
        }
        if (search != null && !search.isEmpty()) {
            sql.append("AND (p.title LIKE ? OR p.content LIKE ?) ");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
        }
        return sql.toString();
    }

    // ✅ 수정
    public int update(Long id, Post post) {
        String sql = "UPDATE board_post SET category=?, title=?, content=?, updated_at=? WHERE post_id=?";
        return jdbcTemplate.update(sql, post.getCategory(), post.getTitle(), post.getContent(),
                Timestamp.valueOf(LocalDateTime.now()), id);
    }

    // ✅ 삭제
    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM board_post WHERE post_id = ?", id);
    }

    // ✅ 조회수 증가
    public void increaseViewCount(Long id) {
        jdbcTemplate.update("UPDATE board_post SET view_count = view_count + 1 WHERE post_id = ?", id);
    }

    // ✅ 이미지
    public void saveImage(Long postId, String fileName) {
        jdbcTemplate.update("INSERT INTO board_post_image (post_id, file_name) VALUES (?, ?)", postId, fileName);
    }

    public List<String> findImages(Long postId) {
        return jdbcTemplate.queryForList(
                "SELECT file_name FROM board_post_image WHERE post_id = ? ORDER BY id", String.class, postId);
    }

    public void deleteImage(Long postId, String fileName) {
        jdbcTemplate.update("DELETE FROM board_post_image WHERE post_id = ? AND file_name = ?", postId, fileName);
    }

    public void deleteImages(Long postId) {
        jdbcTemplate.update("DELETE FROM board_post_image WHERE post_id = ?", postId);
    }
}
