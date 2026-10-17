package com.heddy.adapter.out.ai;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HairAnalysisEnabledContextTest {
    @TempDir Path directory;

    @Test
    void enabledEngineStartsWithBootsAutoConfiguredJacksonMapper() throws Exception {
        Path model = Files.write(directory.resolve("model.onnx"), new byte[]{1});
        OrtEnvironment environment = mock(OrtEnvironment.class);
        OrtSession session = mock(OrtSession.class);
        when(environment.createSession(anyString(), any(OrtSession.SessionOptions.class)))
                .thenReturn(session);
        when(session.getInputNames()).thenReturn(Set.of("pixel_values"));

        // Only the native model is stubbed: Boot's real Jackson auto-configuration must wire the bean.
        try (var nativeEnvironment = mockStatic(OrtEnvironment.class);
             var options = mockConstruction(OrtSession.SessionOptions.class)) {
            nativeEnvironment.when(OrtEnvironment::getEnvironment).thenReturn(environment);
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                    .withUserConfiguration(OnnxFaceParsingHairAnalysisAdapter.class)
                    .withPropertyValues("app.ai.enabled=true", "app.ai.model-path=" + model,
                            "app.ai.model-version=context-test", "app.ai.input-size=320",
                            "app.ai.minimum-confidence-score=50")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(ObjectMapper.class);
                        assertThat(context).hasSingleBean(OnnxFaceParsingHairAnalysisAdapter.class);
                        assertThat(context.getBean(OnnxFaceParsingHairAnalysisAdapter.class).isReady()).isTrue();
                    });
        }
        verify(session).close();
    }
}
