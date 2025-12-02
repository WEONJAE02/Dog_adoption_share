package org.likelion.hsu.db_major.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_major.Dto.DogRequestDto;
import org.likelion.hsu.db_major.Dto.DogResponseDto;
import org.likelion.hsu.db_major.Service.DogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dogs")
@RequiredArgsConstructor
public class DogController {

    private final DogService dogService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * ✅ 등록 (JSON + 다중 이미지 업로드)
     * - Postman: form-data
     *   dog -> JSON 문자열
     *   images -> File (여러 장 가능)
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public DogResponseDto createDog(
            @RequestPart("dog") String dogJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        return dogService.createDog(dto, images);
    }


    /**
     * ✅ 전체 조회 (검색 + 정렬 + 필터 지원)
     */
    @GetMapping
    public List<DogResponseDto> getAllDogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        return dogService.getAllDogs(search, status, sort);
    }


    /**
     * ✅ 상세 조회
     */
    @GetMapping("/{id}")
    public DogResponseDto getDogById(@PathVariable Long id) {
        return dogService.getDogById(id);
    }


    /**
     * ✅ 수정 (JSON + 다중 이미지 업로드)
     * - 기존 이미지 삭제 후 새 이미지 저장하는 방식은 Service에서 처리
     */
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public DogResponseDto updateDog(
            @PathVariable Long id,
            @RequestPart("dog") String dogJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        return dogService.updateDog(id, dto, images);
    }


    /**
     * ✅ 삭제
     */
    @DeleteMapping("/{id}")
    public void deleteDog(@PathVariable Long id) {
        dogService.deleteDog(id);
    }


    /**
     * ✅ 통계 데이터
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return dogService.getStatistics();
    }
}
