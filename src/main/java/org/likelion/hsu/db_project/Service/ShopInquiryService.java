package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.InquiryRequestDto;
import org.likelion.hsu.db_project.Dto.ShopInquiryResponseDto;
import org.likelion.hsu.db_project.Entity.ShopInquiry;
import org.likelion.hsu.db_project.Entity.ShopItem;
import org.likelion.hsu.db_project.Repository.ShopInquiryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 용품 구매 문의 (보낸 사람 → 판매자). 입양 문의와 같은 방식으로 문의함에서 주고받는다.
 */
@Service
@RequiredArgsConstructor
public class ShopInquiryService {

    private final ShopInquiryRepository inquiryRepository;
    private final ShopService shopService;

    // ✅ 구매 문의 보내기
    public void send(Long itemId, InquiryRequestDto dto, Long senderId) {
        ShopItem item = shopService.findVisibleItem(itemId, senderId);
        if (item.getUserId().equals(senderId)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "내가 올린 상품에는 문의할 수 없습니다.");
        }
        if ("거래완료".equals(item.getStatus())) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "거래가 끝난 상품입니다.");
        }

        String message = trim(dto.getMessage());
        String contact = trim(dto.getContact());
        if (message.isEmpty() || message.length() > 1000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "문의 내용은 1~1000자로 입력해주세요.");
        }
        if (contact.length() > 100) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "연락처는 100자 이내로 입력해주세요.");
        }
        if (inquiryRepository.existsPending(itemId, senderId)) {
            throw new BoardException(HttpStatus.CONFLICT, "이 상품에 답변을 기다리는 문의가 이미 있습니다.");
        }

        inquiryRepository.save(ShopInquiry.builder()
                .itemId(itemId)
                .senderId(senderId)
                .receiverId(item.getUserId())
                .message(message)
                .contact(contact.isEmpty() ? null : contact)
                .build());
    }

    // ✅ 받은 문의 (판매자) — 문의한 사람이 남긴 연락처 포함
    public List<ShopInquiryResponseDto> received(Long userId) {
        return inquiryRepository.findByReceiverId(userId).stream().map(this::toDto).toList();
    }

    // ✅ 보낸 문의
    public List<ShopInquiryResponseDto> sent(Long userId) {
        return inquiryRepository.findBySenderId(userId).stream().map(this::toDto).toList();
    }

    public int countPendingReceived(Long userId) {
        return inquiryRepository.countPendingReceived(userId);
    }

    // ✅ 답변 (받은 사람만, 다시 보내면 답변을 고친다)
    public void reply(Long inquiryId, InquiryRequestDto dto, Long userId) {
        ShopInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."));
        if (!inquiry.getReceiverId().equals(userId)) {
            throw new AccessDeniedException("받은 문의에만 답변할 수 있습니다.");
        }
        String reply = trim(dto.getReply());
        if (reply.isEmpty() || reply.length() > 1000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "답변은 1~1000자로 입력해주세요.");
        }
        inquiryRepository.reply(inquiryId, reply);
    }

    private ShopInquiryResponseDto toDto(ShopInquiry q) {
        return ShopInquiryResponseDto.builder()
                .inquiryId(q.getInquiryId())
                .itemId(q.getItemId())
                .itemTitle(q.getItemTitle())
                .itemImage(q.getItemImage())
                .itemPrice(q.getItemPrice())
                .senderNickname(q.getSenderNickname())
                .receiverNickname(q.getReceiverNickname())
                .message(q.getMessage())
                .contact(q.getContact())
                .reply(q.getReply())
                .createdAt(q.getCreatedAt())
                .repliedAt(q.getRepliedAt())
                .build();
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
