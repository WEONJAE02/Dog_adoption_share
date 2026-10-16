package org.likelion.hsu.db_project.Dto;

import lombok.*;

/**
 * 같은 반려동물·같은 부위의 지난 체크와 비교.
 * change 는 긴급도만으로 판단한다 (BETTER: 덜 급해짐, SAME, WORSE: 더 급해짐, FIRST: 지난 체크 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCompareDto {

    public static final String BETTER = "BETTER";
    public static final String SAME = "SAME";
    public static final String WORSE = "WORSE";
    public static final String FIRST = "FIRST";

    private HealthCheckResponseDto current;
    private HealthCheckResponseDto previous;   // 없으면 null
    private String change;
    private int sequence;                      // 같은 부위 몇 번째 체크인지 (1부터)
    private long daysBetween;                  // 지난 체크와 며칠 차이인지 (지난 체크가 없으면 0)
}
