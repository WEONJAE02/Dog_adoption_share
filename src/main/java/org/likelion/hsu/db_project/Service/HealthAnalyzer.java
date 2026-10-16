package org.likelion.hsu.db_project.Service;

import org.likelion.hsu.db_project.Dto.HealthResult;
import org.likelion.hsu.db_project.Entity.MyPet;

import java.util.List;

/**
 * 건강체크 분석기. 지금은 샘플 분석기만 있고, AI 분석기도 같은 형식으로 결과를 돌려준다.
 */
public interface HealthAnalyzer {

    /** 결과에 함께 저장하는 분석기 이름 (sample, 모델 이름 등) */
    String name();

    HealthResult analyze(Input input);

    /** frameFiles: 업로드 폴더에 저장된 프레임 파일명 (촬영 순서) */
    record Input(MyPet pet, String bodyPart, String source, List<String> symptoms, String memo, List<String> frameFiles) {}
}
