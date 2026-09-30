package com.heddy.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 가중치는 저장소에 넣지 않으므로 경로가 주어진 환경에서만 도는 모델 스모크 테스트.
 */
@EnabledIfEnvironmentVariable(named = "HAIR_ANALYSIS_TEST_MODEL", matches = ".+")
class OnnxFaceParsingHairAnalysisAdapterModelTest {

    @Test
    void runsARealOnnxModelOnARealImage() throws Exception {
        String modelPath = System.getenv("HAIR_ANALYSIS_TEST_MODEL");
        String imagePath = System.getenv("HAIR_ANALYSIS_TEST_IMAGE");
        assertThat(imagePath).as("HAIR_ANALYSIS_TEST_IMAGE").isNotBlank();

        OnnxFaceParsingHairAnalysisAdapter adapter =
                new OnnxFaceParsingHairAnalysisAdapter(
                        new ObjectMapper(), modelPath, "real-model-smoke-test", 512, 0);
        try {
            HairAnalysisOutcome outcome = adapter.analyze(
                    Files.readAllBytes(Path.of(imagePath)));
            assertThat(outcome).isInstanceOf(HairAnalysisOutcome.Succeeded.class);
        } finally {
            adapter.close();
        }
    }
}
