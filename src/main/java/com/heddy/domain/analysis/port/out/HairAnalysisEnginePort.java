package com.heddy.domain.analysis.port.out;

import com.heddy.domain.analysis.model.HairAnalysisOutcome;

/** 외부 API 없이 애플리케이션 프로세스 안의 로컬 모델을 실행하는 경계. */
public interface HairAnalysisEnginePort {

    /** 모델 파일을 읽고 추론 세션을 만들 수 있는 상태인지 확인한다. */
    boolean isReady();

    /** 이미지 바이트를 로컬 모델로 분석한다. 시스템 오류는 예외로, 촬영 부적합은 결과로 반환한다. */
    HairAnalysisOutcome analyze(byte[] imageBytes);
}
