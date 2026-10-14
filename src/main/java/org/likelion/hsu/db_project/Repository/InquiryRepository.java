package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Inquiry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InquiryRepository {

    // 유기견 이름·대표 사진, 보낸 사람·받는 사람 닉네임을 함께 조회
    private static final String SELECT_INQUIRY =
            "SELECT q.*, d.name AS dog_name, d.image_name AS dog_image, " +
            "s.nickname AS sender_nickname, r.nickname AS receiver_nickname " +
            "FROM adoption_inquiry q " +
            "JOIN dog d ON d.dog_id = q.dog_id " +
            "JOIN users s ON s.user_id = q.sender_id " +
            "JOIN users r ON r.user_id = q.receiver_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Inquiry> inquiryRowMapper = (rs, rowNum) -> Inquiry.builder()
            .inquiryId(rs.getLong("inquiry_id"))
            .dogId(rs.getLong("dog_id"))
            .senderId(rs.getLong("sender_id"))
            .receiverId(rs.getLong("receiver_id"))
            .message(rs.getString("message"))
            .contact(rs.getString("contact"))
            .reply(rs.getString("reply"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .repliedAt(toLocalDateTime(rs.getTimestamp("replied_at")))
            .dogName(rs.getString("dog_name"))
            .dogImage(rs.getString("dog_image"))
            .senderNickname(rs.getString("sender_nickname"))
            .receiverNickname(rs.getString("receiver_nickname"))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ✅ 등록
    public void save(Inquiry inquiry) {
        String sql = "INSERT INTO adoption_inquiry (dog_id, sender_id, receiver_id, message, contact, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, inquiry.getDogId(), inquiry.getSenderId(), inquiry.getReceiverId(),
                inquiry.getMessage(), inquiry.getContact(), Timestamp.valueOf(LocalDateTime.now()));
    }

    public Optional<Inquiry> findById(Long id) {
        List<Inquiry> result = jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.inquiry_id = ?", inquiryRowMapper, id);
        return result.stream().findFirst();
    }

    // ✅ 받은 문의 / 보낸 문의 (최신순)
    public List<Inquiry> findByReceiverId(Long userId) {
        return jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.receiver_id = ? ORDER BY q.inquiry_id DESC", inquiryRowMapper, userId);
    }

    public List<Inquiry> findBySenderId(Long userId) {
        return jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.sender_id = ? ORDER BY q.inquiry_id DESC", inquiryRowMapper, userId);
    }

    // 같은 유기견에 답변을 기다리는 문의가 이미 있는지
    public boolean existsPending(Long dogId, Long senderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM adoption_inquiry WHERE dog_id = ? AND sender_id = ? AND reply IS NULL",
                Integer.class, dogId, senderId);
        return count != null && count > 0;
    }

    // 답변을 기다리는 받은 문의 수 (문의함 배지)
    public int countPendingReceived(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM adoption_inquiry WHERE receiver_id = ? AND reply IS NULL", Integer.class, userId);
        return count != null ? count : 0;
    }

    // ✅ 답변
    public void reply(Long id, String reply) {
        jdbcTemplate.update("UPDATE adoption_inquiry SET reply = ?, replied_at = ? WHERE inquiry_id = ?",
                reply, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    // ✅ 회원 탈퇴 시 주고받은 문의 삭제
    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM adoption_inquiry WHERE sender_id = ? OR receiver_id = ?", userId, userId);
    }

    // ✅ 유기견 삭제 시 함께 삭제
    public void deleteByDogId(Long dogId) {
        jdbcTemplate.update("DELETE FROM adoption_inquiry WHERE dog_id = ?", dogId);
    }
}
