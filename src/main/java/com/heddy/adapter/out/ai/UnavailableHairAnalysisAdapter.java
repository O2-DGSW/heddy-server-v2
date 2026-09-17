package com.heddy.adapter.out.ai;

import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.port.out.HairAnalysisEnginePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 모델 기능을 끈 환경에서 더미 점수를 만들지 않고 분석 접수를 막는 구현. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "false",
        matchIfMissing = true)
public class UnavailableHairAnalysisAdapter implements HairAnalysisEnginePort {

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public HairAnalysisOutcome analyze(byte[] imageBytes) {
        throw new IllegalStateException("로컬 헤어 분석 모델이 설정되지 않았습니다");
    }
}
