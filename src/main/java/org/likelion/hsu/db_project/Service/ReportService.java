package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.ReportRequestDto;
import org.likelion.hsu.db_project.Repository.ReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 신고: 회원 등록 유기견, 게시글, 댓글, 용품 거래 상품
 * 서로 다른 회원 N명(기본 3명)이 신고하면 자동으로 숨긴다. 숨긴 글은 작성자 본인에게만 보인다.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    public static final List<String> TARGET_TYPES = List.of("dog", "post", "comment", "item");
    public static final List<String> REASONS = List.of(
            "유료 분양·판매 의심", "허위 정보", "동물 학대 의심", "욕설·비방·개인정보 노출", "광고·스팸", "기타");

    private final ReportRepository reportRepository;

    @Value("${app.report.hide-threshold:3}")
    private int hideThreshold;

    @Transactional
    public Map<String, Object> report(ReportRequestDto dto, Long reporterId) {
        String type = dto.getTargetType();
        if (!TARGET_TYPES.contains(type) || dto.getTargetId() == null) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "신고 대상이 올바르지 않습니다.");
        }
        if (!REASONS.contains(dto.getReason())) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "신고 사유를 선택해주세요.");
        }
        String detail = dto.getDetail() != null ? dto.getDetail().trim() : "";
        if (detail.length() > 500) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "자세한 내용은 500자 이내로 입력해주세요.");
        }

        Long owner = reportRepository.findOwner(type, dto.getTargetId())
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "신고할 대상을 찾을 수 없습니다."));
        if (owner.equals(reporterId)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "내가 쓴 글은 신고할 수 없습니다.");
        }

        try {
            reportRepository.save(type, dto.getTargetId(), reporterId, dto.getReason(), detail.isEmpty() ? null : detail);
        } catch (DuplicateKeyException e) {
            throw new BoardException(HttpStatus.CONFLICT, "이미 신고한 대상입니다.");
        }

        boolean hidden = reportRepository.countReporters(type, dto.getTargetId()) >= hideThreshold;
        if (hidden) {
            reportRepository.hide(type, dto.getTargetId());
        }
        return Map.of("message", "신고가 접수되었습니다.", "hidden", hidden);
    }
}
