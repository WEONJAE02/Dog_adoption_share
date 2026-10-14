package org.likelion.hsu.db_project.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드 이미지 저장소.
 * /uploads/** 로 그대로 서빙되므로 이미지 확장자만 허용한다. (html, svg 등 차단)
 */
@Slf4j
@Component
public class ImageStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${file.upload-dir}")
    private String uploadDir;

    public boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/")
                && ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()));
    }

    // ✅ 저장 후 생성된 파일명 반환
    public String store(MultipartFile file) throws IOException {
        if (!isImage(file)) {
            throw new IllegalArgumentException("이미지 파일(jpg, png, gif, webp)만 올릴 수 있습니다.");
        }
        String fileName = UUID.randomUUID() + "." + extension(file.getOriginalFilename());

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    // ✅ 삭제 (파일이 없거나 실패해도 요청은 계속 진행)
    public void delete(String fileName) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path target = uploadPath.resolve(fileName).normalize();
            if (target.startsWith(uploadPath)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("[Upload] 파일 삭제 실패: {}", fileName, e);
        }
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
