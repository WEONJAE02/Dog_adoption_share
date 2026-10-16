package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .userId(rs.getLong("user_id"))
            .provider(rs.getString("provider"))
            .providerId(rs.getString("provider_id"))
            .email(rs.getString("email"))
            .password(rs.getString("password"))
            .nickname(rs.getString("nickname"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .termsAgreedAt(toLocalDateTime(rs.getTimestamp("terms_agreed_at")))
            .deletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ✅ 등록 (생성된 PK 반환)
    public Long save(User user) {
        String sql = "INSERT INTO users (provider, provider_id, email, password, nickname, created_at, terms_agreed_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getProvider());
            ps.setString(2, user.getProviderId());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getNickname());
            ps.setTimestamp(6, Timestamp.valueOf(createdAt));
            ps.setTimestamp(7, user.getTermsAgreedAt() != null ? Timestamp.valueOf(user.getTermsAgreedAt()) : null);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<User> findById(Long userId) {
        List<User> result = jdbcTemplate.query("SELECT * FROM users WHERE user_id = ? AND deleted_at IS NULL", userRowMapper, userId);
        return result.stream().findFirst();
    }

    // ✅ 약관 동의
    public boolean isTermsAgreed(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ? AND terms_agreed_at IS NOT NULL AND deleted_at IS NULL", Integer.class, userId);
        return count != null && count > 0;
    }

    public void agreeTerms(Long userId) {
        jdbcTemplate.update("UPDATE users SET terms_agreed_at = ? WHERE user_id = ? AND terms_agreed_at IS NULL",
                Timestamp.valueOf(LocalDateTime.now()), userId);
    }

    public void updateNickname(Long userId, String nickname) {
        jdbcTemplate.update("UPDATE users SET nickname = ? WHERE user_id = ?", nickname, userId);
    }

    // ✅ 내 활동 수 (등록 유기견, 글, 댓글, 주고받은 입양 문의)
    public Map<String, Integer> countActivities(Long userId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("dogs", count("SELECT COUNT(*) FROM dog WHERE user_id = ?", userId));
        counts.put("posts", count("SELECT COUNT(*) FROM board_post WHERE user_id = ?", userId));
        counts.put("comments", count("SELECT COUNT(*) FROM board_comment WHERE user_id = ? AND deleted = 0", userId));
        counts.put("inquiries", count("SELECT COUNT(*) FROM adoption_inquiry WHERE sender_id = ? OR receiver_id = ?", userId, userId));
        counts.put("shopItems", count("SELECT COUNT(*) FROM shop_item WHERE user_id = ?", userId));
        counts.put("shopInquiries", count("SELECT COUNT(*) FROM shop_inquiry WHERE sender_id = ? OR receiver_id = ?", userId, userId));
        return counts;
    }

    private int count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null ? count : 0;
    }

    // ✅ 탈퇴: 개인정보를 지우고 "탈퇴한 회원"으로 남긴다 (커뮤니티 글·댓글 작성자 표시용)
    // provider_id 를 바꿔 같은 소셜 계정/이메일로 다시 가입하면 새 회원이 된다
    public void anonymize(Long userId) {
        jdbcTemplate.update(
                "UPDATE users SET provider_id = ?, email = NULL, password = NULL, nickname = '탈퇴한 회원', deleted_at = ? WHERE user_id = ?",
                "deleted:" + userId + ":" + UUID.randomUUID(), Timestamp.valueOf(LocalDateTime.now()), userId);
    }

    // ✅ 로그인 수단(provider) + 식별자로 조회
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        String sql = "SELECT * FROM users WHERE provider = ? AND provider_id = ?";
        List<User> result = jdbcTemplate.query(sql, userRowMapper, provider, providerId);
        return result.stream().findFirst();
    }
}
