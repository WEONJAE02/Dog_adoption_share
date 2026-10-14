package org.likelion.hsu.db_project.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.likelion.hsu.db_project.Dto.ShelterAnimalDto;
import org.likelion.hsu.db_project.Dto.ShelterPageDto;
import org.likelion.hsu.db_project.Dto.ShelterStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공공데이터포털 "국가동물보호정보시스템 구조동물 조회 서비스" 연동.
 * 인증키가 브라우저에 노출되지 않도록 서버가 대신 호출하고, 호출 수를 줄이기 위해 결과를 메모리에 캐시한다.
 */
@Slf4j
@Service
public class ShelterService {

    private static final String DOG = "417000";                 // 축종코드: 개
    private static final int STATS_DAYS = 30;
    private static final int STATS_PAGE_SIZE = 1000;            // API 최대값
    private static final int STATS_MAX_PAGES = 10;
    private static final int MAX_PAGE_SIZE = 40;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern AGE = Pattern.compile("^(\\d{4})(?:\\(([^)]*)\\))?\\(년생\\)$");

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${animal.api.key:}")
    private String apiKey;

    @Value("${animal.api.base-url}")
    private String baseUrl;

    public ShelterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // ✅ 시도 목록
    public List<Map<String, String>> getRegions() {
        return cached("regions", Duration.ofHours(24), () -> {
            List<Map<String, String>> regions = new ArrayList<>();
            for (JsonNode item : items(call("/sido_v2", Map.of("numOfRows", "100")))) {
                regions.add(Map.of("code", item.path("orgCd").asText(), "name", item.path("orgdownNm").asText()));
            }
            return regions;
        });
    }

    // ✅ 보호소 유기견 목록 (시도 + 상태 + 페이지)
    public ShelterPageDto getAnimals(String region, String state, int page, int size) {
        String regionCode = region != null && region.matches("\\d{7}") ? region : "";
        String stateCode = "notice".equals(state) || "protect".equals(state) ? state : "";
        int pageNo = Math.min(Math.max(page, 1), 1000);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String cacheKey = String.join("|", "animals", regionCode, stateCode, String.valueOf(pageNo), String.valueOf(pageSize));
        return cached(cacheKey, Duration.ofMinutes(10), () -> {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("upkind", DOG);
            params.put("upr_cd", regionCode);
            params.put("state", stateCode);
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(pageSize));

            JsonNode body = call("/abandonmentPublic_v2", params);
            List<ShelterAnimalDto> animals = new ArrayList<>();
            for (JsonNode item : items(body)) {
                animals.add(toAnimal(item));
            }
            return ShelterPageDto.builder()
                    .items(animals)
                    .page(pageNo)
                    .size(pageSize)
                    .total(body.path("totalCount").asLong())
                    .build();
        });
    }

    // ✅ 최근 30일 구조된 유기견의 처리 현황
    public synchronized ShelterStatsDto getStats() {
        return cached("stats", Duration.ofHours(1), () -> {
            LocalDate to = LocalDate.now(SEOUL);
            LocalDate from = to.minusDays(STATS_DAYS);
            long rescued = 0, adopted = 0, returned = 0, euthanized = 0, protecting = 0;

            for (int page = 1; page <= STATS_MAX_PAGES; page++) {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("upkind", DOG);
                params.put("bgnde", from.format(YMD));
                params.put("endde", to.format(YMD));
                params.put("pageNo", String.valueOf(page));
                params.put("numOfRows", String.valueOf(STATS_PAGE_SIZE));

                JsonNode body = call("/abandonmentPublic_v2", params);
                List<JsonNode> items = items(body);
                for (JsonNode item : items) {
                    String state = item.path("processState").asText();
                    rescued++;
                    if (state.contains("입양")) adopted++;
                    else if (state.contains("반환")) returned++;
                    else if (state.contains("안락사")) euthanized++;
                    else if (!state.startsWith("종료")) protecting++;
                }
                if (items.isEmpty() || (long) page * STATS_PAGE_SIZE >= body.path("totalCount").asLong()) break;
            }

            // 아직 보호 중인 동물이 많아, 비율은 처리가 끝난 건을 기준으로 계산한다
            long closed = rescued - protecting;

            return ShelterStatsDto.builder()
                    .from(from.toString())
                    .to(to.toString())
                    .days(STATS_DAYS)
                    .rescued(rescued)
                    .adopted(adopted)
                    .returned(returned)
                    .euthanized(euthanized)
                    .protecting(protecting)
                    .closed(closed)
                    .adoptionRate(percent(adopted, closed))
                    .euthanasiaRate(percent(euthanized, closed))
                    .build();
        });
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    // ✅ API 호출 → response.body 반환
    private JsonNode call(String path, Map<String, String> params) {
        if (apiKey.isBlank()) {
            throw new ShelterException("공공데이터 API 키가 설정되지 않았습니다.");
        }

        // 인증키의 +, /, = 가 그대로 전달되도록 직접 인코딩한다
        StringBuilder url = new StringBuilder(baseUrl).append(path)
                .append("?serviceKey=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                .append("&_type=json");
        params.forEach((name, value) -> {
            if (value != null && !value.isEmpty()) {
                url.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });

        try {
            byte[] bytes = restClient.get().uri(URI.create(url.toString())).retrieve().body(byte[].class);
            JsonNode response = objectMapper.readTree(new String(bytes != null ? bytes : new byte[0], StandardCharsets.UTF_8)).path("response");
            String resultCode = response.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.warn("[Shelter] {} 응답 오류: code={} msg={}", path, resultCode, response.path("header").path("resultMsg").asText());
                throw new ShelterException("공공데이터 서버가 오류를 응답했습니다.");
            }
            return response.path("body");
        } catch (ShelterException e) {
            throw e;
        } catch (Exception e) {
            // 예외 메시지에는 인증키가 포함된 URL이 들어 있을 수 있어 종류만 남긴다
            log.warn("[Shelter] {} 호출 실패: {}", path, e.getClass().getSimpleName());
            throw new ShelterException("공공데이터 서버에 연결하지 못했습니다.");
        }
    }

    // body.items.item 은 결과 수에 따라 배열, 객체, 빈 문자열로 온다
    private List<JsonNode> items(JsonNode body) {
        JsonNode item = body.path("items").path("item");
        List<JsonNode> list = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(list::add);
        } else if (item.isObject()) {
            list.add(item);
        }
        return list;
    }

    private ShelterAnimalDto toAnimal(JsonNode item) {
        List<String> images = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            String url = text(item, "popfile" + i);
            if (url != null) images.add(url.replaceFirst("^http://", "https://"));
        }

        return ShelterAnimalDto.builder()
                .desertionNo(text(item, "desertionNo"))
                .noticeNo(text(item, "noticeNo"))
                .kind(text(item, "kindNm"))
                .color(text(item, "colorCd"))
                .age(formatAge(text(item, "age")))
                .weight(formatWeight(text(item, "weight")))
                .sex(switch (item.path("sexCd").asText()) { case "M" -> "수컷"; case "F" -> "암컷"; default -> "미상"; })
                .neuter(switch (item.path("neuterYn").asText()) { case "Y" -> "완료"; case "N" -> "안 함"; default -> "미상"; })
                .state(text(item, "processState"))
                .happenDate(formatDate(text(item, "happenDt")))
                .happenPlace(text(item, "happenPlace"))
                .noticeStart(formatDate(text(item, "noticeSdt")))
                .noticeEnd(formatDate(text(item, "noticeEdt")))
                .specialMark(text(item, "specialMark"))
                .images(images)
                .careName(text(item, "careNm"))
                .careTel(text(item, "careTel"))
                .careAddr(text(item, "careAddr"))
                .orgName(text(item, "orgNm"))
                .build();
    }

    private String text(JsonNode item, String field) {
        String value = item.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    // "2026(60일미만)(년생)" → "2026년생 (60일미만)"
    private String formatAge(String age) {
        if (age == null) return null;
        Matcher m = AGE.matcher(age);
        if (!m.matches()) return age;
        return m.group(1) + "년생" + (m.group(2) != null ? " (" + m.group(2) + ")" : "");
    }

    // "13(Kg)" → "13kg"
    private String formatWeight(String weight) {
        return weight == null ? null : weight.replace("(Kg)", "kg");
    }

    // "20261013" → "2026-10-13"
    private String formatDate(String date) {
        if (date == null || !date.matches("\\d{8}")) return date;
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6);
    }

    // ✅ 메모리 캐시. 갱신에 실패하면 이전 값을 그대로 돌려준다
    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Duration ttl, Supplier<T> loader) {
        CacheEntry entry = cache.get(key);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt > now) {
            return (T) entry.value;
        }
        try {
            T value = loader.get();
            if (cache.size() > 500) cache.clear();
            cache.put(key, new CacheEntry(value, now + ttl.toMillis()));
            return value;
        } catch (ShelterException e) {
            if (entry != null) return (T) entry.value;
            throw e;
        }
    }

    private record CacheEntry(Object value, long expiresAt) {
    }
}
