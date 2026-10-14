package org.likelion.hsu.db_project.Controller;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.ShelterPageDto;
import org.likelion.hsu.db_project.Dto.ShelterStatsDetailDto;
import org.likelion.hsu.db_project.Dto.ShelterStatsDto;
import org.likelion.hsu.db_project.Service.ShelterException;
import org.likelion.hsu.db_project.Service.ShelterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 전국 보호소 유기견 (공공데이터포털 국가동물보호정보시스템)
 */
@RestController
@RequestMapping("/shelter")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    /**
     * ✅ 시도 목록
     */
    @GetMapping("/regions")
    public List<Map<String, String>> getRegions() {
        return shelterService.getRegions();
    }

    /**
     * ✅ 보호소 유기견 목록
     * - region: 시도코드 (빈값이면 전국), state: notice(공고중) / protect(보호중) / 빈값(전체)
     */
    @GetMapping("/animals")
    public ShelterPageDto getAnimals(
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "protect") String state,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "8") int size
    ) {
        return shelterService.getAnimals(region, state, page, size);
    }

    /**
     * ✅ 최근 30일 구조된 유기견의 처리 현황 (구조 수, 입양률, 안락사율)
     */
    @GetMapping("/stats")
    public ShelterStatsDto getStats() {
        return shelterService.getStats();
    }

    /**
     * ✅ 상세 통계
     * - days: 7 / 30 / 90, kind: dog / cat / all
     */
    @GetMapping("/stats/detail")
    public ShelterStatsDetailDto getStatsDetail(
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(required = false, defaultValue = "dog") String kind
    ) {
        return shelterService.getStatsDetail(days, kind);
    }

    @ExceptionHandler(ShelterException.class)
    public ResponseEntity<Map<String, Object>> handleShelterException(ShelterException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
    }
}
