package com.heddy.adapter.out.ai;

import ai.onnxruntime.*;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.recommendation.model.PhotoHairEvidence;
import com.heddy.domain.recommendation.port.out.PhotoHairLengthPort;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.photo-length.enabled", havingValue = "true")
public class OnnxPhotoHairLengthAdapter implements PhotoHairLengthPort {
    public static final String MODEL_VERSION = "selfie-multiclass-v1-shape-v2";
    private static final String MODEL_SHA256 = "35ec1ecd9ee7f85073c99c00020b7f6751b69506eeacf683bc8665f6117f85b0";
    private static final Logger log = LoggerFactory.getLogger(OnnxPhotoHairLengthAdapter.class);
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final OrtSession session;
    private final FileStoragePort storage;
    private final PhotoHairLengthEstimator estimator = new PhotoHairLengthEstimator();
    // Only observations are retained, never image bytes. Ownership is checked before every cache lookup.
    private final Map<String, PhotoHairEvidence> cache = new LinkedHashMap<>(256, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, PhotoHairEvidence> entry) { return size() > 256; }
    };

    public OnnxPhotoHairLengthAdapter(FileStoragePort storage,
            @Value("${app.photo-length.model-path:/opt/heddy/photo-models/selfie.onnx}") String path) {
        this.storage = storage;
        try {
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(Path.of(path))));
            if (!MODEL_SHA256.equals(checksum)) throw new IllegalStateException("Photo length model checksum mismatch");
            try (var options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(1);
                options.setInterOpNumThreads(1);
                session = environment.createSession(path, options);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load pinned photo length model", failure);
        }
    }

    @Override
    public synchronized PhotoHairEvidence analyze(StoredFile file) {
        String key = file.userId() + ":" + file.fileId() + ":" + file.sha256();
        PhotoHairEvidence previous = cache.get(key);
        if (previous != null) return previous;
        try {
            byte[] bytes = storage.readObject(file, 10 * 1024 * 1024L);
            BufferedImage source = decode(bytes);
            if (source == null) return evidence(file, "UNREADABLE", null);
            BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try { graphics.drawImage(source, 0, 0, 256, 256, null); } finally { graphics.dispose(); }
            float[] rgb = new float[256 * 256 * 3];
            for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                int color = image.getRGB(x, y), offset = (y * 256 + x) * 3;
                rgb[offset] = (color >> 16 & 255) / 255f;
                rgb[offset + 1] = (color >> 8 & 255) / 255f;
                rgb[offset + 2] = (color & 255) / 255f;
            }
            try (var tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(rgb), new long[]{1, 256, 256, 3});
                 var result = session.run(Map.of("input_29", tensor))) {
                float[][][] logits = ((float[][][][]) result.get(0).getValue())[0];
                var length = estimator.estimate(logits);
                var observation = new PhotoHairEvidence(file.fileId(), MODEL_VERSION,
                        length == null ? "UNCERTAIN" : "ASSESSED", length, false,
                        length == null ? java.util.List.of() : estimator.shape(logits), 0);
                cache.put(key, observation);
                return observation;
            }
        } catch (Exception failure) {
            // Storage failures are transient: do not cache them or log URLs, object keys or image data.
            log.warn("Photo length analysis unavailable ({})", failure.getClass().getSimpleName());
            return evidence(file, "UNAVAILABLE", null);
        }
    }

    private PhotoHairEvidence evidence(StoredFile file, String status,
            com.heddy.domain.account.model.HairProfile.HairLength length) {
        return new PhotoHairEvidence(file.fileId(), MODEL_VERSION, status, length, false);
    }

    private static BufferedImage decode(byte[] bytes) throws java.io.IOException {
        // Inspect dimensions before allocating a decoded image (upload byte limits alone are insufficient).
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 64 || h < 64 || (long) w * h > 20_000_000L) return null;
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }
    @PreDestroy
    public void close() throws OrtException { session.close(); }
}
