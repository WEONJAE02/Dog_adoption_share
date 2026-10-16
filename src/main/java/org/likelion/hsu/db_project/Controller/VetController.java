package org.likelion.hsu.db_project.Controller;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.VetRecommendationDto;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.VetRecommendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 주치의 추천 (샘플 수의사, 조회는 누구나)
 */
@RestController
@RequestMapping("/vets")
@RequiredArgsConstructor
public class VetController {

    private final VetRecommendService recommendService;

    /**
     * ✅ 진료 분야 + 내 지역 + 긴급도로 추천 (기본 3명)
     */
    @GetMapping("/recommend")
    public List<VetRecommendationDto> recommend(
            @RequestParam String specialty,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) Integer limit
    ) {
        return recommendService.recommend(specialty, sido, sigungu, urgency, limit);
    }

    /**
     * ✅ 수의사가 있는 지역 (시도 → 시군구)
     */
    @GetMapping("/regions")
    public Map<String, List<String>> regions() {
        return recommendService.regions();
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
