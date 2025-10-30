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
     * ✅ 등록 (JSON + 이미지 업로드)
     * - Postman에서 form-data로 "dog" JSON과 "image" 파일을 함께 전송
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public DogResponseDto createDog(@RequestPart("dog") String dogJson,
                                    @RequestPart(value = "image", required = false) MultipartFile imageFile)
            throws IOException {

        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        return dogService.createDog(dto, imageFile);
    }

    /**
     * ✅ 전체 조회 (검색 + 정렬 + 필터 지원)
     * - /dogs?search=비숑&status=보호중&sort=age
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
     * ✅ 수정
     */
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public DogResponseDto updateDog(@PathVariable Long id,
                                    @RequestPart("dog") String dogJson,
                                    @RequestPart(value = "image", required = false) MultipartFile imageFile)
            throws IOException {

        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        return dogService.updateDog(id, dto, imageFile);
    }

    /**
     * ✅ 삭제
     */
    @DeleteMapping("/{id}")
    public void deleteDog(@PathVariable Long id) {
        dogService.deleteDog(id);
    }

    /**
     * ✅ 통계 대시보드 API
     * - 상태별 / 품종별 유기견 현황 통계 제공
     * - 예시 응답:
     *   {
     *     "statusStats": [ {"status":"보호중","count":12}, {"status":"입양대기","count":7}, {"status":"입양완료","count":5} ],
     *     "breedStats": [ {"breed":"푸들","count":8}, {"breed":"말티즈","count":6}, {"breed":"믹스","count":7} ]
     *   }
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return dogService.getStatistics();
    }
}
