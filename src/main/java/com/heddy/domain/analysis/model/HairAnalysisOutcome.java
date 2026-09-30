package com.heddy.domain.analysis.model;

import java.util.Objects;

/** 모델 실행의 정상 종료 결과. 촬영 부적합은 시스템 장애와 분리한다. */
public sealed interface HairAnalysisOutcome
        permits HairAnalysisOutcome.Succeeded, HairAnalysisOutcome.Unavailable {

    record Succeeded(HairAnalysisPrediction prediction) implements HairAnalysisOutcome {
        public Succeeded {
            Objects.requireNonNull(prediction, "prediction");
        }
    }

    record Unavailable(String code, String message) implements HairAnalysisOutcome {
        public Unavailable {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("분석 불가 코드가 필요합니다");
            }
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("분석 불가 안내가 필요합니다");
            }
        }
    }
}
