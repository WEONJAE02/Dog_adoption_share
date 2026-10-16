package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MyPetRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MyPet> petRowMapper = (rs, rowNum) -> MyPet.builder()
            .petId(rs.getLong("pet_id"))
            .userId(rs.getLong("user_id"))
            .name(rs.getString("name"))
            .species(rs.getString("species"))
            .breed(rs.getString("breed"))
            .gender(rs.getString("gender"))
            .neutered(rs.getObject("neutered") != null ? rs.getBoolean("neutered") : null)
            .birthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null)
            .weightKg(rs.getBigDecimal("weight_kg"))
            .notes(rs.getString("notes"))
            .imageName(rs.getString("image_name"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .build();

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ✅ 등록 (생성된 PK 반환)
    public Long save(MyPet pet) {
        String sql = "INSERT INTO my_pet (user_id, name, species, breed, gender, neutered, birth_year, weight_kg, notes, image_name, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, pet.getUserId());
            ps.setString(2, pet.getName());
            ps.setString(3, pet.getSpecies());
            ps.setString(4, pet.getBreed());
            ps.setString(5, pet.getGender());
            ps.setObject(6, pet.getNeutered(), Types.TINYINT);
            ps.setObject(7, pet.getBirthYear(), Types.INTEGER);
            ps.setBigDecimal(8, pet.getWeightKg());
            ps.setString(9, pet.getNotes());
            ps.setString(10, pet.getImageName());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<MyPet> findById(Long petId) {
        return jdbcTemplate.query("SELECT * FROM my_pet WHERE pet_id = ?", petRowMapper, petId).stream().findFirst();
    }

    // ✅ 내 반려동물 (먼저 등록한 순)
    public List<MyPet> findByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM my_pet WHERE user_id = ? ORDER BY pet_id", petRowMapper, userId);
    }

    public int countByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM my_pet WHERE user_id = ?", Integer.class, userId);
        return count != null ? count : 0;
    }

    // ✅ 수정 (사진 포함)
    public void update(Long petId, MyPet pet) {
        jdbcTemplate.update(
                "UPDATE my_pet SET name=?, species=?, breed=?, gender=?, neutered=?, birth_year=?, weight_kg=?, notes=?, image_name=?, updated_at=? WHERE pet_id=?",
                pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getGender(), pet.getNeutered(), pet.getBirthYear(),
                pet.getWeightKg(), pet.getNotes(), pet.getImageName(), Timestamp.valueOf(LocalDateTime.now()), petId);
    }

    public void delete(Long petId) {
        jdbcTemplate.update("DELETE FROM my_pet WHERE pet_id = ?", petId);
    }
}
