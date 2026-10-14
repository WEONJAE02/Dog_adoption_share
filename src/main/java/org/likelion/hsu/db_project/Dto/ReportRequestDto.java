package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDto {
    private String targetType;   // dog / post / comment
    private Long targetId;
    private String reason;
    private String detail;
}
