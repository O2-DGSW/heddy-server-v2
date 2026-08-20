package com.heddy.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI heddyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Heddy Customer API")
                .description("고객 중심 미용 시술기록·스타일 분석 API")
                .version("v1"));
    }

    /**
     * springdoc 은 Spring MVC 와 별개의 ObjectMapper 로 스키마를 만들기 때문에
     * {@code spring.jackson.property-naming-strategy} 를 보지 못한다.
     * 그대로 두면 실제 응답은 {@code request_id} 인데 문서에는 {@code requestId} 로 게시된다.
     *
     * <p>문서 자체(OpenAPI 문서 구조)의 직렬화까지 바뀌지 않도록 사본에만 전략을 걸어 모델 해석에만 쓴다.
     */
    @Bean
    ModelResolver snakeCaseModelResolver(ObjectMapperProvider objectMapperProvider) {
        ObjectMapper schemaMapper = objectMapperProvider.jsonMapper().copy();
        schemaMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new ModelResolver(schemaMapper);
    }
}
