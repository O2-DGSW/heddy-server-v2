package com.heddy.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.analysis.model.ConfidenceGrade;
import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.model.HairAnalysisPrediction;
import com.heddy.domain.analysis.model.MetricScore;
import com.heddy.domain.analysis.model.MetricType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 파싱 모델의 실제 출력 위에서만 동작하는 지표 계산기. 모델 출력 없이 임의 점수를 만들지 않는다. */
final class HairMetricCalculator {

    static final int SKIN_LABEL = 1;
    static final int NOSE_LABEL = 2;
    static final int LEFT_EYE_LABEL = 4;
    static final int RIGHT_EYE_LABEL = 5;
    static final int HAIR_LABEL = 13;

    private static final int MAX_COLOR_SAMPLES = 20_000;

    private final ObjectMapper objectMapper;
    private final String modelVersion;
    private final double minimumConfidenceScore;

    HairMetricCalculator(
            ObjectMapper objectMapper, String modelVersion, double minimumConfidenceScore
    ) {
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException("modelVersion 은 필수입니다");
        }
        if (minimumConfidenceScore < 0 || minimumConfidenceScore > 100) {
            throw new IllegalArgumentException("최소 신뢰도는 0~100 이어야 합니다");
        }
        this.objectMapper = objectMapper;
        this.modelVersion = modelVersion;
        this.minimumConfidenceScore = minimumConfidenceScore;
    }

    HairAnalysisOutcome calculate(FaceParsingOutput parsing) {
        Region hair = region(parsing, HAIR_LABEL);
        Region leftEye = region(parsing, LEFT_EYE_LABEL);
        Region rightEye = region(parsing, RIGHT_EYE_LABEL);
        Region nose = region(parsing, NOSE_LABEL);

        double imagePixels = (double) parsing.width() * parsing.height();
        double hairAreaRatio = hair.count / imagePixels;
        if (hair.count < imagePixels * 0.01) {
            return unavailable("HAIR_NOT_DETECTED", "사진에서 분석할 머리 영역을 찾지 못했습니다.");
        }
        if (!hasUsableEyeAxis(leftEye, rightEye)) {
            return unavailable("FACE_NOT_FRONTAL", "두 눈이 보이도록 정면에서 촬영해 주세요.");
        }

        Axis axis = Axis.from(leftEye, rightEye);
        double colorUniformity = colorUniformity(parsing, hair.count);
        double volumeBalance = volumeBalance(parsing, axis);
        double shapeSymmetry = shapeSymmetry(parsing, axis);
        Roughness roughness = roughness(parsing, hair.count);

        double meanHairProbability = meanHairProbability(parsing);
        double rollDegrees = Math.toDegrees(Math.abs(Math.atan2(axis.eyeDy, axis.eyeDx)));
        double frontalness = frontalness(axis, nose, rollDegrees);
        double blurVariance = laplacianVariance(parsing);
        double lightingUniformity = lightingUniformity(parsing);
        double areaQuality = areaQuality(hairAreaRatio);
        double sharpnessQuality = clamp((blurVariance - 15.0) / 160.0, 0, 1);
        double segmentationQuality = clamp((meanHairProbability - 0.35) / 0.60, 0, 1);
        double confidence = 100.0 * weightedHarmonicMean(
                new double[]{segmentationQuality, areaQuality, frontalness,
                        sharpnessQuality, lightingUniformity},
                new double[]{0.28, 0.12, 0.25, 0.20, 0.15});

        if (confidence < minimumConfidenceScore) {
            return unavailable("LOW_CONFIDENCE",
                    lowConfidenceMessage(frontalness, sharpnessQuality, lightingUniformity));
        }

        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        metrics.put(MetricType.COLOR_UNIFORMITY, metric(colorUniformity));
        metrics.put(MetricType.SHAPE_SYMMETRY, metric(shapeSymmetry));
        metrics.put(MetricType.VOLUME_BALANCE, metric(volumeBalance));
        metrics.put(MetricType.ROUGHNESS, metric(roughness.score));

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("pipeline", "segformer-plus-deterministic-metrics-v1");
        raw.put("hair_area_ratio", round(hairAreaRatio, 4));
        raw.put("mean_hair_probability", round(meanHairProbability, 4));
        raw.put("face_roll_degrees", round(rollDegrees, 2));
        raw.put("frontalness", round(frontalness, 4));
        raw.put("blur_laplacian_variance", round(blurVariance, 2));
        raw.put("lighting_uniformity", round(lightingUniformity, 4));
        raw.put("shape_mirrored_iou", round(shapeSymmetry / 100.0, 4));
        raw.put("roughness_contour_irregularity", round(roughness.contourIrregularity, 4));
        raw.put("roughness_glcm_contrast", round(roughness.glcmContrast, 4));

        String summary = summary(colorUniformity, volumeBalance, shapeSymmetry, roughness.score);
        HairAnalysisPrediction prediction = new HairAnalysisPrediction(
                metrics, metric(confidence), modelVersion, summary, json(raw));
        return new HairAnalysisOutcome.Succeeded(prediction);
    }

    private HairAnalysisOutcome unavailable(String code, String message) {
        return new HairAnalysisOutcome.Unavailable(code, message);
    }

    private static MetricScore metric(double score) {
        BigDecimal rounded = BigDecimal.valueOf(clamp(score, 0, 100))
                .setScale(2, RoundingMode.HALF_UP);
        ConfidenceGrade grade = rounded.compareTo(BigDecimal.valueOf(90)) >= 0
                ? ConfidenceGrade.HIGH
                : rounded.compareTo(BigDecimal.valueOf(80)) >= 0
                ? ConfidenceGrade.MEDIUM : ConfidenceGrade.LOW;
        return new MetricScore(rounded, grade);
    }

    private static double colorUniformity(FaceParsingOutput parsing, int hairCount) {
        int step = Math.max(1, hairCount / MAX_COLOR_SAMPLES);
        List<Lab> samples = new ArrayList<>(Math.min(hairCount, MAX_COLOR_SAMPLES));
        int seen = 0;
        for (int y = 0; y < parsing.height(); y++) {
            for (int x = 0; x < parsing.width(); x++) {
                if (parsing.isHair(x, y) && seen++ % step == 0) {
                    samples.add(toLab(parsing.rgb(x, y)));
                }
            }
        }
        if (samples.size() < 3) {
            return 0;
        }
        KMeans clusters = KMeans.fit(samples, 3, 12);
        double dispersion = 0;
        for (int first = 0; first < clusters.centers.length; first++) {
            for (int second = first + 1; second < clusters.centers.length; second++) {
                double weight = (double) clusters.counts[first] * clusters.counts[second]
                        / ((double) samples.size() * samples.size());
                dispersion += 2.0 * weight * deltaE2000(
                        clusters.centers[first], clusters.centers[second]);
            }
        }
        return clamp(100.0 - dispersion * 3.0, 0, 100);
    }

    private static double volumeBalance(FaceParsingOutput parsing, Axis axis) {
        int left = 0;
        int right = 0;
        for (int y = 0; y < parsing.height(); y++) {
            for (int x = 0; x < parsing.width(); x++) {
                if (!parsing.isHair(x, y)) {
                    continue;
                }
                if (axis.horizontalComponent(x, y) < 0) {
                    left++;
                } else {
                    right++;
                }
            }
        }
        int total = left + right;
        return total == 0 ? 0 : 100.0 * (1.0 - Math.abs(left - right) / (double) total);
    }

    private static double shapeSymmetry(FaceParsingOutput parsing, Axis axis) {
        boolean[] mirrored = new boolean[parsing.width() * parsing.height()];
        for (int y = 0; y < parsing.height(); y++) {
            for (int x = 0; x < parsing.width(); x++) {
                if (!parsing.isHair(x, y)) {
                    continue;
                }
                Point reflected = axis.reflect(x, y);
                int rx = (int) Math.round(reflected.x);
                int ry = (int) Math.round(reflected.y);
                if (rx >= 0 && rx < parsing.width() && ry >= 0 && ry < parsing.height()) {
                    mirrored[ry * parsing.width() + rx] = true;
                }
            }
        }
        int intersection = 0;
        int union = 0;
        for (int y = 0; y < parsing.height(); y++) {
            for (int x = 0; x < parsing.width(); x++) {
                boolean original = parsing.isHair(x, y);
                boolean reflected = mirrored[y * parsing.width() + x];
                if (original || reflected) {
                    union++;
                }
                if (original && reflected) {
                    intersection++;
                }
            }
        }
        return union == 0 ? 0 : 100.0 * intersection / union;
    }

    private static Roughness roughness(FaceParsingOutput parsing, int hairCount) {
        int boundary = 0;
        long[][] glcm = new long[16][16];
        long pairs = 0;
        double edgeSum = 0;
        int edgeCount = 0;
        int[][] directions = {{1, 0}, {0, 1}};

        for (int y = 1; y < parsing.height() - 1; y++) {
            for (int x = 1; x < parsing.width() - 1; x++) {
                if (!parsing.isHair(x, y)) {
                    continue;
                }
                if (!parsing.isHair(x - 1, y) || !parsing.isHair(x + 1, y)
                        || !parsing.isHair(x, y - 1) || !parsing.isHair(x, y + 1)) {
                    boundary++;
                }
                int center = gray(parsing.rgb(x, y));
                int gx = gray(parsing.rgb(x + 1, y)) - gray(parsing.rgb(x - 1, y));
                int gy = gray(parsing.rgb(x, y + 1)) - gray(parsing.rgb(x, y - 1));
                edgeSum += Math.hypot(gx, gy) / 360.0;
                edgeCount++;
                for (int[] direction : directions) {
                    int nx = x + direction[0];
                    int ny = y + direction[1];
                    if (parsing.isHair(nx, ny)) {
                        glcm[Math.min(15, center / 16)]
                                [Math.min(15, gray(parsing.rgb(nx, ny)) / 16)]++;
                        pairs++;
                    }
                }
            }
        }
        double idealPerimeter = 2.0 * Math.sqrt(Math.PI * hairCount);
        double contourIrregularity = idealPerimeter == 0 ? 0 : boundary / idealPerimeter;
        double edgeDensity = edgeCount == 0 ? 0 : edgeSum / edgeCount;
        double contrast = 0;
        if (pairs > 0) {
            for (int first = 0; first < 16; first++) {
                for (int second = 0; second < 16; second++) {
                    double probability = glcm[first][second] / (double) pairs;
                    contrast += probability * Math.pow(first - second, 2) / 225.0;
                }
            }
        }
        double score = 100.0 * clamp(
                0.42 * clamp((contourIrregularity - 1.0) / 2.5, 0, 1)
                        + 0.33 * clamp(edgeDensity / 0.45, 0, 1)
                        + 0.25 * clamp(contrast / 0.12, 0, 1),
                0, 1);
        return new Roughness(score, contourIrregularity, contrast);
    }

    private static double meanHairProbability(FaceParsingOutput parsing) {
        double sum = 0;
        int count = 0;
        for (int index = 0; index < parsing.labels().length; index++) {
            if (Byte.toUnsignedInt(parsing.labels()[index]) == HAIR_LABEL) {
                sum += parsing.hairProbability()[index];
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private static double frontalness(Axis axis, Region nose, double rollDegrees) {
        if (nose.count == 0) {
            return 0.45;
        }
        double noseOffset = Math.abs(axis.horizontalComponent(nose.centerX(), nose.centerY()));
        double normalizedOffset = noseOffset / Math.max(1.0, axis.eyeDistance);
        double yawQuality = clamp(1.0 - normalizedOffset / 0.35, 0, 1);
        double rollQuality = clamp(1.0 - rollDegrees / 18.0, 0, 1);
        return 0.75 * yawQuality + 0.25 * rollQuality;
    }

    private static double laplacianVariance(FaceParsingOutput parsing) {
        double sum = 0;
        double squared = 0;
        int count = 0;
        for (int y = 1; y < parsing.height() - 1; y++) {
            for (int x = 1; x < parsing.width() - 1; x++) {
                int center = gray(parsing.rgb(x, y));
                double value = gray(parsing.rgb(x - 1, y)) + gray(parsing.rgb(x + 1, y))
                        + gray(parsing.rgb(x, y - 1)) + gray(parsing.rgb(x, y + 1))
                        - 4.0 * center;
                sum += value;
                squared += value * value;
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        double mean = sum / count;
        return squared / count - mean * mean;
    }

    private static double lightingUniformity(FaceParsingOutput parsing) {
        double sum = 0;
        double squared = 0;
        int count = 0;
        for (int index = 0; index < parsing.labels().length; index++) {
            if (Byte.toUnsignedInt(parsing.labels()[index]) == SKIN_LABEL) {
                int value = gray(parsing.rgb()[index]);
                sum += value;
                squared += (double) value * value;
                count++;
            }
        }
        if (count == 0) {
            return 0.4;
        }
        double variance = Math.max(0, squared / count - Math.pow(sum / count, 2));
        return clamp(1.0 - Math.sqrt(variance) / 70.0, 0, 1);
    }

    private static double areaQuality(double ratio) {
        if (ratio < 0.08) {
            return clamp(ratio / 0.08, 0, 1);
        }
        if (ratio > 0.60) {
            return clamp((0.85 - ratio) / 0.25, 0, 1);
        }
        return 1;
    }

    private static double weightedHarmonicMean(double[] values, double[] weights) {
        double weightSum = 0;
        double denominator = 0;
        for (int index = 0; index < values.length; index++) {
            weightSum += weights[index];
            denominator += weights[index] / Math.max(0.05, values[index]);
        }
        return denominator == 0 ? 0 : weightSum / denominator;
    }

    private static String lowConfidenceMessage(
            double frontalness, double sharpness, double lighting
    ) {
        if (frontalness <= sharpness && frontalness <= lighting) {
            return "얼굴과 머리 전체가 보이도록 정면에서 촬영해 주세요.";
        }
        if (sharpness <= lighting) {
            return "사진이 흔들리거나 흐립니다. 카메라를 고정하고 다시 촬영해 주세요.";
        }
        return "그림자와 역광을 피하고 밝기가 고른 곳에서 촬영해 주세요.";
    }

    private static String summary(
            double color, double volume, double symmetry, double roughness
    ) {
        String colorText = color >= 80 ? "색상 분포가 비교적 균일합니다"
                : "사진에서 색상 편차가 일부 감지되었습니다";
        String balanceText = Math.min(volume, symmetry) >= 80
                ? "좌우 실루엣도 비교적 균형적입니다"
                : "좌우 실루엣에 차이가 보입니다";
        String roughnessText = roughness >= 50
                ? "거칠게 보이는 경계와 텍스처가 일부 감지되었습니다"
                : "거칠기 징후는 비교적 적게 감지되었습니다";
        return colorText + ". " + balanceText + ". " + roughnessText + ".";
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("분석 근거를 JSON으로 만들 수 없습니다", exception);
        }
    }

    private static Region region(FaceParsingOutput parsing, int label) {
        long sumX = 0;
        long sumY = 0;
        int count = 0;
        for (int y = 0; y < parsing.height(); y++) {
            for (int x = 0; x < parsing.width(); x++) {
                if (parsing.label(x, y) == label) {
                    sumX += x;
                    sumY += y;
                    count++;
                }
            }
        }
        return new Region(count, sumX, sumY);
    }

    private static int gray(int rgb) {
        int red = (rgb >>> 16) & 0xff;
        int green = (rgb >>> 8) & 0xff;
        int blue = rgb & 0xff;
        return (int) Math.round(0.299 * red + 0.587 * green + 0.114 * blue);
    }

    private static Lab toLab(int rgb) {
        double red = linearize(((rgb >>> 16) & 0xff) / 255.0);
        double green = linearize(((rgb >>> 8) & 0xff) / 255.0);
        double blue = linearize((rgb & 0xff) / 255.0);
        double x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047;
        double y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue;
        double z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883;
        double fx = labFunction(x);
        double fy = labFunction(y);
        double fz = labFunction(z);
        return new Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz));
    }

    private static double linearize(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double labFunction(double value) {
        double delta = 6.0 / 29.0;
        return value > delta * delta * delta
                ? Math.cbrt(value)
                : value / (3 * delta * delta) + 4.0 / 29.0;
    }

    /** ISO/CIE 11664-6 CIEDE2000 색차. */
    private static double deltaE2000(Lab first, Lab second) {
        double c1 = Math.hypot(first.a, first.b);
        double c2 = Math.hypot(second.a, second.b);
        double meanC = (c1 + c2) / 2.0;
        double g = 0.5 * (1 - Math.sqrt(Math.pow(meanC, 7)
                / (Math.pow(meanC, 7) + Math.pow(25.0, 7))));
        double a1 = (1 + g) * first.a;
        double a2 = (1 + g) * second.a;
        double adjustedC1 = Math.hypot(a1, first.b);
        double adjustedC2 = Math.hypot(a2, second.b);
        double h1 = hueDegrees(first.b, a1);
        double h2 = hueDegrees(second.b, a2);

        double deltaL = second.l - first.l;
        double deltaC = adjustedC2 - adjustedC1;
        double deltaHAngle;
        if (adjustedC1 * adjustedC2 == 0) {
            deltaHAngle = 0;
        } else if (Math.abs(h2 - h1) <= 180) {
            deltaHAngle = h2 - h1;
        } else if (h2 <= h1) {
            deltaHAngle = h2 - h1 + 360;
        } else {
            deltaHAngle = h2 - h1 - 360;
        }
        double deltaH = 2 * Math.sqrt(adjustedC1 * adjustedC2)
                * Math.sin(Math.toRadians(deltaHAngle / 2));
        double meanL = (first.l + second.l) / 2;
        double meanAdjustedC = (adjustedC1 + adjustedC2) / 2;
        double meanH;
        if (adjustedC1 * adjustedC2 == 0) {
            meanH = h1 + h2;
        } else if (Math.abs(h1 - h2) <= 180) {
            meanH = (h1 + h2) / 2;
        } else if (h1 + h2 < 360) {
            meanH = (h1 + h2 + 360) / 2;
        } else {
            meanH = (h1 + h2 - 360) / 2;
        }
        double t = 1 - 0.17 * Math.cos(Math.toRadians(meanH - 30))
                + 0.24 * Math.cos(Math.toRadians(2 * meanH))
                + 0.32 * Math.cos(Math.toRadians(3 * meanH + 6))
                - 0.20 * Math.cos(Math.toRadians(4 * meanH - 63));
        double sl = 1 + 0.015 * Math.pow(meanL - 50, 2)
                / Math.sqrt(20 + Math.pow(meanL - 50, 2));
        double sc = 1 + 0.045 * meanAdjustedC;
        double sh = 1 + 0.015 * meanAdjustedC * t;
        double rotation = 30 * Math.exp(-Math.pow((meanH - 275) / 25, 2));
        double rc = 2 * Math.sqrt(Math.pow(meanAdjustedC, 7)
                / (Math.pow(meanAdjustedC, 7) + Math.pow(25.0, 7)));
        double rt = -rc * Math.sin(Math.toRadians(2 * rotation));
        double l = deltaL / sl;
        double c = deltaC / sc;
        double h = deltaH / sh;
        return Math.sqrt(l * l + c * c + h * h + rt * c * h);
    }

    private static double hueDegrees(double b, double a) {
        double degrees = Math.toDegrees(Math.atan2(b, a));
        return degrees < 0 ? degrees + 360 : degrees;
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean hasUsableEyeAxis(Region leftEye, Region rightEye) {
        if (leftEye.count == 0 || rightEye.count == 0) {
            return false;
        }
        return Math.hypot(
                rightEye.centerX() - leftEye.centerX(),
                rightEye.centerY() - leftEye.centerY()) >= 2;
    }

    private record Point(double x, double y) {
    }

    private record Region(int count, long sumX, long sumY) {
        double centerX() {
            return count == 0 ? 0 : sumX / (double) count;
        }

        double centerY() {
            return count == 0 ? 0 : sumY / (double) count;
        }
    }

    private static final class Axis {
        private final double centerX;
        private final double centerY;
        private final double horizontalX;
        private final double horizontalY;
        private final double eyeDx;
        private final double eyeDy;
        private final double eyeDistance;

        private Axis(double centerX, double centerY, double eyeDx, double eyeDy) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.eyeDx = eyeDx;
            this.eyeDy = eyeDy;
            this.eyeDistance = Math.hypot(eyeDx, eyeDy);
            this.horizontalX = eyeDx / eyeDistance;
            this.horizontalY = eyeDy / eyeDistance;
        }

        static Axis from(Region firstEye, Region secondEye) {
            double firstX = firstEye.centerX();
            double firstY = firstEye.centerY();
            double secondX = secondEye.centerX();
            double secondY = secondEye.centerY();
            double dx = secondX - firstX;
            double dy = secondY - firstY;
            if (Math.hypot(dx, dy) < 2) {
                throw new IllegalArgumentException("눈 중심점을 구분할 수 없습니다");
            }
            return new Axis((firstX + secondX) / 2, (firstY + secondY) / 2, dx, dy);
        }

        double horizontalComponent(double x, double y) {
            return (x - centerX) * horizontalX + (y - centerY) * horizontalY;
        }

        Point reflect(double x, double y) {
            double horizontal = horizontalComponent(x, y);
            return new Point(x - 2 * horizontal * horizontalX,
                    y - 2 * horizontal * horizontalY);
        }
    }

    private record Lab(double l, double a, double b) {
        Lab add(Lab other) {
            return new Lab(l + other.l, a + other.a, b + other.b);
        }

        Lab divide(double divisor) {
            return new Lab(l / divisor, a / divisor, b / divisor);
        }
    }

    private record Roughness(double score, double contourIrregularity, double glcmContrast) {
    }

    private record KMeans(Lab[] centers, int[] counts) {
        static KMeans fit(List<Lab> samples, int k, int iterations) {
            Lab[] centers = initialize(samples, k);
            int[] assignment = new int[samples.size()];
            int[] counts = new int[k];
            for (int iteration = 0; iteration < iterations; iteration++) {
                java.util.Arrays.fill(counts, 0);
                Lab[] sums = new Lab[k];
                java.util.Arrays.fill(sums, new Lab(0, 0, 0));
                for (int index = 0; index < samples.size(); index++) {
                    int cluster = closest(samples.get(index), centers);
                    assignment[index] = cluster;
                    counts[cluster]++;
                    sums[cluster] = sums[cluster].add(samples.get(index));
                }
                for (int cluster = 0; cluster < k; cluster++) {
                    if (counts[cluster] > 0) {
                        centers[cluster] = sums[cluster].divide(counts[cluster]);
                    }
                }
            }
            return new KMeans(centers, counts.clone());
        }

        private static Lab[] initialize(List<Lab> samples, int k) {
            Lab[] centers = new Lab[k];
            centers[0] = samples.get(0);
            for (int index = 1; index < k; index++) {
                Lab farthest = samples.get(0);
                double farthestDistance = -1;
                for (Lab sample : samples) {
                    double nearest = Double.MAX_VALUE;
                    for (int existing = 0; existing < index; existing++) {
                        nearest = Math.min(nearest, squaredDistance(sample, centers[existing]));
                    }
                    if (nearest > farthestDistance) {
                        farthestDistance = nearest;
                        farthest = sample;
                    }
                }
                centers[index] = farthest;
            }
            return centers;
        }

        private static int closest(Lab sample, Lab[] centers) {
            int best = 0;
            double bestDistance = squaredDistance(sample, centers[0]);
            for (int index = 1; index < centers.length; index++) {
                double distance = squaredDistance(sample, centers[index]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = index;
                }
            }
            return best;
        }

        private static double squaredDistance(Lab first, Lab second) {
            return Math.pow(first.l - second.l, 2)
                    + Math.pow(first.a - second.a, 2)
                    + Math.pow(first.b - second.b, 2);
        }
    }
}
