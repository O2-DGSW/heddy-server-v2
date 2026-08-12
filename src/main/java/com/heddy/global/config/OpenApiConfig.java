package com.heddy.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
}
