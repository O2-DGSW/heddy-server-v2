package com.heddy.health;

import com.heddy.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    ApiResponse<HealthResponse> health() {
        return ApiResponse.of(new HealthResponse("UP", Instant.now()));
    }

    record HealthResponse(String status, Instant timestamp) {
    }
}
