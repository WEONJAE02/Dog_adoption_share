package org.likelion.hsu.db_project.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.MyPetRequestDto;
import org.likelion.hsu.db_project.Dto.MyPetResponseDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.MyPetService;
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
 * 내 반려동물 (모두 로그인 필요, 본인 것만)
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class MyPetController {

    private final MyPetService petService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<MyPetResponseDto> getPets(@AuthenticationPrincipal LoginUser user) {
        return petService.getPets(user.getUserId());
    }

    @GetMapping("/{id}")
    public MyPetResponseDto getPet(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        return petService.getPet(id, user.getUserId());
    }

    /**
     * ✅ 등록 (form-data: pet -> JSON 문자열, image -> File 1장, 선택)
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public MyPetResponseDto createPet(
            @RequestPart("pet") String petJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return petService.createPet(parse(petJson), image, user.getUserId());
    }

    /**
     * ✅ 수정 (새 image 를 보내면 사진 교체, pet.removeImage 면 사진 삭제)
     */
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public MyPetResponseDto updatePet(
            @PathVariable Long id,
            @RequestPart("pet") String petJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return petService.updatePet(id, parse(petJson), image, user.getUserId());
    }

    @DeleteMapping("/{id}")
    public void deletePet(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        petService.deletePet(id, user.getUserId());
    }

    private MyPetRequestDto parse(String petJson) {
        try {
            return objectMapper.readValue(petJson, MyPetRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
        }
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
