package org.likelion.hsu.db_project.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.HealthCheckRequestDto;
import org.likelion.hsu.db_project.Dto.HealthCheckResponseDto;
import org.likelion.hsu.db_project.Dto.HealthCompareDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.HealthAnalyzer;
import org.likelion.hsu.db_project.Service.HealthCheckService;
import org.likelion.hsu.db_project.Service.ImageStorage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 건강체크 (모두 로그인 필요, 본인 것만)
 */
@RestController
@RequestMapping("/health-checks")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService checkService;
    private final HealthAnalyzer analyzer;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 부위·증상 목록, 오늘 남은 횟수, 분석기 이름
     */
    @GetMapping("/meta")
    public Map<String, Object> meta(@AuthenticationPrincipal LoginUser user) {
        return Map.of(
                "bodyParts", HealthCheckService.BODY_PARTS,
                "symptoms", HealthCheckService.SYMPTOMS,
                "emergencySymptoms", HealthCheckService.EMERGENCY_SYMPTOMS,
                "dailyLimit", checkService.dailyLimit(),
                "usedToday", checkService.countToday(user.getUserId()),
                "analyzer", analyzer.name());
    }

    @GetMapping
    public List<HealthCheckResponseDto> getChecks(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String bodyPart,
            @AuthenticationPrincipal LoginUser user
    ) {
        return checkService.getChecks(user.getUserId(), petId, bodyPart);
    }

    /**
     * ✅ 장면 사진 (본인만, n 은 1부터). 비공개 폴더에 있어 /uploads 로는 열리지 않는다
     */
    @GetMapping("/{id}/frames/{seq}")
    public ResponseEntity<Resource> getFrame(@PathVariable Long id, @PathVariable int seq, @AuthenticationPrincipal LoginUser user) {
        Path file = checkService.getFrame(id, seq, user.getUserId());
        return ResponseEntity.ok()
                .contentType(ImageStorage.mediaType(file.getFileName().toString()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(file));
    }

    /**
     * ✅ 같은 반려동물·같은 부위의 지난 체크와 비교
     */
    @GetMapping("/{id}/compare")
    public HealthCompareDto compare(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return checkService.compare(id, user.getUserId());
    }

    @GetMapping("/{id}")
    public HealthCheckResponseDto getCheck(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return checkService.getCheck(id, user.getUserId());
    }

    /**
     * ✅ 체크 (form-data: check -> JSON 문자열, frames -> 이미지 여러 장)
     * - 영상은 브라우저에서 장면을 뽑아 이미지로 보낸다 (최대 8장), 사진은 최대 5장
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public HealthCheckResponseDto create(
            @RequestPart("check") String checkJson,
            @RequestPart(value = "frames", required = false) List<MultipartFile> frames,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return checkService.create(parse(checkJson), frames, user.getUserId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        checkService.deleteCheck(id, user.getUserId());
    }

    private HealthCheckRequestDto parse(String json) {
        try {
            return objectMapper.readValue(json, HealthCheckRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
        }
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
