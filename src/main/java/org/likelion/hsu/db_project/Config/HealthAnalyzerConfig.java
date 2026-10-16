package org.likelion.hsu.db_project.Config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.likelion.hsu.db_project.Service.ClaudeHealthAnalyzer;
import org.likelion.hsu.db_project.Service.HealthAnalyzer;
import org.likelion.hsu.db_project.Service.ImageStorage;
import org.likelion.hsu.db_project.Service.SampleHealthAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * 건강체크 분석기 선택: ANTHROPIC_API_KEY 가 있으면 Claude, 없으면 샘플 분석기.
 */
@Slf4j
@Configuration
public class HealthAnalyzerConfig {

    @Bean
    @Primary
    public HealthAnalyzer healthAnalyzer(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${app.health.ai-model:claude-opus-5-5}") String model,
            SampleHealthAnalyzer sampleAnalyzer,
            ImageStorage imageStorage
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[HealthAI] ANTHROPIC_API_KEY 가 없어 샘플 분석기를 사용합니다.");
            return sampleAnalyzer;
        }
        log.info("[HealthAI] Claude 분석기를 사용합니다. model={}", model);
        return new ClaudeHealthAnalyzer(
                AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(120))
                        .maxRetries(1)
                        .build(),
                model, imageStorage);
    }
}
