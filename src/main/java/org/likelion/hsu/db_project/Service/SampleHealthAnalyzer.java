package org.likelion.hsu.db_project.Service;

import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Dto.HealthResult.Finding;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 연결 전에 쓰는 샘플 분석기. 사진을 보지 않고 부위별로 정해진 결과를 돌려준다.
 * 화면·저장·추천 흐름을 AI 없이 개발하고 시연하기 위한 용도다.
 */
@Component
public class SampleHealthAnalyzer implements HealthAnalyzer {

    private record Sample(String urgency, String observation, String summary, List<String> causes,
                          String specialty, List<String> homeCare, List<String> questions) {}

    private static final Map<String, Sample> SAMPLES = Map.of(
            "눈", new Sample(HealthResult.SOON,
                    "흰자위가 붉고 눈 안쪽에 눈곱이 보입니다.",
                    "결막이 충혈되어 보이고 눈곱이 있어 결막염 같은 눈 표면 염증이 의심됩니다.",
                    List.of("결막염", "각막 손상", "알레르기"), "안과",
                    List.of("눈을 비비거나 긁지 못하게 해 주세요.", "미지근한 물에 적신 거즈로 눈곱만 살살 닦아 주세요."),
                    List.of("각막에 상처가 있는지 형광 염색 검사가 필요할까요?", "안약은 하루에 몇 번 넣어야 하나요?")),
            "피부", new Sample(HealthResult.SOON,
                    "털이 빠진 부위에 붉은 반점과 각질이 보입니다.",
                    "붉은 반점과 각질, 탈모가 함께 보여 피부염이나 곰팡이 감염이 의심됩니다.",
                    List.of("세균성 피부염", "곰팡이(진균) 감염", "알레르기성 피부염"), "피부과",
                    List.of("핥거나 긁지 못하게 넥카라를 고려해 주세요.", "병원 가기 전까지 사람용 연고는 바르지 마세요."),
                    List.of("피부 긁어서 보는 검사(소양감 원인 검사)가 필요할까요?", "사료나 간식을 바꿔야 할까요?")),
            "귀", new Sample(HealthResult.SOON,
                    "귓바퀴 안쪽이 붉고 갈색 귀지가 많이 보입니다.",
                    "갈색 귀지가 많고 귀 안쪽이 붉어 외이염이 의심됩니다.",
                    List.of("외이염", "귀 진드기", "효모균 감염"), "피부과",
                    List.of("면봉을 귀 깊숙이 넣지 마세요.", "귀를 자주 털거나 긁는 횟수를 기록해 두세요."),
                    List.of("귀지 현미경 검사가 필요할까요?", "귀 세정제는 얼마나 자주 써야 하나요?")),
            "치아·입", new Sample(HealthResult.MONITOR,
                    "어금니 쪽에 누런 치석이 붙어 있고 잇몸 경계가 약간 붉습니다.",
                    "치석과 가벼운 잇몸 염증이 보입니다. 당장 급하지는 않지만 스케일링 상담이 필요해 보입니다.",
                    List.of("치석", "치은염"), "치과",
                    List.of("반려동물 전용 칫솔·치약으로 하루 한 번 양치해 주세요.", "딱딱한 뼈 간식은 이가 깨질 수 있어 피해 주세요."),
                    List.of("마취 스케일링이 필요한 단계인가요?", "뽑아야 하는 이가 있나요?")),
            "걸음걸이", new Sample(HealthResult.SOON,
                    "걸을 때 뒷다리 한쪽에 체중을 덜 싣고 가끔 들고 걷습니다.",
                    "뒷다리 한쪽을 아껴 걷는 모습이 보여 관절이나 슬개골 문제가 의심됩니다.",
                    List.of("슬개골 탈구", "십자인대 손상", "관절염"), "정형외과",
                    List.of("계단 오르내리기와 점프를 줄여 주세요.", "미끄러운 바닥에는 매트를 깔아 주세요."),
                    List.of("엑스레이 촬영이 필요할까요?", "수술이 필요한 단계인지 알고 싶어요.")),
            "기타", new Sample(HealthResult.MONITOR,
                    "사진에서 뚜렷한 이상은 보이지 않습니다.",
                    "사진으로 보이는 뚜렷한 이상은 없습니다. 증상이 계속되면 병원에서 확인해 주세요.",
                    List.of("특이 소견 없음"), "일반",
                    List.of("식사량·배변·활동량을 며칠간 기록해 주세요."),
                    List.of("기본 혈액검사가 필요할까요?"))
    );

    @Override
    public String name() {
        return "sample";
    }

    @Override
    public HealthResult analyze(Input input) {
        Sample s = SAMPLES.getOrDefault(input.bodyPart(), SAMPLES.get("기타"));
        int frame = Math.max(1, (input.frameFiles().size() + 1) / 2);   // 가운데 프레임을 근거로 든다
        return HealthResult.builder()
                .urgency(s.urgency())
                .urgencyReason(HealthResult.MONITOR.equals(s.urgency())
                        ? "급한 신호는 보이지 않지만 상태 변화를 지켜봐야 합니다."
                        : "확인이 필요한 모습이 보여 며칠 안에 진료를 받아 보는 것이 좋습니다.")
                .findings(List.of(new Finding(frame, s.observation(), "medium")))
                .summary(s.summary())
                .possibleCauses(s.causes())
                .specialty(s.specialty())
                .homeCare(s.homeCare())
                .questionsForVet(s.questions())
                .imageQuality("OK")
                .retakeTip(null)
                .build();
    }
}
