package org.likelion.hsu.db_project.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.InquiryRequestDto;
import org.likelion.hsu.db_project.Dto.ShopInquiryResponseDto;
import org.likelion.hsu.db_project.Dto.ShopItemPageDto;
import org.likelion.hsu.db_project.Dto.ShopItemRequestDto;
import org.likelion.hsu.db_project.Dto.ShopItemResponseDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.ShopInquiryService;
import org.likelion.hsu.db_project.Service.ShopService;
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
 * 반려견 용품 중고거래 (조회는 누구나, 나머지는 로그인 필요)
 */
@RestController
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopInquiryService shopInquiryService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 카테고리 / 거래 상태 목록
     */
    @GetMapping("/shop/meta")
    public Map<String, List<String>> meta() {
        return Map.of("categories", ShopService.CATEGORIES, "statuses", ShopService.STATUSES);
    }

    /**
     * ✅ 목록 (카테고리 + 검색 + 거래 가능만 + 관심/내 판매 + 정렬 + 페이지)
     * - filter: liked(관심 목록), mine(내 판매 상품) — 로그인 필요
     * - sort: latest, priceAsc, priceDesc, popular
     */
    @GetMapping("/shop/items")
    public ShopItemPageDto getItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean available,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUser user
    ) {
        return shopService.getItems(category, search, available, filter, sort, page, size, userId(user));
    }

    /**
     * ✅ 상세 조회
     */
    @GetMapping("/shop/items/{id}")
    public ShopItemResponseDto getItem(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return shopService.getItem(id, userId(user));
    }

    /**
     * ✅ 등록 (JSON + 다중 이미지 업로드)
     * - form-data: item -> JSON 문자열, images -> File (최대 5장)
     */
    @PostMapping(value = "/shop/items", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ShopItemResponseDto createItem(
            @RequestPart("item") String itemJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return shopService.createItem(parse(itemJson), images, user.getUserId());
    }

    /**
     * ✅ 수정 (판매자 본인만 가능)
     * - item.keepImages 에 남길 기존 이미지 파일명을 담는다
     */
    @PutMapping(value = "/shop/items/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ShopItemResponseDto updateItem(
            @PathVariable Long id,
            @RequestPart("item") String itemJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return shopService.updateItem(id, parse(itemJson), images, user.getUserId());
    }

    /**
     * ✅ 거래 상태 변경 (판매자 본인만 가능)
     */
    @PutMapping("/shop/items/{id}/status")
    public ShopItemResponseDto updateStatus(
            @PathVariable Long id,
            @RequestBody ShopItemRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        return shopService.updateStatus(id, dto.getStatus(), user.getUserId());
    }

    /**
     * ✅ 삭제 (판매자 본인만 가능)
     */
    @DeleteMapping("/shop/items/{id}")
    public void deleteItem(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        shopService.deleteItem(id, user.getUserId());
    }

    /**
     * ✅ 관심 등록 / 해제
     */
    @PostMapping("/shop/items/{id}/like")
    public Map<String, Object> like(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return shopService.setLike(id, true, user.getUserId());
    }

    @DeleteMapping("/shop/items/{id}/like")
    public Map<String, Object> unlike(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return shopService.setLike(id, false, user.getUserId());
    }

    /**
     * ✅ 구매 문의 보내기
     */
    @PostMapping("/shop/items/{id}/inquiries")
    public ResponseEntity<Map<String, Object>> sendInquiry(
            @PathVariable Long id,
            @RequestBody InquiryRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        shopInquiryService.send(id, dto, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "문의를 보냈습니다."));
    }

    /**
     * ✅ 받은 / 보낸 구매 문의 (문의함, /inquiries 아래라 로그인 필요)
     */
    @GetMapping("/inquiries/shop/received")
    public List<ShopInquiryResponseDto> receivedInquiries(@AuthenticationPrincipal LoginUser user) {
        return shopInquiryService.received(user.getUserId());
    }

    @GetMapping("/inquiries/shop/sent")
    public List<ShopInquiryResponseDto> sentInquiries(@AuthenticationPrincipal LoginUser user) {
        return shopInquiryService.sent(user.getUserId());
    }

    /**
     * ✅ 구매 문의 답변 (받은 사람만)
     */
    @PostMapping("/inquiries/shop/{inquiryId}/reply")
    public ResponseEntity<Map<String, Object>> reply(
            @PathVariable Long inquiryId,
            @RequestBody InquiryRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        shopInquiryService.reply(inquiryId, dto, user.getUserId());
        return ResponseEntity.ok(Map.of("message", "답변을 등록했습니다."));
    }

    private Long userId(LoginUser user) {
        return user != null ? user.getUserId() : null;
    }

    private ShopItemRequestDto parse(String itemJson) {
        try {
            return objectMapper.readValue(itemJson, ShopItemRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
        }
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
