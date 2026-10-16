package org.likelion.hsu.db_project.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Dto.HealthCheckRequestDto;
import org.likelion.hsu.db_project.Dto.HealthCheckResponseDto;
import org.likelion.hsu.db_project.Dto.HealthCompareDto;
import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.HealthCheck;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.likelion.hsu.db_project.Repository.HealthCheckRepository;
import org.likelion.hsu.db_project.Repository.MyPetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HealthCheckServiceTest {

    private static final long OWNER = 1L;
    private static final long OTHER = 2L;
    private static final long PET_ID = 10L;

    private HealthCheckRepository checkRepository;
    private MyPetRepository petRepository;
    private ImageStorage imageStorage;
    private HealthAnalyzer analyzer;
    private TransactionTemplate transactionTemplate;
    private HealthCheckService service;

    @BeforeEach
    void setUp() throws Exception {
        checkRepository = mock(HealthCheckRepository.class);
        petRepository = mock(MyPetRepository.class);
        imageStorage = mock(ImageStorage.class);
        analyzer = mock(HealthAnalyzer.class);
        transactionTemplate = mock(TransactionTemplate.class);
        service = new HealthCheckService(checkRepository, petRepository, imageStorage, analyzer, new ObjectMapper(), transactionTemplate);
        ReflectionTestUtils.setField(service, "dailyLimit", 10);

        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(MyPet.builder().petId(PET_ID).userId(OWNER).name("콩이").species("개").build()));
        when(imageStorage.isImage(any())).thenReturn(true);
        when(imageStorage.store(any())).thenReturn("f1.jpg", "f2.jpg", "f3.jpg");
        when(analyzer.name()).thenReturn("test");
        when(analyzer.analyze(any())).thenAnswer(inv -> result(HealthResult.MONITOR, "안과"));
        // 트랜잭션 없이 콜백만 실행
        when(transactionTemplate.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    // ---------- 응급 규칙 ----------

    @Test
    void 응급_증상이_있으면_분석_결과와_상관없이_바로_병원이다() {
        HealthResult r = service.applySafetyRules(result(HealthResult.MONITOR, "치과"), List.of("구토", "경련·발작"));

        assertEquals(HealthResult.EMERGENCY, r.getUrgency());
        assertTrue(r.getUrgencyReason().contains("경련·발작"));
        assertEquals("치과", r.getSpecialty());
    }

    @Test
    void 응급_증상이_없으면_분석_결과를_그대로_둔다() {
        HealthResult r = service.applySafetyRules(result(HealthResult.MONITOR, "피부과"), List.of("자꾸 긁거나 핥음"));

        assertEquals(HealthResult.MONITOR, r.getUrgency());
        assertEquals("원래 이유", r.getUrgencyReason());
    }

    @Test
    void 형식에_맞지_않는_긴급도와_진료_분야는_안전한_값으로_바꾼다() {
        HealthResult r = service.applySafetyRules(result("괜찮아요", "한의원"), List.of());

        assertEquals(HealthResult.SOON, r.getUrgency());
        assertEquals("일반", r.getSpecialty());
    }

    @Test
    void 응급_증상을_고르면_저장되는_결과도_바로_병원이다() throws Exception {
        HealthCheckRequestDto dto = request("치아·입", "photo", List.of("호흡이 가쁨"));
        stubSavedCheck();

        service.create(dto, frames(1), OWNER);

        verify(checkRepository).save(argThat(c -> HealthResult.EMERGENCY.equals(c.getUrgency())));
    }

    // ---------- 하루 횟수 제한 ----------

    @Test
    void 하루_제한을_넘으면_분석하지_않고_거부한다() throws Exception {
        when(checkRepository.countToday(OWNER)).thenReturn(10);

        BoardException e = assertThrows(BoardException.class, () -> service.create(request("눈", "photo", List.of()), frames(1), OWNER));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        verify(analyzer, never()).analyze(any());
        verify(checkRepository, never()).recordUsage(any());
        verify(imageStorage, never()).store(any());
    }

    @Test
    void 분석을_시작하면_사용_기록을_남긴다() throws Exception {
        stubSavedCheck();

        HealthCheckResponseDto r = service.create(request("눈", "video", List.of("눈곱·눈물")), frames(3), OWNER);

        verify(checkRepository, times(1)).recordUsage(OWNER);
        verify(checkRepository).saveFrame(1L, 1, "f1.jpg");
        verify(checkRepository).saveFrame(1L, 3, "f3.jpg");
        assertEquals("안과", r.getResult().getSpecialty());
    }

    @Test
    void 기록을_지워도_사용_기록은_지우지_않는다() {
        when(checkRepository.findById(5L)).thenReturn(Optional.of(savedCheck(5L, OWNER)));
        when(checkRepository.findFrames(5L)).thenReturn(List.of("f1.jpg"));

        service.deleteCheck(5L, OWNER);

        verify(checkRepository).delete(5L);
        verify(imageStorage).delete("f1.jpg");
        verify(checkRepository, never()).deleteUsageByUserId(any());
    }

    // ---------- 권한 ----------

    @Test
    void 다른_사람의_반려동물로는_체크할_수_없다() {
        BoardException e = assertThrows(BoardException.class, () -> service.create(request("눈", "photo", List.of()), frames(1), OTHER));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        verify(checkRepository, never()).recordUsage(any());
    }

    @Test
    void 다른_사람의_기록은_보거나_지울_수_없다() {
        when(checkRepository.findById(5L)).thenReturn(Optional.of(savedCheck(5L, OWNER)));

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BoardException.class, () -> service.getCheck(5L, OTHER)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BoardException.class, () -> service.deleteCheck(5L, OTHER)).getStatus());
        verify(checkRepository, never()).delete(any());
    }

    // ---------- 입력 검증 ----------

    @Test
    void 목록에_없는_부위나_증상은_거부한다() {
        assertThrows(BoardException.class, () -> service.create(request("꼬리", "photo", List.of()), frames(1), OWNER));
        assertThrows(BoardException.class, () -> service.create(request("눈", "photo", List.of("해킹")), frames(1), OWNER));
    }

    @Test
    void 사진은_5장_영상_장면은_8장까지다() {
        assertThrows(BoardException.class, () -> service.create(request("눈", "photo", List.of()), frames(6), OWNER));
        assertThrows(BoardException.class, () -> service.create(request("눈", "video", List.of()), frames(9), OWNER));
        assertThrows(BoardException.class, () -> service.create(request("눈", "photo", List.of()), frames(0), OWNER));
    }

    // ---------- 실패 처리 ----------

    @Test
    void 저장에_실패하면_올린_장면_파일을_지운다() {
        doThrow(new IllegalStateException("DB 오류")).when(transactionTemplate).execute(any());

        assertThrows(IllegalStateException.class, () -> service.create(request("눈", "video", List.of()), frames(2), OWNER));

        verify(imageStorage).delete("f1.jpg");
        verify(imageStorage).delete("f2.jpg");
    }

    // ---------- 지난 체크와 비교 ----------

    @Test
    void 긴급도가_낮아지면_나아졌다고_본다() {
        assertEquals(HealthCompareDto.BETTER, HealthCheckService.change(HealthResult.EMERGENCY, HealthResult.SOON));
        assertEquals(HealthCompareDto.BETTER, HealthCheckService.change(HealthResult.SOON, HealthResult.MONITOR));
        assertEquals(HealthCompareDto.SAME, HealthCheckService.change(HealthResult.SOON, HealthResult.SOON));
        assertEquals(HealthCompareDto.WORSE, HealthCheckService.change(HealthResult.MONITOR, HealthResult.EMERGENCY));
    }

    @Test
    void 같은_반려동물_같은_부위의_바로_이전_체크와_비교한다() {
        HealthCheck now = savedCheck(7L, OWNER, HealthResult.MONITOR, LocalDateTime.of(2026, 10, 16, 9, 0));
        HealthCheck before = savedCheck(3L, OWNER, HealthResult.SOON, LocalDateTime.of(2026, 10, 12, 21, 0));
        when(checkRepository.findById(7L)).thenReturn(Optional.of(now));
        when(checkRepository.findPrevious(PET_ID, "눈", 7L)).thenReturn(Optional.of(before));
        when(checkRepository.countUpTo(PET_ID, "눈", 7L)).thenReturn(2);

        HealthCompareDto r = service.compare(7L, OWNER);

        assertEquals(HealthCompareDto.BETTER, r.getChange());
        assertEquals(3L, r.getPrevious().getCheckId());
        assertEquals(7L, r.getCurrent().getCheckId());
        assertEquals(2, r.getSequence());
        assertEquals(4, r.getDaysBetween());   // 날짜 기준 (12일 밤 → 16일 아침)
    }

    @Test
    void 지난_체크가_없으면_첫_체크다() {
        when(checkRepository.findById(7L)).thenReturn(Optional.of(savedCheck(7L, OWNER)));
        when(checkRepository.findPrevious(any(), any(), any())).thenReturn(Optional.empty());
        when(checkRepository.countUpTo(any(), any(), any())).thenReturn(1);

        HealthCompareDto r = service.compare(7L, OWNER);

        assertEquals(HealthCompareDto.FIRST, r.getChange());
        assertNull(r.getPrevious());
        assertEquals(0, r.getDaysBetween());
    }

    @Test
    void 다른_사람의_기록은_비교할_수_없다() {
        when(checkRepository.findById(7L)).thenReturn(Optional.of(savedCheck(7L, OWNER)));

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BoardException.class, () -> service.compare(7L, OTHER)).getStatus());
        verify(checkRepository, never()).findPrevious(any(), any(), any());
    }

    @Test
    void 목록에_없는_부위로는_기록을_거를_수_없다() {
        assertThrows(BoardException.class, () -> service.getChecks(OWNER, null, "꼬리"));
        service.getChecks(OWNER, PET_ID, "");
        verify(checkRepository).findByUser(OWNER, PET_ID, null, 50);
    }

    // ---------- helpers ----------

    private static HealthResult result(String urgency, String specialty) {
        return HealthResult.builder()
                .urgency(urgency)
                .urgencyReason("원래 이유")
                .specialty(specialty)
                .summary("요약")
                .findings(List.of(new HealthResult.Finding(1, "관찰", "medium")))
                .build();
    }

    private static HealthCheckRequestDto request(String bodyPart, String source, List<String> symptoms) {
        HealthCheckRequestDto dto = new HealthCheckRequestDto();
        dto.setPetId(PET_ID);
        dto.setBodyPart(bodyPart);
        dto.setSource(source);
        dto.setSymptoms(symptoms);
        return dto;
    }

    private static List<MultipartFile> frames(int count) {
        List<MultipartFile> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new MockMultipartFile("frames", "frame-" + i + ".jpg", "image/jpeg", new byte[]{1, 2, 3}));
        }
        return list;
    }

    private HealthCheck savedCheck(long checkId, long userId) {
        return savedCheck(checkId, userId, HealthResult.MONITOR, LocalDateTime.now());
    }

    private HealthCheck savedCheck(long checkId, long userId, String urgency, LocalDateTime createdAt) {
        return HealthCheck.builder()
                .checkId(checkId).petId(PET_ID).userId(userId).petName("콩이").petSpecies("개")
                .bodyPart("눈").source("photo").urgency(urgency).specialty("안과")
                .resultJson("{\"urgency\":\"" + urgency + "\",\"specialty\":\"안과\",\"findings\":[]}")
                .analyzer("test").createdAt(createdAt)
                .build();
    }

    private void stubSavedCheck() {
        when(checkRepository.save(any())).thenReturn(1L);
        when(checkRepository.findById(1L)).thenReturn(Optional.of(savedCheck(1L, OWNER)));
        when(checkRepository.findFrames(1L)).thenReturn(List.of("f1.jpg"));
    }
}
