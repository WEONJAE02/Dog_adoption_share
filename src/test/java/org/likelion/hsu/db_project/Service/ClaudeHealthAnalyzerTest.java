package org.likelion.hsu.db_project.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.Base64ImageSource;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Config.HealthAnalyzerConfig;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.likelion.hsu.db_project.Service.ClaudeHealthAnalyzer.AiFinding;
import org.likelion.hsu.db_project.Service.ClaudeHealthAnalyzer.AiHealthResult;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ClaudeHealthAnalyzerTest {

    private final MyPet pet = MyPet.builder().petId(1L).userId(1L).name("콩이").species("개").breed("말티즈")
            .gender("암컷").neutered(true).birthYear(Year.now().getValue() - 7).weightKg(new BigDecimal("3.40"))
            .notes("슬개골 탈구 1기").build();

    private HealthAnalyzer.Input input(List<String> symptoms, List<String> frames) {
        return new HealthAnalyzer.Input(pet, "걸음걸이", "video", symptoms, "산책 후 절뚝여요", frames);
    }

    // ---------- 요청 만들기 (API를 부르지 않고 확인) ----------

    @Test
    void 장면_라벨과_사진을_번갈아_넣고_마지막에_보호자_설명을_붙인다() throws Exception {
        ImageStorage storage = mock(ImageStorage.class);
        when(storage.readPrivate(anyString())).thenReturn(new byte[]{1, 2, 3});
        ClaudeHealthAnalyzer analyzer = new ClaudeHealthAnalyzer(mock(AnthropicClient.class), "claude-opus-5-5", storage);

        JsonNode body = json(analyzer.buildParams(input(List.of("절뚝거림"), List.of("a.jpg", "b.png"))).rawParams()._body());

        JsonNode blocks = body.at("/messages/0/content");
        assertEquals(5, blocks.size());
        assertEquals("장면 1", blocks.get(0).get("text").asText());
        assertEquals("image/jpeg", blocks.get(1).at("/source/media_type").asText());
        assertEquals("AQID", blocks.get(1).at("/source/data").asText());   // {1,2,3} 의 base64
        assertEquals("장면 2", blocks.get(2).get("text").asText());
        assertEquals("image/png", blocks.get(3).at("/source/media_type").asText());
        assertTrue(blocks.get(4).get("text").asText().contains("절뚝거림"));
        assertEquals("claude-opus-5-5", body.get("model").asText());
    }

    @Test
    void 결과_형식은_구조화_출력_스키마로_강제한다() throws Exception {
        ImageStorage storage = mock(ImageStorage.class);
        when(storage.readPrivate(anyString())).thenReturn(new byte[]{1});
        ClaudeHealthAnalyzer analyzer = new ClaudeHealthAnalyzer(mock(AnthropicClient.class), "claude-opus-5-5", storage);

        var params = analyzer.buildParams(input(List.of(), List.of("a.jpg"))).rawParams();
        JsonNode schema = json(params._body()).at("/output_config/format/schema");

        assertEquals("json_schema", json(params._body()).at("/output_config/format/type").asText());
        for (String field : List.of("urgency", "urgencyReason", "findings", "summary", "possibleCauses",
                "specialty", "homeCare", "questionsForVet", "imageQuality", "retakeTip")) {
            assertTrue(schema.at("/properties").has(field), field);
        }
        assertEquals("default", params._additionalBodyProperties().get("fallbacks").asString().orElseThrow());
        assertTrue(params._headers().values("anthropic-beta").contains("server-side-fallback-2026-07-01"));
    }

    @Test
    void 보호자_설명에_반려동물_정보와_증상을_담는다() {
        String text = ClaudeHealthAnalyzer.describe(input(List.of(), List.of("a.jpg")));

        assertTrue(text.contains("개 (말티즈)"));
        assertTrue(text.contains("약 7살"));
        assertTrue(text.contains("3.4kg"));
        assertTrue(text.contains("슬개골 탈구 1기"));
        assertTrue(text.contains("보호자가 고른 증상: 없음"));
        assertTrue(text.contains("산책 후 절뚝여요"));
        assertTrue(text.contains("시간 순서"));
    }

    // ---------- AI 결과 → 저장 형식 ----------

    @Test
    void 범위를_벗어난_장면_번호는_버리고_값을_정리한다() {
        AiHealthResult ai = new AiHealthResult("soon", "이유",
                List.of(new AiFinding(2, "보임", "HIGH"), new AiFinding(9, "없는 장면", "low"), new AiFinding(0, "0번", "low")),
                "요약", List.of("슬개골 탈구"), " 정형외과 ", List.of("쉬게 해 주세요"), List.of("엑스레이?"), "ok", "  ");

        HealthResult r = ClaudeHealthAnalyzer.toHealthResult(ai, 3);

        assertEquals("SOON", r.getUrgency());
        assertEquals(1, r.getFindings().size());
        assertEquals(2, r.getFindings().get(0).getFrame());
        assertEquals("high", r.getFindings().get(0).getConfidence());
        assertEquals("정형외과", r.getSpecialty());
        assertEquals("OK", r.getImageQuality());
        assertNull(r.getRetakeTip());
    }

    @Test
    void 사진이_나쁘면_다시_찍는_방법을_남긴다() {
        AiHealthResult ai = new AiHealthResult("SOON", "이유", null, "흐려요", null, "일반", null, null, "poor", "밝은 곳에서 다시 찍어 주세요");

        HealthResult r = ClaudeHealthAnalyzer.toHealthResult(ai, 2);

        assertEquals("POOR", r.getImageQuality());
        assertEquals("밝은 곳에서 다시 찍어 주세요", r.getRetakeTip());
        assertTrue(r.getFindings().isEmpty());
        assertTrue(r.getHomeCare().isEmpty());
    }

    @Test
    void 확장자로_이미지_형식을_고른다() {
        assertEquals(Base64ImageSource.MediaType.IMAGE_JPEG, ClaudeHealthAnalyzer.mediaType("x.JPG"));
        assertEquals(Base64ImageSource.MediaType.IMAGE_PNG, ClaudeHealthAnalyzer.mediaType("x.png"));
        assertEquals(Base64ImageSource.MediaType.IMAGE_WEBP, ClaudeHealthAnalyzer.mediaType("x.webp"));
        assertEquals(Base64ImageSource.MediaType.IMAGE_GIF, ClaudeHealthAnalyzer.mediaType("x.gif"));
    }

    // ---------- 분석기 선택 ----------

    @Test
    void API_키가_없으면_샘플_분석기를_쓴다() {
        SampleHealthAnalyzer sample = new SampleHealthAnalyzer();

        assertSame(sample, new HealthAnalyzerConfig().healthAnalyzer("", "claude-opus-5-5", sample, mock(ImageStorage.class)));
        assertSame(sample, new HealthAnalyzerConfig().healthAnalyzer("  ", "claude-opus-5-5", sample, mock(ImageStorage.class)));
    }

    @Test
    void API_키가_있으면_Claude_분석기를_쓴다() {
        HealthAnalyzer analyzer = new HealthAnalyzerConfig().healthAnalyzer("sk-ant-test", "claude-opus-5-5", new SampleHealthAnalyzer(), mock(ImageStorage.class));

        assertInstanceOf(ClaudeHealthAnalyzer.class, analyzer);
        assertEquals("claude-opus-5-5", analyzer.name());
    }

    private static JsonNode json(Object value) throws Exception {
        return ObjectMappers.jsonMapper().valueToTree(value);
    }
}
