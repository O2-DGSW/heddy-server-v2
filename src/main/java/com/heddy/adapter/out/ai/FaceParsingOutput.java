package com.heddy.adapter.out.ai;

import java.awt.image.BufferedImage;

/** ONNX logits를 입력 해상도로 확대한 얼굴 파싱 결과. */
record FaceParsingOutput(
        int width,
        int height,
        int[] rgb,
        byte[] labels,
        float[] hairProbability
) {
    static FaceParsingOutput from(BufferedImage image, float[][][] logits) {
        int classes = logits.length;
        int outputHeight = logits[0].length;
        int outputWidth = logits[0][0].length;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = new int[width * height];
        byte[] labels = new byte[width * height];
        float[] hairProbability = new float[width * height];

        for (int y = 0; y < height; y++) {
            int outputY = Math.min(outputHeight - 1, y * outputHeight / height);
            for (int x = 0; x < width; x++) {
                int outputX = Math.min(outputWidth - 1, x * outputWidth / width);
                int index = y * width + x;
                rgb[index] = image.getRGB(x, y) & 0x00ffffff;

                int bestClass = 0;
                float maximum = logits[0][outputY][outputX];
                for (int label = 1; label < classes; label++) {
                    float value = logits[label][outputY][outputX];
                    if (value > maximum) {
                        maximum = value;
                        bestClass = label;
                    }
                }
                labels[index] = (byte) bestClass;

                double denominator = 0;
                for (int label = 0; label < classes; label++) {
                    denominator += Math.exp(logits[label][outputY][outputX] - maximum);
                }
                hairProbability[index] = (float) (Math.exp(
                        logits[HairMetricCalculator.HAIR_LABEL][outputY][outputX] - maximum)
                        / denominator);
            }
        }
        return new FaceParsingOutput(width, height, rgb, labels, hairProbability);
    }

    int label(int x, int y) {
        return Byte.toUnsignedInt(labels[y * width + x]);
    }

    boolean isHair(int x, int y) {
        return label(x, y) == HairMetricCalculator.HAIR_LABEL;
    }

    int rgb(int x, int y) {
        return rgb[y * width + x];
    }
}
