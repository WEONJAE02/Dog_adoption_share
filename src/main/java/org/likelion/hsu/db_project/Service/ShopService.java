package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.ShopItemPageDto;
import org.likelion.hsu.db_project.Dto.ShopItemRequestDto;
import org.likelion.hsu.db_project.Dto.ShopItemResponseDto;
import org.likelion.hsu.db_project.Entity.ShopItem;
import org.likelion.hsu.db_project.Repository.ShopInquiryRepository;
import org.likelion.hsu.db_project.Repository.ShopItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 반려견 용품 중고거래. 회원끼리 사고팔며, 가격 0원은 나눔으로 표시한다.
 * 동물 자체의 분양·판매는 이용약관상 금지라 카테고리에 없다.
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    public static final List<String> CATEGORIES = List.of(
            "사료", "간식", "장난감", "의류", "식기·급수기", "하우스·방석", "산책용품", "위생·미용", "기타");
    public static final List<String> STATUSES = List.of("판매중", "예약중", "거래완료");
    public static final List<String> SORTS = List.of("latest", "priceAsc", "priceDesc", "popular");

    private static final int MAX_IMAGES = 5;
    private static final int MAX_PAGE_SIZE = 40;
    private static final int MAX_PRICE = 10_000_000;

    private final ShopItemRepository itemRepository;
    private final ShopInquiryRepository inquiryRepository;
    private final ImageStorage imageStorage;

    // ✅ 등록 (여러 장 이미지 업로드)
    @Transactional
    public ShopItemResponseDto createItem(ShopItemRequestDto dto, List<MultipartFile> images, Long userId) throws IOException {
        ShopItem item = validate(dto);
        item.setUserId(userId);
        List<MultipartFile> files = validImages(images, 0);

        Long itemId = itemRepository.save(item);
        for (MultipartFile file : files) {
            itemRepository.saveImage(itemId, imageStorage.store(file));
        }

        return toDetail(findItem(itemId, userId));
    }

    // ✅ 목록
    public ShopItemPageDto getItems(String category, String search, boolean availableOnly, String filter,
                                    String sort, int page, int size, Long viewerId) {
        if (filter != null && !filter.isEmpty() && viewerId == null) {
            throw new BoardException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNo = Math.max(page, 1);
        ShopItemRepository.Filter f = new ShopItemRepository.Filter(
                category, search != null ? search.trim() : null, availableOnly, filter, viewerId);

        long total = itemRepository.count(f);
        List<ShopItemResponseDto> items = itemRepository.findPage(f, sort, pageSize, (pageNo - 1) * pageSize).stream()
                .map(this::toSummary)
                .toList();

        return ShopItemPageDto.builder()
                .items(items)
                .page(pageNo)
                .size(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();
    }

    // ✅ 상세 조회 (조회수 증가)
    public ShopItemResponseDto getItem(Long id, Long viewerId) {
        findVisibleItem(id, viewerId);
        itemRepository.increaseViewCount(id);
        return toDetail(findItem(id, viewerId));
    }

    // ✅ 수정 (keepImages에 없는 기존 이미지는 삭제, 새 이미지는 추가)
    @Transactional
    public ShopItemResponseDto updateItem(Long id, ShopItemRequestDto dto, List<MultipartFile> images, Long userId) throws IOException {
        checkOwner(findItem(id, userId).getUserId(), userId);

        ShopItem item = validate(dto);
        List<String> current = itemRepository.findImages(id);
        List<String> keep = dto.getKeepImages() != null ? dto.getKeepImages() : List.of();
        List<String> removed = current.stream().filter(name -> !keep.contains(name)).toList();
        List<MultipartFile> files = validImages(images, current.size() - removed.size());

        itemRepository.update(id, item);
        for (String fileName : removed) {
            itemRepository.deleteImage(id, fileName);
            imageStorage.delete(fileName);
        }
        for (MultipartFile file : files) {
            itemRepository.saveImage(id, imageStorage.store(file));
        }

        return toDetail(findItem(id, userId));
    }

    // ✅ 거래 상태 변경 (판매중 / 예약중 / 거래완료)
    public ShopItemResponseDto updateStatus(Long id, String status, Long userId) {
        checkOwner(findItem(id, userId).getUserId(), userId);
        if (!STATUSES.contains(status)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "거래 상태가 올바르지 않습니다.");
        }
        itemRepository.updateStatus(id, status);
        return toDetail(findItem(id, userId));
    }

    // ✅ 삭제 (문의, 관심, 이미지 포함)
    @Transactional
    public void deleteItem(Long id, Long userId) {
        checkOwner(findItem(id, userId).getUserId(), userId);

        List<String> images = itemRepository.findImages(id);
        inquiryRepository.deleteByItemId(id);
        itemRepository.deleteLikes(id);
        itemRepository.deleteImages(id);
        itemRepository.delete(id);
        images.forEach(imageStorage::delete);
    }

    // ✅ 관심 등록 / 해제
    public Map<String, Object> setLike(Long id, boolean like, Long userId) {
        ShopItem item = findVisibleItem(id, userId);
        if (item.getUserId().equals(userId)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "내가 올린 상품에는 관심을 누를 수 없습니다.");
        }
        if (like) {
            itemRepository.like(id, userId);
        } else {
            itemRepository.unlike(id, userId);
        }
        ShopItem updated = findItem(id, userId);
        return Map.of("liked", updated.isLiked(), "likeCount", updated.getLikeCount());
    }

    // 신고로 숨겨진 상품은 판매자 본인만 볼 수 있다
    ShopItem findVisibleItem(Long id, Long viewerId) {
        ShopItem item = findItem(id, viewerId);
        if (item.isHidden() && !item.getUserId().equals(viewerId)) {
            throw new BoardException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        return item;
    }

    private ShopItem findItem(Long id, Long viewerId) {
        return itemRepository.findById(id, viewerId)
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    private void checkOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("판매자만 수정·삭제할 수 있습니다.");
        }
    }

    // ✅ 입력값 검증
    private ShopItem validate(ShopItemRequestDto dto) {
        String title = trim(dto.getTitle());
        String description = trim(dto.getDescription());
        String tradeArea = trim(dto.getTradeArea());
        String category = dto.getCategory() != null ? dto.getCategory() : "";
        Integer price = dto.getPrice();

        if (!CATEGORIES.contains(category)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "카테고리를 선택해주세요.");
        }
        if (title.isEmpty() || title.length() > 50) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "상품명은 1~50자로 입력해주세요.");
        }
        if (price == null || price < 0 || price > MAX_PRICE) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "가격은 0원(나눔)부터 1,000만 원까지 입력할 수 있습니다.");
        }
        if (description.isEmpty() || description.length() > 3000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "상품 설명은 1~3000자로 입력해주세요.");
        }
        if (tradeArea.length() > 50) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "거래 희망 지역은 50자 이내로 입력해주세요.");
        }

        return ShopItem.builder()
                .category(category)
                .title(title)
                .price(price)
                .description(description)
                .tradeArea(tradeArea.isEmpty() ? null : tradeArea)
                .build();
    }

    private List<MultipartFile> validImages(List<MultipartFile> images, int existingCount) {
        List<MultipartFile> files = images == null ? List.of()
                : images.stream().filter(file -> !file.isEmpty()).toList();

        if (existingCount + files.size() > MAX_IMAGES) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "사진은 최대 " + MAX_IMAGES + "장까지 올릴 수 있습니다.");
        }
        if (!files.stream().allMatch(imageStorage::isImage)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "이미지 파일(jpg, png, gif, webp)만 올릴 수 있습니다.");
        }
        return files;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private ShopItemResponseDto toSummary(ShopItem item) {
        return ShopItemResponseDto.builder()
                .itemId(item.getItemId())
                .userId(item.getUserId())
                .nickname(item.getNickname())
                .category(item.getCategory())
                .title(item.getTitle())
                .price(item.getPrice())
                .tradeArea(item.getTradeArea())
                .status(item.getStatus())
                .viewCount(item.getViewCount())
                .likeCount(item.getLikeCount())
                .liked(item.isLiked())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .hidden(item.isHidden())
                .thumbnail(item.getThumbnail())
                .build();
    }

    private ShopItemResponseDto toDetail(ShopItem item) {
        ShopItemResponseDto dto = toSummary(item);
        dto.setDescription(item.getDescription());
        dto.setImageNames(itemRepository.findImages(item.getItemId()));
        return dto;
    }
}
