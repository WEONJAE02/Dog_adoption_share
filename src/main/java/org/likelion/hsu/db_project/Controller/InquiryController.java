package org.likelion.hsu.db_project.Controller;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.InquiryRequestDto;
import org.likelion.hsu.db_project.Dto.InquiryResponseDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 회원 등록 유기견 입양 문의 (모두 로그인 필요)
 */
@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * ✅ 입양 문의 보내기
     */
    @PostMapping("/dogs/{dogId}/inquiries")
    public ResponseEntity<Map<String, Object>> send(
            @PathVariable Long dogId,
            @RequestBody InquiryRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        inquiryService.send(dogId, dto, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "문의를 보냈습니다."));
    }

    /**
     * ✅ 받은 문의 / 보낸 문의 / 답변 대기 수
     */
    @GetMapping("/inquiries/received")
    public List<InquiryResponseDto> received(@AuthenticationPrincipal LoginUser user) {
        return inquiryService.received(user.getUserId());
    }

    @GetMapping("/inquiries/sent")
    public List<InquiryResponseDto> sent(@AuthenticationPrincipal LoginUser user) {
        return inquiryService.sent(user.getUserId());
    }

    @GetMapping("/inquiries/summary")
    public Map<String, Integer> summary(@AuthenticationPrincipal LoginUser user) {
        return inquiryService.summary(user.getUserId());
    }

    /**
     * ✅ 답변 (받은 사람만)
     */
    @PostMapping("/inquiries/{id}/reply")
    public ResponseEntity<Map<String, Object>> reply(
            @PathVariable Long id,
            @RequestBody InquiryRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        inquiryService.reply(id, dto, user.getUserId());
        return ResponseEntity.ok(Map.of("message", "답변을 등록했습니다."));
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
