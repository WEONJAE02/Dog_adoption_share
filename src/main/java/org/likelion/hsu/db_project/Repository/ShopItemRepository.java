package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.ShopItem;
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
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShopItemRepository {

    // 판매자 닉네임, 대표 이미지, 관심 수, 보는 사람의 관심 여부를 함께 조회 (첫 번째 ? 는 보는 사람 id)
    private static final String SELECT_ITEM =
            "SELECT s.*, u.nickname, " +
            "(SELECT i.file_name FROM shop_item_image i WHERE i.item_id = s.item_id ORDER BY i.id LIMIT 1) AS thumbnail, " +
            "(SELECT COUNT(*) FROM shop_item_like l WHERE l.item_id = s.item_id) AS like_count, " +
            "EXISTS (SELECT 1 FROM shop_item_like l WHERE l.item_id = s.item_id AND l.user_id = ?) AS liked " +
            "FROM shop_item s JOIN users u ON u.user_id = s.user_id ";

    // 정렬은 고정값만 SQL에 들어간다
    private static final Map<String, String> ORDER_BY = Map.of(
            "latest", "s.item_id DESC",
            "priceAsc", "s.price ASC, s.item_id DESC",
            "priceDesc", "s.price DESC, s.item_id DESC",
            "popular", "like_count DESC, s.view_count DESC, s.item_id DESC");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ShopItem> itemRowMapper = (rs, rowNum) -> ShopItem.builder()
            .itemId(rs.getLong("item_id"))
            .userId(rs.getLong("user_id"))
            .category(rs.getString("category"))
            .title(rs.getString("title"))
            .price(rs.getInt("price"))
            .description(rs.getString("description"))
            .tradeArea(rs.getString("trade_area"))
            .status(rs.getString("status"))
            .viewCount(rs.getInt("view_count"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .hidden(rs.getBoolean("hidden"))
            .nickname(rs.getString("nickname"))
            .thumbnail(rs.getString("thumbnail"))
            .likeCount(rs.getInt("like_count"))
            .liked(rs.getBoolean("liked"))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /** 목록 조건 (null/빈 값은 조건 없음) */
    public record Filter(String category, String search, boolean availableOnly, String filter, Long viewerId) {}

    // ✅ 등록 (생성된 PK 반환)
    public Long save(ShopItem item) {
        String sql = "INSERT INTO shop_item (user_id, category, title, price, description, trade_area, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getUserId());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getTitle());
            ps.setInt(4, item.getPrice());
            ps.setString(5, item.getDescription());
            ps.setString(6, item.getTradeArea());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    // ✅ 상세 조회
    public Optional<ShopItem> findById(Long id, Long viewerId) {
        List<ShopItem> result = jdbcTemplate.query(SELECT_ITEM + "WHERE s.item_id = ?", itemRowMapper, viewerId, id);
        return result.stream().findFirst();
    }

    // ✅ 목록 (카테고리 + 검색 + 거래 가능만 + 관심/내 판매 + 정렬 + 페이지)
    public List<ShopItem> findPage(Filter f, String sort, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        params.add(f.viewerId());
        String sql = SELECT_ITEM + where(f, params)
                + "ORDER BY " + ORDER_BY.getOrDefault(sort, ORDER_BY.get("latest")) + " LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql, itemRowMapper, params.toArray());
    }

    public long count(Filter f) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM shop_item s " + where(f, params);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0;
    }

    // 숨겨진 상품은 판매자 본인(viewerId)에게만 보인다
    private String where(Filter f, List<Object> params) {
        StringBuilder sql = new StringBuilder("WHERE (s.hidden = 0 OR s.user_id = ?) ");
        params.add(f.viewerId());
        if (f.category() != null && !f.category().isEmpty()) {
            sql.append("AND s.category = ? ");
            params.add(f.category());
        }
        if (f.search() != null && !f.search().isEmpty()) {
            sql.append("AND (s.title LIKE ? OR s.description LIKE ? OR s.trade_area LIKE ?) ");
            String like = "%" + f.search() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (f.availableOnly()) {
            sql.append("AND s.status <> '거래완료' ");
        }
        if ("mine".equals(f.filter())) {
            sql.append("AND s.user_id = ? ");
            params.add(f.viewerId());
        } else if ("liked".equals(f.filter())) {
            sql.append("AND EXISTS (SELECT 1 FROM shop_item_like l WHERE l.item_id = s.item_id AND l.user_id = ?) ");
            params.add(f.viewerId());
        }
        return sql.toString();
    }

    // ✅ 수정
    public int update(Long id, ShopItem item) {
        String sql = "UPDATE shop_item SET category=?, title=?, price=?, description=?, trade_area=?, updated_at=? WHERE item_id=?";
        return jdbcTemplate.update(sql, item.getCategory(), item.getTitle(), item.getPrice(), item.getDescription(),
                item.getTradeArea(), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    // ✅ 거래 상태 변경
    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update("UPDATE shop_item SET status = ? WHERE item_id = ?", status, id);
    }

    // ✅ 삭제
    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM shop_item WHERE item_id = ?", id);
    }

    public List<Long> findIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList("SELECT item_id FROM shop_item WHERE user_id = ?", Long.class, userId);
    }

    // ✅ 조회수 증가
    public void increaseViewCount(Long id) {
        jdbcTemplate.update("UPDATE shop_item SET view_count = view_count + 1 WHERE item_id = ?", id);
    }

    // ✅ 이미지
    public void saveImage(Long itemId, String fileName) {
        jdbcTemplate.update("INSERT INTO shop_item_image (item_id, file_name) VALUES (?, ?)", itemId, fileName);
    }

    public List<String> findImages(Long itemId) {
        return jdbcTemplate.queryForList(
                "SELECT file_name FROM shop_item_image WHERE item_id = ? ORDER BY id", String.class, itemId);
    }

    public void deleteImage(Long itemId, String fileName) {
        jdbcTemplate.update("DELETE FROM shop_item_image WHERE item_id = ? AND file_name = ?", itemId, fileName);
    }

    public void deleteImages(Long itemId) {
        jdbcTemplate.update("DELETE FROM shop_item_image WHERE item_id = ?", itemId);
    }

    // ✅ 관심 (이미 있으면 무시)
    public void like(Long itemId, Long userId) {
        jdbcTemplate.update("INSERT IGNORE INTO shop_item_like (item_id, user_id, created_at) VALUES (?, ?, ?)",
                itemId, userId, Timestamp.valueOf(LocalDateTime.now()));
    }

    public void unlike(Long itemId, Long userId) {
        jdbcTemplate.update("DELETE FROM shop_item_like WHERE item_id = ? AND user_id = ?", itemId, userId);
    }

    public void deleteLikes(Long itemId) {
        jdbcTemplate.update("DELETE FROM shop_item_like WHERE item_id = ?", itemId);
    }

    public void deleteLikesByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM shop_item_like WHERE user_id = ?", userId);
    }
}
