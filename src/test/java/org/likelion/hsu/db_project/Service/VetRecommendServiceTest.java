package org.likelion.hsu.db_project.Service;

import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Dto.VetRecommendationDto;
import org.likelion.hsu.db_project.Entity.Vet;
import org.likelion.hsu.db_project.Repository.VetRepository;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VetRecommendServiceTest {

    private static Vet vet(long id, String specialty, List<String> subs, String sido, String sigungu, int cases, String rating, boolean open24h) {
        return Vet.builder().vetId(id).name("수의사" + id).clinicName("펫룸 " + id).sido(sido).sigungu(sigungu)
                .specialty(specialty).subSpecialties(subs).careerYears(10).casesCount(cases)
                .rating(new BigDecimal(rating)).reviewCount(100).open24h(open24h).sample(true).build();
    }

    private static List<Long> ids(List<VetRecommendationDto> list) {
        return list.stream().map(VetRecommendationDto::getVetId).toList();
    }

    @Test
    void 주_분야가_함께_보는_분야보다_앞선다() {
        List<Vet> vets = List.of(
                vet(1, "피부과", List.of("안과"), "서울특별시", "마포구", 3000, "5.0", false),
                vet(2, "안과", List.of(), "부산광역시", "해운대구", 500, "4.0", false));

        List<VetRecommendationDto> r = VetRecommendService.rank(vets, "안과", null, null, false);

        assertEquals(List.of(2L, 1L), ids(r));
        assertTrue(r.get(0).getReasons().contains("안과 주 분야"));
        assertTrue(r.get(1).getReasons().contains("안과도 진료"));
    }

    @Test
    void 분야가_맞지_않으면_추천하지_않는다() {
        List<Vet> vets = List.of(vet(1, "치과", List.of(), "서울특별시", "마포구", 100, "5.0", false));

        assertTrue(VetRecommendService.rank(vets, "안과", null, null, false).isEmpty());
    }

    @Test
    void 같은_분야면_가까운_지역이_앞선다() {
        List<Vet> vets = List.of(
                vet(1, "안과", List.of(), "부산광역시", "해운대구", 1000, "4.8", false),
                vet(2, "안과", List.of(), "서울특별시", "강남구", 1000, "4.8", false),
                vet(3, "안과", List.of(), "서울특별시", "마포구", 1000, "4.8", false));

        List<VetRecommendationDto> r = VetRecommendService.rank(vets, "안과", "서울특별시", "마포구", false);

        assertEquals(List.of(3L, 2L, 1L), ids(r));
        assertEquals(r.get(1).getScore() + 10, r.get(0).getScore());   // 같은 시군구 +10
        assertEquals(r.get(2).getScore() + 10, r.get(1).getScore());   // 같은 시도 +10
        assertTrue(r.get(0).getReasons().contains("같은 마포구"));
        assertTrue(r.get(1).getReasons().contains("같은 서울"));
    }

    @Test
    void 응급이면_24시간_병원에_가산점을_준다() {
        List<Vet> vets = List.of(
                vet(1, "내과", List.of(), "서울특별시", "마포구", 1000, "4.8", false),
                vet(2, "내과", List.of(), "서울특별시", "마포구", 1000, "4.8", true));

        List<VetRecommendationDto> normal = VetRecommendService.rank(vets, "내과", null, null, false);
        List<VetRecommendationDto> emergency = VetRecommendService.rank(vets, "내과", null, null, true);

        assertEquals(normal.get(0).getScore(), normal.get(1).getScore());
        assertEquals(2L, emergency.get(0).getVetId());
        assertEquals(emergency.get(1).getScore() + 5, emergency.get(0).getScore());
        assertTrue(emergency.get(0).getReasons().contains("24시간 진료"));
    }

    @Test
    void 진료_건수와_평점이_많을수록_높고_최대_100점이다() {
        List<Vet> vets = List.of(
                vet(1, "치과", List.of(), "서울특별시", "마포구", 2000, "5.0", true),
                vet(2, "치과", List.of(), "서울특별시", "마포구", 1000, "3.0", false));

        List<VetRecommendationDto> r = VetRecommendService.rank(vets, "치과", "서울특별시", "마포구", true);

        assertEquals(100, r.get(0).getScore());               // 50 + 15 + 20 + 10 + 5
        assertEquals(50 + 8 + 20, r.get(1).getScore());       // 건수 절반 7.5 → 반올림, 평점 3.0 → 0
        assertTrue(r.get(1).getReasons().stream().noneMatch(x -> x.startsWith("평점")));   // 4.5 미만은 이유에 쓰지 않음
    }

    @Test
    void 일반_진료는_모든_수의사가_후보이고_일반_주_분야가_앞선다() {
        List<Vet> vets = List.of(
                vet(1, "안과", List.of(), "서울특별시", "마포구", 1000, "4.8", false),
                vet(2, "일반", List.of(), "서울특별시", "마포구", 1000, "4.8", false));

        assertEquals(List.of(2L, 1L), ids(VetRecommendService.rank(vets, "일반", null, null, false)));
    }

    @Test
    void 목록에_없는_진료_분야는_거부한다() {
        VetRecommendService service = new VetRecommendService(mock(VetRepository.class));

        BoardException e = assertThrows(BoardException.class, () -> service.recommend("한의원", null, null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void 시도_이름을_짧게_줄인다() {
        assertEquals("서울", VetRecommendService.shortSido("서울특별시"));
        assertEquals("경기", VetRecommendService.shortSido("경기도"));
        assertEquals("충북", VetRecommendService.shortSido("충청북도"));
        assertEquals("제주", VetRecommendService.shortSido("제주특별자치도"));
    }
}
