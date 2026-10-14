package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.*;
import org.likelion.hsu.db_project.Entity.Comment;
import org.likelion.hsu.db_project.Entity.Post;
import org.likelion.hsu.db_project.Repository.CommentRepository;
import org.likelion.hsu.db_project.Repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    public static final List<String> CATEGORIES = List.of("자유", "정보공유", "입양후기", "질문", "실종·목격");

    private static final int MAX_IMAGES = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ImageStorage imageStorage;

    // ✅ 등록 (여러 장 이미지 업로드)
    @Transactional
    public PostResponseDto createPost(PostRequestDto dto, List<MultipartFile> images, Long userId) throws IOException {
        Post post = validate(dto);
        post.setUserId(userId);
        List<MultipartFile> files = validImages(images, 0);

        Long postId = postRepository.save(post);
        for (MultipartFile file : files) {
            postRepository.saveImage(postId, imageStorage.store(file));
        }

        return toDetail(findPost(postId));
    }

    // ✅ 목록 (말머리 + 검색 + 페이지)
    public PostPageDto getPosts(String category, String search, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNo = Math.max(page, 1);
        String keyword = search != null ? search.trim() : null;

        long total = postRepository.count(category, keyword);
        List<PostResponseDto> items = postRepository
                .findPage(category, keyword, pageSize, (pageNo - 1) * pageSize).stream()
                .map(this::toSummary)
                .toList();

        return PostPageDto.builder()
                .items(items)
                .page(pageNo)
                .size(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();
    }

    // ✅ 상세 조회 (조회수 증가)
    public PostResponseDto getPost(Long id) {
        findPost(id);
        postRepository.increaseViewCount(id);
        return toDetail(findPost(id));
    }

    // ✅ 수정 (keepImages에 없는 기존 이미지는 삭제, 새 이미지는 추가)
    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto dto, List<MultipartFile> images, Long userId) throws IOException {
        Post existing = findPost(id);
        checkOwner(existing.getUserId(), userId);

        Post post = validate(dto);
        List<String> current = postRepository.findImages(id);
        List<String> keep = dto.getKeepImages() != null ? dto.getKeepImages() : List.of();
        List<String> removed = current.stream().filter(name -> !keep.contains(name)).toList();
        List<MultipartFile> files = validImages(images, current.size() - removed.size());

        postRepository.update(id, post);
        for (String fileName : removed) {
            postRepository.deleteImage(id, fileName);
            imageStorage.delete(fileName);
        }
        for (MultipartFile file : files) {
            postRepository.saveImage(id, imageStorage.store(file));
        }

        return toDetail(findPost(id));
    }

    // ✅ 삭제 (댓글, 이미지 포함)
    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = findPost(id);
        checkOwner(post.getUserId(), userId);

        List<String> images = postRepository.findImages(id);
        commentRepository.deleteByPostId(id);
        postRepository.deleteImages(id);
        postRepository.delete(id);
        images.forEach(imageStorage::delete);
    }

    // ✅ 댓글 / 대댓글 등록
    @Transactional
    public PostResponseDto addComment(Long postId, CommentRequestDto dto, Long userId) {
        findPost(postId);

        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (content.isEmpty() || content.length() > 500) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "댓글은 1~500자로 입력해주세요.");
        }

        Long parentId = null;
        if (dto.getParentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentId())
                    .filter(c -> c.getPostId().equals(postId))
                    .orElseThrow(() -> new BoardException(HttpStatus.BAD_REQUEST, "답글을 달 댓글을 찾을 수 없습니다."));
            // 대댓글에 단 답글은 같은 원댓글 아래로 모은다 (2단계 유지)
            parentId = parent.getParentId() != null ? parent.getParentId() : parent.getCommentId();
        }

        commentRepository.save(Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentId(parentId)
                .content(content)
                .build());

        return toDetail(findPost(postId));
    }

    // ✅ 댓글 삭제 (답글이 달린 댓글은 "삭제된 댓글"로 남긴다)
    @Transactional
    public PostResponseDto deleteComment(Long postId, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> c.getPostId().equals(postId) && !c.isDeleted())
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        checkOwner(comment.getUserId(), userId);

        if (commentRepository.countReplies(commentId) > 0) {
            commentRepository.markDeleted(commentId);
        } else {
            commentRepository.delete(commentId);
            // 마지막 답글이 지워지면, 이미 삭제 처리된 원댓글도 함께 정리
            if (comment.getParentId() != null) {
                commentRepository.findById(comment.getParentId())
                        .filter(parent -> parent.isDeleted() && commentRepository.countReplies(parent.getCommentId()) == 0)
                        .ifPresent(parent -> commentRepository.delete(parent.getCommentId()));
            }
        }

        return toDetail(findPost(postId));
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private void checkOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("작성자만 수정·삭제할 수 있습니다.");
        }
    }

    // ✅ 입력값 검증
    private Post validate(PostRequestDto dto) {
        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        String category = dto.getCategory() != null && !dto.getCategory().isEmpty() ? dto.getCategory() : CATEGORIES.get(0);

        if (title.isEmpty() || title.length() > 100) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "제목은 1~100자로 입력해주세요.");
        }
        if (content.isEmpty() || content.length() > 5000) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "내용은 1~5000자로 입력해주세요.");
        }
        if (!CATEGORIES.contains(category)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "말머리가 올바르지 않습니다.");
        }

        return Post.builder().title(title).content(content).category(category).build();
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

    private PostResponseDto toSummary(Post post) {
        return PostResponseDto.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .nickname(post.getNickname())
                .category(post.getCategory())
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .thumbnail(post.getThumbnail())
                .build();
    }

    private PostResponseDto toDetail(Post post) {
        PostResponseDto dto = toSummary(post);
        dto.setContent(post.getContent());
        dto.setImageNames(postRepository.findImages(post.getPostId()));
        dto.setComments(toCommentTree(commentRepository.findByPostId(post.getPostId())));
        return dto;
    }

    // ✅ 댓글 목록 → 댓글 + 답글 구조
    private List<CommentResponseDto> toCommentTree(List<Comment> comments) {
        Map<Long, CommentResponseDto> roots = new LinkedHashMap<>();
        for (Comment comment : comments) {
            CommentResponseDto dto = CommentResponseDto.builder()
                    .commentId(comment.getCommentId())
                    .userId(comment.isDeleted() ? null : comment.getUserId())
                    .nickname(comment.isDeleted() ? null : comment.getNickname())
                    .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                    .deleted(comment.isDeleted())
                    .createdAt(comment.getCreatedAt())
                    .build();

            CommentResponseDto parent = comment.getParentId() != null ? roots.get(comment.getParentId()) : null;
            if (parent != null) {
                parent.getReplies().add(dto);
            } else {
                roots.put(comment.getCommentId(), dto);
            }
        }
        return new ArrayList<>(roots.values());
    }
}
