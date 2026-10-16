package org.likelion.hsu.db_project.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.likelion.hsu.db_project.Entity.Vet;
import org.likelion.hsu.db_project.Repository.VetRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주치의 추천을 시연하기 위한 샘플 수의사 데이터. vet 테이블이 비어 있을 때만 넣는다.
 * 모두 가상의 인물·병원이며("펫룸 ○○"), 실제 병원 정보가 아니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleVetSeeder implements ApplicationRunner {

    private final VetRepository vetRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (vetRepository.count() > 0) {
            return;
        }
        SAMPLES.forEach(vetRepository::save);
        log.info("[Vet] 샘플 수의사 {}명을 넣었습니다.", SAMPLES.size());
    }

    private static Vet vet(String name, String clinic, String sido, String sigungu, String specialty, String subs,
                           int career, int cases, String rating, int reviews, boolean open24h, String intro) {
        return Vet.builder()
                .name(name).clinicName(clinic).sido(sido).sigungu(sigungu).specialty(specialty)
                .subSpecialties(subs.isEmpty() ? List.of() : List.of(subs.split(",")))
                .careerYears(career).casesCount(cases).rating(new BigDecimal(rating)).reviewCount(reviews)
                .open24h(open24h).intro(intro).sample(true)
                .build();
    }

    static final List<Vet> SAMPLES = List.of(
            // 서울
            vet("한서윤", "펫룸 마포 안과클리닉", "서울특별시", "마포구", "안과", "일반", 12, 1840, "4.8", 312, false, "백내장·녹내장 검사와 각막 질환을 주로 봐요."),
            vet("오지훈", "펫룸 마포 동물의료센터", "서울특별시", "마포구", "내과", "일반,피부과", 9, 2210, "4.6", 401, true, "24시간 응급 진료와 내과 질환을 함께 봐요."),
            vet("정하린", "펫룸 강남 피부클리닉", "서울특별시", "강남구", "피부과", "일반", 14, 2630, "4.9", 528, false, "아토피·알레르기성 피부염과 귀 질환을 오래 봐 왔어요."),
            vet("문태호", "펫룸 강남 정형외과", "서울특별시", "강남구", "정형외과", "신경과", 16, 1980, "4.7", 365, false, "슬개골 탈구와 십자인대 수술을 주로 해요."),
            vet("윤채원", "펫룸 송파 치과클리닉", "서울특별시", "송파구", "치과", "일반", 8, 1320, "4.7", 244, false, "마취 스케일링과 발치, 구강 종양 검사를 해요."),
            vet("백승우", "펫룸 송파 24시 동물의료센터", "서울특별시", "송파구", "일반", "내과,정형외과", 11, 3050, "4.5", 612, true, "야간·주말 응급 환자를 받는 24시간 병원이에요."),
            vet("서민아", "펫룸 성동 신경클리닉", "서울특별시", "성동구", "신경과", "정형외과", 13, 860, "4.8", 151, false, "디스크와 경련 질환을 MRI 판독과 함께 봐요."),
            vet("장도윤", "펫룸 은평 동물병원", "서울특별시", "은평구", "일반", "피부과,치과", 6, 1490, "4.4", 207, false, "예방접종과 건강검진, 가벼운 피부·치아 질환을 봐요."),
            vet("임유진", "펫룸 관악 고양이클리닉", "서울특별시", "관악구", "내과", "치과", 10, 1710, "4.9", 389, false, "고양이 신장·갑상선 질환과 구내염을 주로 봐요."),
            vet("강예준", "펫룸 은평 안과·피부센터", "서울특별시", "은평구", "피부과", "안과", 7, 1180, "4.5", 176, false, "피부염과 눈물·결막염을 함께 봐요."),
            // 경기·인천
            vet("조현서", "펫룸 분당 동물의료센터", "경기도", "성남시", "정형외과", "일반", 15, 2340, "4.8", 455, true, "골절·관절 수술과 24시간 입원 관리를 해요."),
            vet("신지안", "펫룸 일산 안과클리닉", "경기도", "고양시", "안과", "", 9, 1270, "4.7", 198, false, "안압 검사와 각막 궤양, 체리아이 수술을 해요."),
            vet("홍시우", "펫룸 수원 피부클리닉", "경기도", "수원시", "피부과", "일반", 8, 1560, "4.6", 233, false, "곰팡이·세균성 피부염과 외이염을 주로 봐요."),
            vet("권나연", "펫룸 수지 치과클리닉", "경기도", "용인시", "치과", "", 11, 1090, "4.8", 167, false, "치주 질환과 부러진 이 치료를 해요."),
            vet("류건우", "펫룸 용인 24시 동물의료센터", "경기도", "용인시", "내과", "신경과,일반", 13, 2780, "4.6", 502, true, "구토·설사 같은 소화기 질환과 응급 진료를 해요."),
            vet("남궁솔", "펫룸 송도 동물병원", "인천광역시", "연수구", "일반", "안과,피부과", 7, 1640, "4.5", 221, false, "건강검진과 눈·피부 초기 진료를 함께 봐요."),
            vet("배서진", "펫룸 송도 정형·신경센터", "인천광역시", "연수구", "신경과", "정형외과", 12, 740, "4.7", 133, false, "보행 이상과 디스크 재활을 함께 봐요."),
            // 부산·대구·대전·광주
            vet("황민재", "펫룸 해운대 동물의료센터", "부산광역시", "해운대구", "내과", "일반", 14, 2510, "4.7", 418, true, "24시간 응급과 내과 입원 치료를 해요."),
            vet("송가은", "펫룸 해운대 안과클리닉", "부산광역시", "해운대구", "안과", "일반", 10, 1380, "4.8", 209, false, "백내장 수술 전 검사와 안구 건조증을 봐요."),
            vet("전우빈", "펫룸 서면 피부·치과", "부산광역시", "부산진구", "피부과", "치과", 9, 1440, "4.6", 254, false, "피부염과 치석 관리를 함께 봐요."),
            vet("곽다은", "펫룸 수성 정형외과", "대구광역시", "수성구", "정형외과", "", 11, 1620, "4.7", 187, false, "슬개골·고관절 질환 수술과 재활을 해요."),
            vet("이태민", "펫룸 수성 24시 동물병원", "대구광역시", "수성구", "일반", "내과", 8, 2050, "4.4", 296, true, "야간 응급과 일반 진료를 해요."),
            vet("최지우", "펫룸 유성 동물의료센터", "대전광역시", "유성구", "내과", "피부과,일반", 12, 1950, "4.6", 274, true, "내과·피부 질환과 24시간 응급을 봐요."),
            vet("김도현", "펫룸 유성 치과·안과", "대전광역시", "유성구", "치과", "안과", 9, 980, "4.5", 142, false, "스케일링과 눈 질환 초기 진료를 해요."),
            vet("박소율", "펫룸 광주 동물의료센터", "광주광역시", "서구", "일반", "내과,피부과,정형외과", 10, 2220, "4.5", 318, true, "24시간 응급과 일반 진료를 하는 병원이에요."),
            vet("유하준", "펫룸 광주 신경·정형", "광주광역시", "서구", "신경과", "정형외과", 15, 690, "4.8", 118, false, "경련·마비 질환과 척추 수술 상담을 해요.")
    );
}
