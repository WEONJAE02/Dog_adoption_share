package org.likelion.hsu.db_project.Service;

import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.MyPet;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleHealthAnalyzerTest {

    private final SampleHealthAnalyzer analyzer = new SampleHealthAnalyzer();
    private final MyPet pet = MyPet.builder().petId(1L).userId(1L).name("콩이").species("개").build();

    @Test
    void 모든_부위에_대해_정해진_형식의_결과를_돌려준다() {
        List<String> frames = List.of("a.jpg", "b.jpg", "c.jpg");
        for (String part : HealthCheckService.BODY_PARTS) {
            HealthResult r = analyzer.analyze(new HealthAnalyzer.Input(pet, part, "video", List.of(), "", frames));

            assertTrue(List.of(HealthResult.EMERGENCY, HealthResult.SOON, HealthResult.MONITOR).contains(r.getUrgency()), part);
            assertTrue(HealthCheckService.SPECIALTIES.contains(r.getSpecialty()), part);
            assertFalse(r.getSummary().isBlank(), part);
            assertFalse(r.getHomeCare().isEmpty(), part);
            // 근거 장면 번호는 실제로 올린 장면 안에 있어야 한다
            r.getFindings().forEach(f -> assertTrue(f.getFrame() >= 1 && f.getFrame() <= frames.size(), part));
        }
    }

    @Test
    void 장면이_한_장이어도_근거_장면은_1번이다() {
        HealthResult r = analyzer.analyze(new HealthAnalyzer.Input(pet, "눈", "photo", List.of(), "", List.of("a.jpg")));

        assertEquals(1, r.getFindings().get(0).getFrame());
    }
}
