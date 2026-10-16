package org.likelion.hsu.db_project.Repository;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.Vet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VetRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Vet> vetRowMapper = (rs, rowNum) -> Vet.builder()
            .vetId(rs.getLong("vet_id"))
            .name(rs.getString("name"))
            .clinicName(rs.getString("clinic_name"))
            .sido(rs.getString("sido"))
            .sigungu(rs.getString("sigungu"))
            .specialty(rs.getString("specialty"))
            .subSpecialties(split(rs.getString("sub_specialties")))
            .careerYears(rs.getInt("career_years"))
            .casesCount(rs.getInt("cases_count"))
            .rating(rs.getBigDecimal("rating"))
            .reviewCount(rs.getInt("review_count"))
            .open24h(rs.getBoolean("open_24h"))
            .intro(rs.getString("intro"))
            .sample(rs.getBoolean("is_sample"))
            .build();

    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(String::trim).toList();
    }

    // 샘플 수의사는 수십 명이라 한 번에 읽어 점수를 계산한다
    public List<Vet> findAll() {
        return jdbcTemplate.query("SELECT * FROM vet ORDER BY vet_id", vetRowMapper);
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vet", Integer.class);
        return count != null ? count : 0;
    }

    public void save(Vet vet) {
        jdbcTemplate.update(
                "INSERT INTO vet (name, clinic_name, sido, sigungu, specialty, sub_specialties, career_years, cases_count, rating, review_count, open_24h, intro, is_sample) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                vet.getName(), vet.getClinicName(), vet.getSido(), vet.getSigungu(), vet.getSpecialty(),
                vet.getSubSpecialties() == null ? null : String.join(",", vet.getSubSpecialties()),
                vet.getCareerYears(), vet.getCasesCount(), vet.getRating(), vet.getReviewCount(),
                vet.isOpen24h(), vet.getIntro(), vet.isSample());
    }
}
