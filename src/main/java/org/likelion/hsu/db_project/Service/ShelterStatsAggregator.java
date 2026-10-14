package org.likelion.hsu.db_project.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.likelion.hsu.db_project.Dto.ShelterStatsDetailDto;
import org.likelion.hsu.db_project.Dto.ShelterStatsDetailDto.Count;
import org.likelion.hsu.db_project.Dto.ShelterStatsDetailDto.Group;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 구조동물 목록을 한 건씩 받아 상세 통계를 누적한다. (축종 하나 또는 전체)
 */
class ShelterStatsAggregator {

    static final String PROTECTING = "보호·공고 중";
    private static final List<String> OUTCOMES = List.of("입양", "반환", "기증", "방사", "자연사", "안락사", PROTECTING, "기타");
    private static final List<String> AGE_GROUPS = List.of("1살 미만", "1~2살", "3~6살", "7살 이상", "미상");
    private static final List<String> SIZE_GROUPS = List.of("소형 (5kg 미만)", "중형 (5~15kg)", "대형 (15kg 이상)", "미상");
    private static final Pattern BIRTH_YEAR = Pattern.compile("^(\\d{4})");
    private static final Pattern WEIGHT = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)");

    private final String kind;
    private final LocalDate from;
    private final LocalDate to;
    private final boolean withSizes;

    private long rescued;
    private final Map<String, Long> outcomes = new LinkedHashMap<>();
    private final Map<String, Long> daily = new TreeMap<>();
    private final Map<String, long[]> regions = new HashMap<>();      // [구조, 처리완료, 입양, 안락사]
    private final Map<String, Long> breeds = new HashMap<>();
    private final Map<String, long[]> ages = new LinkedHashMap<>();
    private final Map<String, long[]> sizes = new LinkedHashMap<>();
    private final Map<String, Long> species = new LinkedHashMap<>();
    private final Map<String, Long> sex = new LinkedHashMap<>();
    private final Map<String, Long> neuter = new LinkedHashMap<>();

    ShelterStatsAggregator(String kind, LocalDate from, LocalDate to) {
        this.kind = kind;
        this.from = from;
        this.to = to;
        this.withSizes = "dog".equals(kind);
        OUTCOMES.forEach(o -> outcomes.put(o, 0L));
        AGE_GROUPS.forEach(a -> ages.put(a, new long[4]));
        SIZE_GROUPS.forEach(s -> sizes.put(s, new long[4]));
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            daily.put(d.toString(), 0L);
        }
        List.of("수컷", "암컷", "미상").forEach(s -> sex.put(s, 0L));
        List.of("완료", "안 함", "미상").forEach(n -> neuter.put(n, 0L));
    }

    void add(JsonNode item) {
        rescued++;
        String outcome = outcome(item.path("processState").asText());
        boolean closed = !PROTECTING.equals(outcome);
        boolean adopted = "입양".equals(outcome);
        boolean euthanized = "안락사".equals(outcome);

        outcomes.merge(outcome, 1L, Long::sum);

        String happen = item.path("happenDt").asText();
        if (happen.matches("\\d{8}")) {
            String date = happen.substring(0, 4) + "-" + happen.substring(4, 6) + "-" + happen.substring(6);
            daily.computeIfPresent(date, (k, v) -> v + 1);
        }

        // "부산광역시 해운대구" → "부산광역시"
        String org = item.path("orgNm").asText("").trim();
        String region = org.isEmpty() ? "미상" : org.split("\\s+")[0];
        count(regions.computeIfAbsent(region, k -> new long[4]), closed, adopted, euthanized);

        String breed = item.path("kindNm").asText("").trim();
        breeds.merge(breed.isEmpty() ? "미상" : breed, 1L, Long::sum);

        count(ages.get(ageGroup(item.path("age").asText(""))), closed, adopted, euthanized);
        if (withSizes) {
            count(sizes.get(sizeGroup(item.path("weight").asText(""))), closed, adopted, euthanized);
        }

        String upKind = item.path("upKindNm").asText("").trim();
        species.merge(upKind.isEmpty() ? "기타" : upKind, 1L, Long::sum);
        sex.merge(switch (item.path("sexCd").asText()) { case "M" -> "수컷"; case "F" -> "암컷"; default -> "미상"; }, 1L, Long::sum);
        neuter.merge(switch (item.path("neuterYn").asText()) { case "Y" -> "완료"; case "N" -> "안 함"; default -> "미상"; }, 1L, Long::sum);
    }

    ShelterStatsDetailDto build() {
        long protecting = outcomes.get(PROTECTING);
        long closed = rescued - protecting;

        List<Group> regionList = regions.entrySet().stream()
                .map(e -> group(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Group::getRescued).reversed())
                .toList();
        List<Count> breedList = breeds.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new Count(e.getKey(), e.getValue()))
                .toList();

        return ShelterStatsDetailDto.builder()
                .kind(kind)
                .from(from.toString())
                .to(to.toString())
                .days((int) (to.toEpochDay() - from.toEpochDay()))
                .rescued(rescued)
                .closed(closed)
                .protecting(protecting)
                .adoptionRate(percent(outcomes.get("입양"), closed))
                .returnRate(percent(outcomes.get("반환"), closed))
                .euthanasiaRate(percent(outcomes.get("안락사"), closed))
                .outcomes(counts(outcomes, true))
                .daily(counts(daily, false))
                .regions(regionList)
                .breeds(breedList)
                .ages(groups(ages))
                .sizes(withSizes ? groups(sizes) : List.of())
                .species("all".equals(kind) ? counts(species, true) : List.of())
                .sex(counts(sex, true))
                .neuter(counts(neuter, true))
                .build();
    }

    // "종료(입양)" → "입양", 그 외 진행 중 상태 → "보호·공고 중"
    private static String outcome(String state) {
        if (!state.startsWith("종료")) return PROTECTING;
        for (String o : OUTCOMES) {
            if (state.contains(o)) return o;
        }
        return "기타";
    }

    private String ageGroup(String age) {
        if (age.contains("60일미만")) return "1살 미만";
        Matcher m = BIRTH_YEAR.matcher(age);
        if (!m.find()) return "미상";
        int years = to.getYear() - Integer.parseInt(m.group(1));
        if (years < 1) return "1살 미만";
        if (years <= 2) return "1~2살";
        if (years <= 6) return "3~6살";
        return "7살 이상";
    }

    private static String sizeGroup(String weight) {
        Matcher m = WEIGHT.matcher(weight);
        if (!m.find()) return "미상";
        double kg = Double.parseDouble(m.group(1));
        if (kg <= 0) return "미상";
        if (kg < 5) return "소형 (5kg 미만)";
        if (kg < 15) return "중형 (5~15kg)";
        return "대형 (15kg 이상)";
    }

    private static void count(long[] c, boolean closed, boolean adopted, boolean euthanized) {
        c[0]++;
        if (closed) c[1]++;
        if (adopted) c[2]++;
        if (euthanized) c[3]++;
    }

    private static Group group(String label, long[] c) {
        return new Group(label, c[0], c[1], c[2], c[3], percent(c[2], c[1]), percent(c[3], c[1]));
    }

    private static List<Group> groups(Map<String, long[]> map) {
        return map.entrySet().stream().map(e -> group(e.getKey(), e.getValue())).toList();
    }

    // skipEmpty: 0건 항목은 빼고 돌려준다 (일별 추이는 0인 날도 유지)
    private static List<Count> counts(Map<String, Long> map, boolean skipEmpty) {
        return map.entrySet().stream()
                .filter(e -> !skipEmpty || e.getValue() > 0)
                .map(e -> new Count(e.getKey(), e.getValue()))
                .toList();
    }

    static double percent(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }
}
