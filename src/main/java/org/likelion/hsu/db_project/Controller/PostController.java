package org.likelion.hsu.db_project.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.CommentRequestDto;
import org.likelion.hsu.db_project.Dto.PostPageDto;
import org.likelion.hsu.db_project.Dto.PostRequestDto;
import org.likelion.hsu.db_project.Dto.PostResponseDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.BoardException;
import org.likelion.hsu.db_project.Service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 목록 (말머리 + 검색 + 페이지)
     */
    @GetMapping
    public PostPageDto getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        return postService.getPosts(category, search, page, size);
    }

    /**
     * ✅ 말머리 목록
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return PostService.CATEGORIES;
    }

    /**
     * ✅ 상세 조회 (댓글 포함)
     */
    @GetMapping("/{id}")
    public PostResponseDto getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    /**
     * ✅ 등록 (JSON + 다중 이미지 업로드)
     * - form-data: post -> JSON 문자열, images -> File (최대 5장)
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public PostResponseDto createPost(
            @RequestPart("post") String postJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return postService.createPost(parse(postJson), images, user.getUserId());
    }

    /**
     * ✅ 수정 (작성자 본인만 가능)
     * - post.keepImages 에 남길 기존 이미지 파일명을 담는다
     */
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public PostResponseDto updatePost(
            @PathVariable Long id,
            @RequestPart("post") String postJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal LoginUser user
    ) throws IOException {
        return postService.updatePost(id, parse(postJson), images, user.getUserId());
    }

    /**
     * ✅ 삭제 (작성자 본인만 가능)
     */
    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        postService.deletePost(id, user.getUserId());
    }

    /**
     * ✅ 댓글 / 대댓글 등록 → 갱신된 게시글 반환
     */
    @PostMapping("/{id}/comments")
    public PostResponseDto addComment(
            @PathVariable Long id,
            @RequestBody CommentRequestDto dto,
            @AuthenticationPrincipal LoginUser user
    ) {
        return postService.addComment(id, dto, user.getUserId());
    }

    /**
     * ✅ 댓글 삭제 (작성자 본인만 가능) → 갱신된 게시글 반환
     */
    @DeleteMapping("/{id}/comments/{commentId}")
    public PostResponseDto deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return postService.deleteComment(id, commentId, user.getUserId());
    }

    private PostRequestDto parse(String postJson) {
        try {
            return objectMapper.readValue(postJson, PostRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
        }
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<Map<String, Object>> handleBoardException(BoardException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }
}
