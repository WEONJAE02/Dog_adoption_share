package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.DogDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DogDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DogDetail> detailRowMapper = new RowMapper<>() {
        @Override
        public DogDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DogDetail.builder()
                    .id(rs.getLong("id"))
                    .foundPlace(rs.getString("found_place"))
                    .memo(rs.getString("memo"))
                    .build();
        }
    };

    // 등록
    public void save(DogDetail detail, Long dogId) {
        String sql = "INSERT INTO dog_detail (dog_id, found_place, memo) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, dogId, detail.getFoundPlace(), detail.getMemo());
    }

    // 조회
    public DogDetail findByDogId(Long dogId) {
        String sql = "SELECT * FROM dog_detail WHERE dog_id = ?";
        List<DogDetail> list = jdbcTemplate.query(sql, detailRowMapper, dogId);
        return list.isEmpty() ? null : list.get(0);
    }

    // 수정
    public void update(Long dogId, DogDetail detail) {
        String sql = "UPDATE dog_detail SET found_place=?, memo=? WHERE dog_id=?";
        jdbcTemplate.update(sql, detail.getFoundPlace(), detail.getMemo(), dogId);
    }

    // 삭제
    public void deleteByDogId(Long dogId) {
        String sql = "DELETE FROM dog_detail WHERE dog_id = ?";
        jdbcTemplate.update(sql, dogId);
    }
}
