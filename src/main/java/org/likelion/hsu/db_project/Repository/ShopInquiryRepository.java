package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.ShopInquiry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShopInquiryRepository {

    // 상품 제목·대표 사진·가격, 보낸 사람·받는 사람 닉네임을 함께 조회
    private static final String SELECT_INQUIRY =
            "SELECT q.*, s.title AS item_title, s.price AS item_price, " +
            "(SELECT i.file_name FROM shop_item_image i WHERE i.item_id = q.item_id ORDER BY i.id LIMIT 1) AS item_image, " +
            "su.nickname AS sender_nickname, ru.nickname AS receiver_nickname " +
            "FROM shop_inquiry q " +
            "JOIN shop_item s ON s.item_id = q.item_id " +
            "JOIN users su ON su.user_id = q.sender_id " +
            "JOIN users ru ON ru.user_id = q.receiver_id ";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ShopInquiry> inquiryRowMapper = (rs, rowNum) -> ShopInquiry.builder()
            .inquiryId(rs.getLong("inquiry_id"))
            .itemId(rs.getLong("item_id"))
            .senderId(rs.getLong("sender_id"))
            .receiverId(rs.getLong("receiver_id"))
            .message(rs.getString("message"))
            .contact(rs.getString("contact"))
            .reply(rs.getString("reply"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .repliedAt(toLocalDateTime(rs.getTimestamp("replied_at")))
            .itemTitle(rs.getString("item_title"))
            .itemImage(rs.getString("item_image"))
            .itemPrice(rs.getInt("item_price"))
            .senderNickname(rs.getString("sender_nickname"))
            .receiverNickname(rs.getString("receiver_nickname"))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ✅ 등록
    public void save(ShopInquiry inquiry) {
        String sql = "INSERT INTO shop_inquiry (item_id, sender_id, receiver_id, message, contact, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, inquiry.getItemId(), inquiry.getSenderId(), inquiry.getReceiverId(),
                inquiry.getMessage(), inquiry.getContact(), Timestamp.valueOf(LocalDateTime.now()));
    }

    public Optional<ShopInquiry> findById(Long id) {
        List<ShopInquiry> result = jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.inquiry_id = ?", inquiryRowMapper, id);
        return result.stream().findFirst();
    }

    // ✅ 받은 문의 / 보낸 문의 (최신순)
    public List<ShopInquiry> findByReceiverId(Long userId) {
        return jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.receiver_id = ? ORDER BY q.inquiry_id DESC", inquiryRowMapper, userId);
    }

    public List<ShopInquiry> findBySenderId(Long userId) {
        return jdbcTemplate.query(SELECT_INQUIRY + "WHERE q.sender_id = ? ORDER BY q.inquiry_id DESC", inquiryRowMapper, userId);
    }

    // 같은 상품에 답변을 기다리는 문의가 이미 있는지
    public boolean existsPending(Long itemId, Long senderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shop_inquiry WHERE item_id = ? AND sender_id = ? AND reply IS NULL",
                Integer.class, itemId, senderId);
        return count != null && count > 0;
    }

    // 답변을 기다리는 받은 문의 수 (문의함 배지)
    public int countPendingReceived(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shop_inquiry WHERE receiver_id = ? AND reply IS NULL", Integer.class, userId);
        return count != null ? count : 0;
    }

    // ✅ 답변
    public void reply(Long id, String reply) {
        jdbcTemplate.update("UPDATE shop_inquiry SET reply = ?, replied_at = ? WHERE inquiry_id = ?",
                reply, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    // ✅ 회원 탈퇴 시 주고받은 문의 삭제
    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM shop_inquiry WHERE sender_id = ? OR receiver_id = ?", userId, userId);
    }

    // ✅ 상품 삭제 시 함께 삭제
    public void deleteByItemId(Long itemId) {
        jdbcTemplate.update("DELETE FROM shop_inquiry WHERE item_id = ?", itemId);
    }
}
