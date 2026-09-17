package com.heddy.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.model.MetricType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class HairMetricCalculatorTest {

    private final HairMetricCalculator calculator = new HairMetricCalculator(
            new ObjectMapper(), "test-segformer-v1", 0);

    @Test
    void neverProducesScoresWithoutHairFromTheSegmentationModel() {
        int size = 64;
        FaceParsingOutput output = new FaceParsingOutput(size, size, new int[size * size],
                new byte[size * size], new float[size * size]);

        assertThat(calculator.calculate(output))
                .isInstanceOfSatisfying(HairAnalysisOutcome.Unavailable.class,
                        unavailable -> assertThat(unavailable.code()).isEqualTo("HAIR_NOT_DETECTED"));
    }

    @Test
    void computesAllFourMetricsOnlyFromAValidModelSegmentation() {
        int width = 96;
        int height = 96;
        int[] rgb = new int[width * height];
        byte[] labels = new byte[width * height];
        float[] hairProbability = new float[width * height];
        Arrays.fill(rgb, 0x00d2aa8a);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                // 선명도 신호가 생기도록 미세한 체크 패턴을 둔다.
                rgb[index] = ((x + y) & 1) == 0 ? 0x00604030 : 0x00806040;
                if (y < 42 && x >= 12 && x < 84) {
                    labels[index] = HairMetricCalculator.HAIR_LABEL;
                    hairProbability[index] = 0.99f;
                } else if (x >= 28 && x < 68 && y >= 42 && y < 90) {
                    labels[index] = HairMetricCalculator.SKIN_LABEL;
                }
            }
        }
        mark(labels, width, 36, 52, HairMetricCalculator.LEFT_EYE_LABEL);
        mark(labels, width, 60, 52, HairMetricCalculator.RIGHT_EYE_LABEL);
        mark(labels, width, 48, 65, HairMetricCalculator.NOSE_LABEL);

        HairAnalysisOutcome outcome = calculator.calculate(
                new FaceParsingOutput(width, height, rgb, labels, hairProbability));

        assertThat(outcome).isInstanceOfSatisfying(HairAnalysisOutcome.Succeeded.class,
                succeeded -> {
                    assertThat(succeeded.prediction().modelVersion())
                            .isEqualTo("test-segformer-v1");
                    assertThat(succeeded.prediction().metrics())
                            .containsOnlyKeys(MetricType.values());
                    assertThat(succeeded.prediction().evidence())
                            .contains("mean_hair_probability", "shape_mirrored_iou");
                });
    }

    private static void mark(byte[] labels, int width, int centerX, int centerY, int label) {
        for (int y = centerY - 2; y <= centerY + 2; y++) {
            for (int x = centerX - 3; x <= centerX + 3; x++) {
                labels[y * width + x] = (byte) label;
            }
        }
    }
}
