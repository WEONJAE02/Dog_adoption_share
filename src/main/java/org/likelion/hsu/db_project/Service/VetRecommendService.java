package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Dto.VetRecommendationDto;
import org.likelion.hsu.db_project.Entity.Vet;
import org.likelion.hsu.db_project.Repository.VetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 진료 분야에 맞는 주치의 추천. AI는 진료 분야만 정하고, 순위는 아래 점수로 코드가 정한다.
 *
 * 점수 (최대 100)
 * - 분야: 주 분야 일치 50, 함께 보는 분야 25 (일반 진료는 모든 수의사가 25)
 * - 지역: 같은 시도 10, 같은 시군구 +10
 * - 해당 분야 진료 건수: 같은 주 분야 수의사 중 가장 많은 사람 대비 최대 15
 * - 평점: 3.0 이하 0 ~ 5.0 10
 * - 응급일 때 24시간 진료 5
 */
@Service
@RequiredArgsConstructor
public class VetRecommendService {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 10;

    private final VetRepository vetRepository;

    public List<VetRecommendationDto> recommend(String specialty, String sido, String sigungu, String urgency, Integer limit) {
        if (specialty == null || !HealthCheckService.SPECIALTIES.contains(specialty)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "진료 분야가 올바르지 않습니다.");
        }
        int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        return rank(vetRepository.findAll(), specialty, blankToNull(sido), blankToNull(sigungu),
                HealthResult.EMERGENCY.equals(urgency)).stream().limit(size).toList();
    }

    /** 수의사가 있는 지역 (시도 → 시군구), 화면의 지역 선택용 */
    public Map<String, List<String>> regions() {
        return vetRepository.findAll().stream().collect(Collectors.groupingBy(Vet::getSido, TreeMap::new,
                Collectors.collectingAndThen(Collectors.mapping(Vet::getSigungu, Collectors.toCollection(TreeSet::new)), ArrayList::new)));
    }

    /** 점수 계산과 정렬 (DB 없이 테스트할 수 있게 분리) */
    static List<VetRecommendationDto> rank(List<Vet> vets, String specialty, String sido, String sigungu, boolean emergency) {
        boolean general = "일반".equals(specialty);
        int maxCases = vets.stream().filter(v -> specialty.equals(v.getSpecialty())).mapToInt(Vet::getCasesCount).max().orElse(0);

        List<VetRecommendationDto> result = new ArrayList<>();
        for (Vet v : vets) {
            List<String> reasons = new ArrayList<>();
            double score = 0;

            if (specialty.equals(v.getSpecialty())) {
                score += 50;
                reasons.add(specialty + " 주 분야");
                if (maxCases > 0) {
                    score += 15.0 * v.getCasesCount() / maxCases;
                    reasons.add(String.format("%s 진료 %,d건", specialty, v.getCasesCount()));
                }
            } else if (v.getSubSpecialties().contains(specialty) || general) {
                score += 25;
                reasons.add(specialty + "도 진료");
            } else {
                continue;   // 분야가 맞지 않으면 추천하지 않는다
            }

            if (sido != null && sido.equals(v.getSido())) {
                score += 10;
                if (sigungu != null && sigungu.equals(v.getSigungu())) {
                    score += 10;
                    reasons.add("같은 " + v.getSigungu());
                } else {
                    reasons.add("같은 " + shortSido(v.getSido()));
                }
            }

            double rating = v.getRating().doubleValue();
            score += Math.max(0, Math.min(10, (rating - 3.0) / 2.0 * 10));
            if (rating >= 4.5) {
                reasons.add(String.format("평점 %.1f (%,d명)", rating, v.getReviewCount()));
            }

            if (emergency && v.isOpen24h()) {
                score += 5;
                reasons.add("24시간 진료");
            }

            result.add(toDto(v, (int) Math.round(score), reasons));
        }

        result.sort(Comparator.comparingInt(VetRecommendationDto::getScore).reversed()
                .thenComparing(VetRecommendationDto::getRating, Comparator.reverseOrder())
                .thenComparing(VetRecommendationDto::getCasesCount, Comparator.reverseOrder()));
        return result;
    }

    // "서울특별시" → "서울", "충청북도" → "충북"
    static String shortSido(String sido) {
        if (sido.matches("^(충청|전라|경상)(북|남)도$")) {
            return "" + sido.charAt(0) + sido.charAt(2);
        }
        return sido.replaceAll("(특별자치도|특별자치시|특별시|광역시|도)$", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static VetRecommendationDto toDto(Vet v, int score, List<String> reasons) {
        return VetRecommendationDto.builder()
                .vetId(v.getVetId())
                .name(v.getName())
                .clinicName(v.getClinicName())
                .sido(v.getSido())
                .sigungu(v.getSigungu())
                .specialty(v.getSpecialty())
                .subSpecialties(v.getSubSpecialties())
                .careerYears(v.getCareerYears())
                .casesCount(v.getCasesCount())
                .rating(v.getRating())
                .reviewCount(v.getReviewCount())
                .open24h(v.isOpen24h())
                .intro(v.getIntro())
                .sample(v.isSample())
                .score(score)
                .reasons(reasons)
                .build();
    }
}
