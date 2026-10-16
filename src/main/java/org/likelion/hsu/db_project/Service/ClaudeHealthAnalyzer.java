package org.likelion.hsu.db_project.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Claude로 장면 사진을 보고 건강체크 결과를 만든다.
 * 결과 형식은 구조화 출력으로 강제하고, 응급 규칙·값 보정은 HealthCheckService 가 한 번 더 적용한다.
 */
@Slf4j
public class ClaudeHealthAnalyzer implements HealthAnalyzer {

    static final String SYSTEM_PROMPT = """
            너는 반려동물 보호자가 동물병원에 가기 전에 상태를 가늠하도록 돕는 보조자다. 진단을 내리지 않고, 병원에 얼마나 서둘러 가야 하는지와 어떤 진료 분야가 맞는지를 판단한다.

            판단 규칙
            - 사진에서 실제로 보이는 것만 근거로 쓴다. 보호자가 적은 증상·메모는 보호자 설명으로 참고하되, 사진에서 보인 것처럼 쓰지 않는다.
            - 병명을 단정하지 않는다. possibleCauses 는 의심되는 후보 1~3개만 쓴다.
            - urgency
              - EMERGENCY: 지금 바로 병원. 호흡 곤란, 경련, 의식 저하, 멈추지 않는 출혈, 눈알이 튀어나옴, 배가 부풀고 헛구역질, 뒷다리를 전혀 못 씀 등
              - SOON: 2~3일 안에 진료가 필요함
              - MONITOR: 집에서 지켜봐도 됨
              - 둘 중 애매하면 더 급한 쪽을 고른다.
            - specialty 는 안과, 피부과, 치과, 정형외과, 내과, 신경과, 일반 중 하나만 쓴다.
            - 사진이 흐리거나, 걱정되는 부위가 보이지 않거나, 동물이 없는 사진이면 imageQuality 를 POOR 로 하고 retakeTip 에 다시 찍는 방법을 쓴다. 이때 urgency 는 MONITOR 로 하지 않는다. 사진에 문제가 없으면 imageQuality 는 OK, retakeTip 은 빈 문자열이다.
            - findings 의 frame 은 사진 바로 앞에 적힌 "장면 N"의 N 이다. 근거가 된 장면만 1~3개 쓴다.

            쓰는 방식
            - 보호자가 읽는 한국어 존댓말(~요)로, 짧고 쉬운 문장으로 쓴다. 의학 용어는 괄호로 풀어 쓴다.
            - homeCare: 병원 가기 전까지 집에서 할 수 있는 안전한 조치 2~4개. 약을 먹이거나 바르라는 지시는 하지 않는다.
            - questionsForVet: 진료 때 수의사에게 물어볼 질문 2~3개.
            """;

    /** AI가 돌려주는 형식 (구조화 출력 스키마) */
    record AiFinding(
            @JsonPropertyDescription("근거가 된 장면 번호 (사진 앞의 '장면 N')") int frame,
            @JsonPropertyDescription("그 장면에서 보이는 것") String observation,
            @JsonPropertyDescription("low, medium, high 중 하나") String confidence) {}

    record AiHealthResult(
            @JsonPropertyDescription("EMERGENCY, SOON, MONITOR 중 하나") String urgency,
            @JsonPropertyDescription("그 긴급도를 고른 이유 한두 문장") String urgencyReason,
            List<AiFinding> findings,
            @JsonPropertyDescription("보이는 것 요약 한두 문장") String summary,
            List<String> possibleCauses,
            @JsonPropertyDescription("안과, 피부과, 치과, 정형외과, 내과, 신경과, 일반 중 하나") String specialty,
            List<String> homeCare,
            List<String> questionsForVet,
            @JsonPropertyDescription("OK 또는 POOR") String imageQuality,
            @JsonPropertyDescription("POOR 일 때 다시 찍는 방법, 아니면 빈 문자열") String retakeTip) {}

    private final AnthropicClient client;
    private final String model;
    private final ImageStorage imageStorage;

    public ClaudeHealthAnalyzer(AnthropicClient client, String model, ImageStorage imageStorage) {
        this.client = client;
        this.model = model;
        this.imageStorage = imageStorage;
    }

    @Override
    public String name() {
        return model;
    }

    @Override
    public HealthResult analyze(Input input) {
        StructuredMessageCreateParams<AiHealthResult> params = buildParams(input);
        StructuredMessage<AiHealthResult> message;
        try {
            message = client.messages().create(params);
        } catch (RateLimitException e) {
            throw new BoardException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 요청이 많아요. 잠시 후 다시 시도해주세요.");
        } catch (UnauthorizedException e) {
            log.error("[HealthAI] API 키가 올바르지 않습니다.");
            throw new BoardException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석을 지금 사용할 수 없어요. 관리자에게 문의해주세요.");
        } catch (AnthropicServiceException e) {
            log.warn("[HealthAI] API 오류 status={} type={}", e.statusCode(), e.errorType().orElse(null));
            throw new BoardException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 중 문제가 생겼어요. 잠시 후 다시 시도해주세요.");
        } catch (AnthropicException e) {
            log.warn("[HealthAI] 연결 오류: {}", e.getMessage());
            throw new BoardException(HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서버에 연결하지 못했어요. 잠시 후 다시 시도해주세요.");
        }

        log.info("[HealthAI] model={} frames={} input_tokens={} output_tokens={} stop={}",
                message.model().asString(), input.frameFiles().size(),
                message.usage().inputTokens(), message.usage().outputTokens(),
                message.stopReason().map(StopReason::asString).orElse(null));

        Optional<StopReason> stop = message.stopReason();
        if (stop.isPresent() && StopReason.REFUSAL.equals(stop.get())) {
            throw new BoardException(HttpStatus.UNPROCESSABLE_ENTITY, "이 사진은 분석할 수 없어요. 반려동물이 잘 보이는 다른 사진으로 시도해주세요.");
        }
        if (stop.isPresent() && StopReason.MAX_TOKENS.equals(stop.get())) {
            throw new BoardException(HttpStatus.BAD_GATEWAY, "분석 결과가 너무 길어져 중단됐어요. 다시 시도해주세요.");
        }

        AiHealthResult ai = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new BoardException(HttpStatus.BAD_GATEWAY, "AI가 결과를 돌려주지 않았어요. 다시 시도해주세요."));
        return toHealthResult(ai, input.frameFiles().size());
    }

    /** 요청 만들기: "장면 N" 라벨 + 사진을 번갈아 넣고, 마지막에 보호자 설명을 붙인다 */
    StructuredMessageCreateParams<AiHealthResult> buildParams(Input input) {
        List<ContentBlockParam> content = new ArrayList<>();
        try {
            List<String> frames = input.frameFiles();
            for (int i = 0; i < frames.size(); i++) {
                content.add(text("장면 " + (i + 1)));
                content.add(image(frames.get(i)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("장면 사진을 읽지 못했습니다.", e);
        }
        content.add(text(describe(input)));

        // effort 는 이 모델의 기본값(medium)을 그대로 쓴다: 사진 판독 + 짧은 요약이라 충분하다
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                // 안전 분류기가 거절하면 서버가 다른 모델로 다시 시도한다
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessageOfBlockParams(content)
                .outputConfig(AiHealthResult.class)
                .build();
    }

    /** 보호자가 입력한 정보 (사진 뒤에 붙는 설명) */
    static String describe(Input input) {
        MyPet pet = input.pet();
        StringBuilder sb = new StringBuilder("보호자 설명\n");
        sb.append("- 동물: ").append(pet.getSpecies());
        if (pet.getBreed() != null) sb.append(" (").append(pet.getBreed()).append(")");
        sb.append("\n");
        if (pet.getBirthYear() != null) sb.append("- 나이: 약 ").append(Math.max(0, Year.now().getValue() - pet.getBirthYear())).append("살\n");
        if (pet.getGender() != null) sb.append("- 성별: ").append(pet.getGender()).append("\n");
        if (pet.getNeutered() != null) sb.append("- 중성화: ").append(pet.getNeutered() ? "했음" : "안 함").append("\n");
        if (pet.getWeightKg() != null) sb.append("- 몸무게: ").append(pet.getWeightKg().stripTrailingZeros().toPlainString()).append("kg\n");
        if (pet.getNotes() != null) sb.append("- 평소 특이사항: ").append(pet.getNotes()).append("\n");
        sb.append("- 걱정되는 부위: ").append(input.bodyPart()).append("\n");
        sb.append("- 사진 종류: ").append("video".equals(input.source()) ? "영상에서 고르게 뽑은 장면 (번호 순서가 시간 순서)" : "보호자가 찍은 사진").append("\n");
        sb.append("- 보호자가 고른 증상: ").append(input.symptoms().isEmpty() ? "없음" : String.join(", ", input.symptoms())).append("\n");
        if (input.memo() != null && !input.memo().isBlank()) sb.append("- 보호자 메모: ").append(input.memo()).append("\n");
        sb.append("\n위 사진과 설명을 보고 정해진 형식으로 답해줘.");
        return sb.toString();
    }

    /** AI 결과 → 저장 형식. 장면 번호가 범위를 벗어난 근거는 버린다 */
    static HealthResult toHealthResult(AiHealthResult ai, int frameCount) {
        List<HealthResult.Finding> findings = ai.findings() == null ? List.of() : ai.findings().stream()
                .filter(f -> f.frame() >= 1 && f.frame() <= frameCount)
                .map(f -> new HealthResult.Finding(f.frame(), f.observation(),
                        f.confidence() == null ? "low" : f.confidence().toLowerCase(Locale.ROOT)))
                .toList();
        String retake = ai.retakeTip() == null || ai.retakeTip().isBlank() ? null : ai.retakeTip();
        return HealthResult.builder()
                .urgency(ai.urgency() == null ? null : ai.urgency().trim().toUpperCase(Locale.ROOT))
                .urgencyReason(ai.urgencyReason())
                .findings(findings)
                .summary(ai.summary())
                .possibleCauses(nonNull(ai.possibleCauses()))
                .specialty(ai.specialty() == null ? null : ai.specialty().trim())
                .homeCare(nonNull(ai.homeCare()))
                .questionsForVet(nonNull(ai.questionsForVet()))
                .imageQuality("POOR".equalsIgnoreCase(ai.imageQuality()) ? "POOR" : "OK")
                .retakeTip(retake)
                .build();
    }

    private static List<String> nonNull(List<String> list) {
        return list == null ? List.of() : list;
    }

    private static ContentBlockParam text(String value) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(value).build());
    }

    private ContentBlockParam image(String fileName) throws IOException {
        String data = Base64.getEncoder().encodeToString(imageStorage.read(fileName));
        return ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder().data(data).mediaType(mediaType(fileName)).build())
                .build());
    }

    static Base64ImageSource.MediaType mediaType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }
}
