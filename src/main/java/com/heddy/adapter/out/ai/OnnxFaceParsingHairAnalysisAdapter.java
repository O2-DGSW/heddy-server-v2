package com.heddy.adapter.out.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.port.out.HairAnalysisEnginePort;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * SegFormer 얼굴 파싱 ONNX 모델을 JVM 안에서 실행한다. 사진은 외부 AI API 로 전송하지 않는다.
 * 모델의 19개 클래스 중 hair(13), l_eye(4), r_eye(5), skin(1), nose(2)를 지표 계산에 사용한다.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
public class OnnxFaceParsingHairAnalysisAdapter implements HairAnalysisEnginePort {

    private static final float[] IMAGE_MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] IMAGE_STD = {0.229f, 0.224f, 0.225f};

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final int inputSize;
    private final HairMetricCalculator metricCalculator;

    public OnnxFaceParsingHairAnalysisAdapter(
            ObjectMapper objectMapper,
            @Value("${app.ai.model-path}") String modelPath,
            @Value("${app.ai.model-version}") String modelVersion,
            @Value("${app.ai.input-size}") int inputSize,
            @Value("${app.ai.minimum-confidence-score}") double minimumConfidenceScore
    ) {
        if (modelPath == null || modelPath.isBlank()) {
            throw new IllegalStateException(
                    "HAIR_ANALYSIS_ENABLED=true 이면 HAIR_ANALYSIS_MODEL_PATH 가 필요합니다");
        }
        Path path = Path.of(modelPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("읽을 수 있는 ONNX 모델 파일이 없습니다: " + path);
        }
        if (inputSize < 128 || inputSize > 1024) {
            throw new IllegalArgumentException("분석 입력 크기는 128~1024 여야 합니다");
        }
        try {
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            session = environment.createSession(path.toString(), options);
            if (session.getInputNames().size() != 1) {
                throw new IllegalStateException("얼굴 파싱 모델 입력은 하나여야 합니다");
            }
            inputName = session.getInputNames().iterator().next();
        } catch (OrtException exception) {
            throw new IllegalStateException("ONNX 얼굴 파싱 모델을 열 수 없습니다", exception);
        }
        this.inputSize = inputSize;
        this.metricCalculator = new HairMetricCalculator(
                objectMapper, modelVersion, minimumConfidenceScore);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public HairAnalysisOutcome analyze(byte[] imageBytes) {
        final BufferedImage image;
        try {
            image = decode(imageBytes);
        } catch (UnsupportedImageException unsupported) {
            return new HairAnalysisOutcome.Unavailable(
                    "UNSUPPORTED_IMAGE_FORMAT", unsupported.getMessage());
        }
        BufferedImage resized = resize(image, inputSize, inputSize);
        float[] input = normalizedChannelsFirst(resized);
        long[] shape = {1, 3, inputSize, inputSize};

        try (OnnxTensor tensor = OnnxTensor.createTensor(
                environment, FloatBuffer.wrap(input), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            Object value = result.get(0).getValue();
            if (!(value instanceof float[][][][] logits)
                    || logits.length != 1 || logits[0].length < 19) {
                throw new IllegalStateException("얼굴 파싱 모델 출력 모양이 [1,19,H,W]가 아닙니다");
            }
            FaceParsingOutput parsing = FaceParsingOutput.from(resized, logits[0]);
            return metricCalculator.calculate(parsing);
        } catch (OrtException exception) {
            throw new IllegalStateException("ONNX 얼굴 파싱 추론에 실패했습니다", exception);
        }
    }

    private static BufferedImage decode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("분석할 이미지가 비어 있습니다");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new UnsupportedImageException();
            }
            return image;
        } catch (IOException exception) {
            throw new UnsupportedImageException(exception);
        }
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private static float[] normalizedChannelsFirst(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int plane = width * height;
        float[] input = new float[3 * plane];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int index = y * width + x;
                input[index] = normalize((rgb >>> 16) & 0xff, 0);
                input[plane + index] = normalize((rgb >>> 8) & 0xff, 1);
                input[2 * plane + index] = normalize(rgb & 0xff, 2);
            }
        }
        return input;
    }

    private static float normalize(int channel, int index) {
        return ((channel / 255.0f) - IMAGE_MEAN[index]) / IMAGE_STD[index];
    }

    @PreDestroy
    void close() throws OrtException {
        session.close();
    }

    /** ImageIO가 디코딩하지 못하는 HEIC 등은 작업 실패가 아니라 재촬영/변환 안내 대상이다. */
    static final class UnsupportedImageException extends RuntimeException {
        UnsupportedImageException() {
            super("JPEG 또는 PNG 이미지만 분석할 수 있습니다");
        }

        UnsupportedImageException(Throwable cause) {
            super("JPEG 또는 PNG 이미지만 분석할 수 있습니다", cause);
        }
    }
}
