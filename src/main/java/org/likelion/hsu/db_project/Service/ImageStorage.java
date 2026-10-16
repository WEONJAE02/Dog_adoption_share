package org.likelion.hsu.db_project.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드 이미지 저장소.
 * - 공개 폴더(file.upload-dir): /uploads/** 로 그대로 서빙된다. 유기견·게시글·Shop 사진
 * - 비공개 폴더(file.private-dir): 서빙되지 않는다. 내 반려동물 사진, 건강체크 장면
 *   → 본인 확인을 거친 API(/pets/{id}/image, /health-checks/{id}/frames/{n})로만 내려준다
 * 이미지 확장자만 허용한다. (html, svg 등 차단)
 */
@Slf4j
@Component
public class ImageStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.private-dir:private-uploads}")
    private String privateDir;

    public boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/")
                && ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()));
    }

    // ✅ 공개 폴더: 저장 후 생성된 파일명 반환
    public String store(MultipartFile file) throws IOException {
        return store(file, publicRoot());
    }

    // ✅ 공개 폴더: 삭제 (파일이 없거나 실패해도 요청은 계속 진행)
    public void delete(String fileName) {
        delete(fileName, publicRoot());
    }

    // ✅ 비공개 폴더
    public String storePrivate(MultipartFile file) throws IOException {
        return store(file, privateRoot());
    }

    public byte[] readPrivate(String fileName) throws IOException {
        return Files.readAllBytes(resolve(privateRoot(), fileName)
                .orElseThrow(() -> new IOException("잘못된 파일 경로입니다.")));
    }

    public void deletePrivate(String fileName) {
        delete(fileName, privateRoot());
    }

    /** 비공개 파일 경로 (없거나 폴더 밖이면 empty) */
    public Optional<Path> privateFile(String fileName) {
        return resolve(privateRoot(), fileName).filter(Files::isRegularFile);
    }

    /**
     * 공개 폴더에 있는 파일을 비공개 폴더로 옮긴다 (비공개 폴더 도입 전에 올린 사진용).
     * 옮겼으면 true
     */
    public boolean moveToPrivate(String fileName) throws IOException {
        Optional<Path> from = resolve(publicRoot(), fileName).filter(Files::isRegularFile);
        Optional<Path> to = resolve(privateRoot(), fileName);
        if (from.isEmpty() || to.isEmpty() || Files.exists(to.get())) {
            return false;
        }
        Files.createDirectories(privateRoot());
        Files.move(from.get(), to.get());
        return true;
    }

    public static MediaType mediaType(String fileName) {
        return switch (extension(fileName)) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
    }

    private String store(MultipartFile file, Path root) throws IOException {
        if (!isImage(file)) {
            throw new IllegalArgumentException("이미지 파일(jpg, png, gif, webp)만 올릴 수 있습니다.");
        }
        String fileName = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
        Files.createDirectories(root);
        Files.copy(file.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void delete(String fileName, Path root) {
        try {
            Optional<Path> target = resolve(root, fileName);
            if (target.isPresent()) {
                Files.deleteIfExists(target.get());
            }
        } catch (IOException e) {
            log.warn("[Upload] 파일 삭제 실패: {}", fileName, e);
        }
    }

    // 폴더 밖을 가리키는 경로(../ 등)는 거부한다
    private static Optional<Path> resolve(Path root, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        Path target = root.resolve(fileName).normalize();
        return target.startsWith(root) && !target.equals(root) ? Optional.of(target) : Optional.empty();
    }

    private Path publicRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path privateRoot() {
        return Paths.get(privateDir).toAbsolutePath().normalize();
    }

    private static String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
