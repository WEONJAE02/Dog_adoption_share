package org.likelion.hsu.db_project.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.HealthCheckRequestDto;
import org.likelion.hsu.db_project.Dto.HealthCheckResponseDto;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.HealthCheck;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.likelion.hsu.db_project.Repository.HealthCheckRepository;
import org.likelion.hsu.db_project.Repository.MyPetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 건강체크: 프레임 저장 → 분석 → 응급 규칙 적용 → 저장.
 * 참고용 결과이며 진단이 아니다. 응급 신호는 분석기와 상관없이 서버가 "바로 병원"으로 올린다.
 */
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    public static final List<String> BODY_PARTS = List.of("눈", "피부", "귀", "치아·입", "걸음걸이", "기타");
    public static final List<String> SYMPTOMS = List.of(
            "식욕 저하", "구토", "설사", "기침·재채기", "자꾸 긁거나 핥음", "눈곱·눈물", "절뚝거림",
            "기운 없음", "물을 많이 마심", "호흡이 가쁨", "경련·발작", "의식이 흐림", "피가 멈추지 않음");
    /** 하나라도 있으면 분석 결과와 상관없이 EMERGENCY */
    public static final List<String> EMERGENCY_SYMPTOMS = List.of("호흡이 가쁨", "경련·발작", "의식이 흐림", "피가 멈추지 않음");
    public static final List<String> SPECIALTIES = List.of("안과", "피부과", "치과", "정형외과", "내과", "신경과", "일반");
    private static final List<String> URGENCIES = List.of(HealthResult.EMERGENCY, HealthResult.SOON, HealthResult.MONITOR);

    private static final int MAX_VIDEO_FRAMES = 8;
    private static final int MAX_PHOTOS = 5;
    private static final int LIST_LIMIT = 50;

    private final HealthCheckRepository checkRepository;
    private final MyPetRepository petRepository;
    private final ImageStorage imageStorage;
    private final HealthAnalyzer analyzer;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.health.daily-limit:10}")
    private int dailyLimit;

    // ✅ 체크 등록 → 분석 결과 포함 반환
    public HealthCheckResponseDto create(HealthCheckRequestDto dto, List<MultipartFile> frames, Long userId) throws IOException {
        MyPet pet = findMyPet(dto.getPetId(), userId);
        String bodyPart = dto.getBodyPart() != null ? dto.getBodyPart() : "";
        String source = "video".equals(dto.getSource()) ? "video" : "photo";
        List<String> symptoms = validSymptoms(dto.getSymptoms());
        String memo = dto.getMemo() != null ? dto.getMemo().trim() : "";
        List<MultipartFile> files = frames == null ? List.of() : frames.stream().filter(f -> !f.isEmpty()).toList();

        if (!BODY_PARTS.contains(bodyPart)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "체크할 부위를 선택해주세요.");
        }
        if (memo.length() > 1000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "메모는 1000자 이내로 입력해주세요.");
        }
        int maxFrames = "video".equals(source) ? MAX_VIDEO_FRAMES : MAX_PHOTOS;
        if (files.isEmpty() || files.size() > maxFrames) {
            throw new BoardException(HttpStatus.BAD_REQUEST,
                    "video".equals(source) ? "영상 장면은 1~8장이어야 합니다." : "사진은 1~5장 올려주세요.");
        }
        if (!files.stream().allMatch(imageStorage::isImage)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "이미지 파일(jpg, png, gif, webp)만 올릴 수 있습니다.");
        }
        if (checkRepository.countToday(userId) >= dailyLimit) {
            throw new BoardException(HttpStatus.TOO_MANY_REQUESTS, "건강체크는 하루 " + dailyLimit + "번까지 할 수 있어요. 내일 다시 시도해주세요.");
        }

        // 분석을 시작하는 순간 횟수를 쓴다 (분석이 실패해도 AI 호출 비용은 나갈 수 있으므로)
        checkRepository.recordUsage(userId);
        List<String> stored = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                stored.add(imageStorage.store(file));
            }
            // 분석은 오래 걸릴 수 있어 DB 트랜잭션 밖에서 한다
            HealthResult result = applySafetyRules(
                    analyzer.analyze(new HealthAnalyzer.Input(pet, bodyPart, source, symptoms, memo, List.copyOf(stored))), symptoms);

            HealthCheck check = HealthCheck.builder()
                    .petId(pet.getPetId())
                    .userId(userId)
                    .bodyPart(bodyPart)
                    .source(source)
                    .symptoms(symptoms.isEmpty() ? null : String.join(",", symptoms))
                    .memo(memo.isEmpty() ? null : memo)
                    .urgency(result.getUrgency())
                    .specialty(result.getSpecialty())
                    .summary(truncate(result.getSummary(), 1000))
                    .resultJson(toJson(result))
                    .analyzer(analyzer.name())
                    .build();

            Long checkId = transactionTemplate.execute(status -> {
                Long id = checkRepository.save(check);
                for (int i = 0; i < stored.size(); i++) {
                    checkRepository.saveFrame(id, i + 1, stored.get(i));
                }
                return id;
            });
            return toDetail(checkRepository.findById(checkId).orElseThrow());
        } catch (RuntimeException | IOException e) {
            stored.forEach(imageStorage::delete);   // 저장에 실패하면 올린 프레임도 지운다
            throw e;
        }
    }

    public List<HealthCheckResponseDto> getChecks(Long userId, Long petId) {
        return checkRepository.findByUser(userId, petId, LIST_LIMIT).stream().map(this::toSummary).toList();
    }

    public HealthCheckResponseDto getCheck(Long checkId, Long userId) {
        return toDetail(findMyCheck(checkId, userId));
    }

    // ✅ 삭제 (프레임 파일 포함)
    public void deleteCheck(Long checkId, Long userId) {
        findMyCheck(checkId, userId);
        deleteWithFiles(checkId);
    }

    // ✅ 반려동물을 삭제할 때 그 반려동물의 체크도 모두 삭제
    public void deleteAllByPet(Long petId) {
        checkRepository.findIdsByPetId(petId).forEach(this::deleteWithFiles);
    }

    public int countToday(Long userId) {
        return checkRepository.countToday(userId);
    }

    // ✅ 회원 탈퇴 시 사용 기록 삭제 (체크 기록은 반려동물 삭제 때 함께 지워진다)
    public void deleteUsageByUser(Long userId) {
        checkRepository.deleteUsageByUserId(userId);
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    private void deleteWithFiles(Long checkId) {
        List<String> frames = checkRepository.findFrames(checkId);
        checkRepository.delete(checkId);
        frames.forEach(imageStorage::delete);
    }

    /**
     * 분석기 결과를 정리하고 응급 규칙을 적용한다.
     * - 응급 증상이 하나라도 있으면 무조건 EMERGENCY
     * - 형식에 맞지 않는 값은 안전한 쪽(SOON, 일반)으로 바꾼다
     */
    HealthResult applySafetyRules(HealthResult r, List<String> symptoms) {
        if (!URGENCIES.contains(r.getUrgency())) {
            r.setUrgency(HealthResult.SOON);
        }
        if (!SPECIALTIES.contains(r.getSpecialty())) {
            r.setSpecialty("일반");
        }
        List<String> emergency = symptoms.stream().filter(EMERGENCY_SYMPTOMS::contains).toList();
        if (!emergency.isEmpty()) {
            r.setUrgency(HealthResult.EMERGENCY);
            r.setUrgencyReason("응급 신호(" + String.join(", ", emergency) + ")가 있어요. 사진 결과와 상관없이 지금 바로 동물병원에 연락하세요.");
        }
        return r;
    }

    private List<String> validSymptoms(List<String> input) {
        if (input == null) {
            return List.of();
        }
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(input));
        if (!SYMPTOMS.containsAll(unique)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "증상 항목이 올바르지 않습니다.");
        }
        return unique;
    }

    private MyPet findMyPet(Long petId, Long userId) {
        if (petId == null) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "체크할 반려동물을 선택해주세요.");
        }
        return petRepository.findById(petId)
                .filter(pet -> pet.getUserId().equals(userId))
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "반려동물 정보를 찾을 수 없습니다."));
    }

    // 다른 사람의 기록은 있는지조차 알려주지 않는다
    private HealthCheck findMyCheck(Long checkId, Long userId) {
        return checkRepository.findById(checkId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "체크 기록을 찾을 수 없습니다."));
    }

    private String toJson(HealthResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("분석 결과를 저장하지 못했습니다.", e);
        }
    }

    private HealthResult fromJson(String json) {
        try {
            return objectMapper.readValue(json, HealthResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 분석 결과를 읽지 못했습니다.", e);
        }
    }

    private String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }

    private HealthCheckResponseDto toSummary(HealthCheck c) {
        return HealthCheckResponseDto.builder()
                .checkId(c.getCheckId())
                .petId(c.getPetId())
                .petName(c.getPetName())
                .petSpecies(c.getPetSpecies())
                .bodyPart(c.getBodyPart())
                .source(c.getSource())
                .symptoms(c.getSymptoms() == null ? List.of() : Arrays.asList(c.getSymptoms().split(",")))
                .memo(c.getMemo())
                .urgency(c.getUrgency())
                .specialty(c.getSpecialty())
                .summary(c.getSummary())
                .analyzer(c.getAnalyzer())
                .createdAt(c.getCreatedAt())
                .thumbnail(c.getThumbnail())
                .build();
    }

    private HealthCheckResponseDto toDetail(HealthCheck c) {
        HealthCheckResponseDto dto = toSummary(c);
        dto.setResult(fromJson(c.getResultJson()));
        dto.setFrames(checkRepository.findFrames(c.getCheckId()));
        return dto;
    }
}
