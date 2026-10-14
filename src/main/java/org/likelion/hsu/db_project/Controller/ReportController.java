package org.likelion.hsu.db_project.Controller;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.ReportRequestDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * ✅ 신고 사유 목록
     */
    @GetMapping("/reasons")
    public List<String> reasons() {
        return ReportService.REASONS;
    }

    /**
     * ✅ 신고 (로그인 필요)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> report(@RequestBody ReportRequestDto dto, @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.report(dto, user.getUserId()));
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
