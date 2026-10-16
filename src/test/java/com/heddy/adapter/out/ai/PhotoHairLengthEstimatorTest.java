package com.heddy.adapter.out.ai;

import com.heddy.domain.account.model.HairProfile.HairLength;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PhotoHairLengthEstimatorTest {
    private final PhotoHairLengthEstimator estimator = new PhotoHairLengthEstimator();
    @Test void separatesShortChinAndLongVisibleHair() {
        assertThat(estimator.estimate(portrait(80))).isEqualTo(HairLength.SHORT);
        assertThat(estimator.estimate(portrait(140))).isEqualTo(HairLength.BELOW_CHIN);
        assertThat(estimator.estimate(portrait(220))).isEqualTo(HairLength.BELOW_SHOULDER);
    }
    @Test void rejectsBackViewsInsteadOfInventingLength() {
        var image = portrait(140); rectangle(image, 90, 70, 160, 140, 0);
        assertThat(estimator.estimate(image)).isNull();
    }
    @Test void rejectsMultiplePeopleAndCroppedHair() {
        var image = portrait(140); rectangle(image, 15, 70, 60, 130, 3);
        assertThat(estimator.estimate(image)).isNull();
        assertThat(estimator.estimate(portrait(256))).isNull();
    }
    @Test void rejectsUncertainSegmentation() {
        assertThat(estimator.estimate(new float[256][256][6])).isNull();
    }
    private float[][][] portrait(int hairBottom) {
        float[][][] result = new float[256][256][6];
        rectangle(result, 0, 0, 256, 256, 0);
        rectangle(result, 65, 30, 185, hairBottom, 1);
        rectangle(result, 90, 70, 160, 140, 3);
        return result;
    }
    private void rectangle(float[][][] image, int left, int top, int right, int bottom, int label) {
        for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
            java.util.Arrays.fill(image[y][x], -8); image[y][x][label] = 8;
        }
    }
}
