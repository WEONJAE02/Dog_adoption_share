package org.likelion.hsu.db_project.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.DogRequestDto;
import org.likelion.hsu.db_project.Dto.DogResponseDto;
import org.likelion.hsu.db_project.Service.DogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ObjectMapper objectMapper; // 스프링이 제공하는 ObjectMapper 주입

    /** 등록 (form-data: dog JSON + image 파일) */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DogResponseDto> createDog(
            @RequestPart("dog") String dogJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) throws IOException {
        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        DogResponseDto res = dogService.createDog(dto, imageFile);
        return ResponseEntity.ok(res);
    }

    /** 목록 조회 (검색/필터/정렬) */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DogResponseDto>> getAllDogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        return ResponseEntity.ok(dogService.getAllDogs(search, status, sort));
    }

    /** 상세 */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DogResponseDto> getDogById(@PathVariable Long id) {
        return ResponseEntity.ok(dogService.getDogById(id));
    }

    /** 수정 (form-data: dog JSON + image 파일) */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DogResponseDto> updateDog(
            @PathVariable Long id,
            @RequestPart("dog") String dogJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) throws IOException {
        DogRequestDto dto = objectMapper.readValue(dogJson, DogRequestDto.class);
        DogResponseDto res = dogService.updateDog(id, dto, imageFile);
        return ResponseEntity.ok(res);
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDog(@PathVariable Long id) {
        dogService.deleteDog(id);
        return ResponseEntity.noContent().build(); // 204
    }

    /** 통계 */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(dogService.getStatistics());
    }
}
