package org.likelion.hsu.db_project.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        // Windows에서도 안전한 URI 포맷 (file:///C:/... 형태)
        String location = uploadPath.toUri().toString(); // 예: file:///C:/Users/user/Desktop/upload/

        log.info("[Static] /uploads/** -> {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
