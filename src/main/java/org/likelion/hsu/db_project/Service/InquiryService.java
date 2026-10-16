package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.InquiryRequestDto;
import org.likelion.hsu.db_project.Dto.InquiryResponseDto;
import org.likelion.hsu.db_project.Entity.Dog;
import org.likelion.hsu.db_project.Entity.Inquiry;
import org.likelion.hsu.db_project.Repository.DogRepository;
import org.likelion.hsu.db_project.Repository.InquiryRepository;
import org.likelion.hsu.db_project.Repository.ShopInquiryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final DogRepository dogRepository;
    private final ShopInquiryRepository shopInquiryRepository;

    // ✅ 입양 문의 보내기 (회원 등록 유기견 → 등록자)
    public void send(Long dogId, InquiryRequestDto dto, Long senderId) {
        Dog dog = dogRepository.findById(dogId)
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "유기견 정보를 찾을 수 없습니다."));
        if (dog.getUserId() == null) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "등록자 정보가 없는 글이라 문의를 보낼 수 없습니다.");
        }
        if (dog.getUserId().equals(senderId)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "내가 등록한 유기견에는 문의할 수 없습니다.");
        }

        String message = trim(dto.getMessage());
        String contact = trim(dto.getContact());
        if (message.isEmpty() || message.length() > 1000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "문의 내용은 1~1000자로 입력해주세요.");
        }
        if (contact.length() > 100) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "연락처는 100자 이내로 입력해주세요.");
        }
        if (inquiryRepository.existsPending(dogId, senderId)) {
            throw new BoardException(HttpStatus.CONFLICT, "이 아이에 대해 답변을 기다리는 문의가 이미 있습니다.");
        }

        inquiryRepository.save(Inquiry.builder()
                .dogId(dogId)
                .senderId(senderId)
                .receiverId(dog.getUserId())
                .message(message)
                .contact(contact.isEmpty() ? null : contact)
                .build());
    }

    // ✅ 받은 문의 (등록자) — 문의한 사람이 남긴 연락처 포함
    public List<InquiryResponseDto> received(Long userId) {
        return inquiryRepository.findByReceiverId(userId).stream().map(this::toDto).toList();
    }

    // ✅ 보낸 문의
    public List<InquiryResponseDto> sent(Long userId) {
        return inquiryRepository.findBySenderId(userId).stream().map(this::toDto).toList();
    }

    // 답변 대기 수 (문의함 배지: 입양 문의 + 용품 구매 문의)
    public Map<String, Integer> summary(Long userId) {
        int adoption = inquiryRepository.countPendingReceived(userId);
        int shop = shopInquiryRepository.countPendingReceived(userId);
        return Map.of("pendingReceived", adoption + shop, "pendingAdoption", adoption, "pendingShop", shop);
    }

    // ✅ 답변 (받은 사람만, 다시 보내면 답변을 고친다)
    public void reply(Long inquiryId, InquiryRequestDto dto, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
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

    private InquiryResponseDto toDto(Inquiry q) {
        return InquiryResponseDto.builder()
                .inquiryId(q.getInquiryId())
                .dogId(q.getDogId())
                .dogName(q.getDogName())
                .dogImage(q.getDogImage())
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
