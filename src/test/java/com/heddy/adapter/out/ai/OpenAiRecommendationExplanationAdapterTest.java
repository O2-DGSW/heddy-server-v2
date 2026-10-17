package com.heddy.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.recommendation.model.*;
import com.heddy.domain.recommendation.service.RuleBasedV1Scorer;
import com.heddy.domain.treatment.model.ServiceType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiRecommendationExplanationAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> received = new AtomicReference<>();
    private HttpServer server;
    private int status = 200;
    private String body;
    private long delayMs;

    @BeforeEach
    void startServer() throws Exception {
        body = completed("{\"items\":[{\"rank\":1,\"message\":\"저장해 둔 스타일로, 하루 관리 시간도 설정한 범위 안에 들어요.\"}]}");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            calls.incrementAndGet();
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() { server.stop(0); }

    @Test
    void sendsOnlyCompactFactsAndAcceptsStructuredOutput() throws Exception {
        var candidate = candidate(1);
        var context = context(candidate.hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(List.of(candidate), context);
        var result = adapter(true, "test-key", Duration.ofSeconds(2)).explain(selected, context);
        assertThat(result).containsKey(candidate.hairstyleId());
        JsonNode request = mapper.readTree(received.get());
        assertThat(request.path("store").asBoolean()).isFalse();
        assertThat(request.path("max_output_tokens").asInt()).isEqualTo(384);
        assertThat(request.at("/text/format/strict").asBoolean()).isTrue();
        JsonNode facts = mapper.readTree(request.path("input").asText()).get(0).path("facts");
        assertThat(facts.path("saved_style").asBoolean()).isTrue();
        assertThat(facts.at("/care_minutes/required").asInt()).isEqualTo(5);
        assertThat(facts.at("/matched_hair_attributes/length").asText()).isEqualTo("SHORT");
        assertThat(facts.at("/matched_hair_attributes/condition").isMissingNode()).isTrue();
        assertThat(received.get()).doesNotContain(candidate.hairstyleId().toString(),
                context.hairProfile().userId().toString(), "test-key");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void batchesThreeItemsInOneCallAndUsesReturnedRanks() throws Exception {
        body = completed("""
                {"items":[{"rank":3,"message":"하루 관리 시간이 설정한 범위 안에 들어요."},
                {"rank":1,"message":"저장해 둔 스타일을 다시 살펴보세요."},
                {"rank":2,"message":"현재 등록한 짧은 모발 길이와 맞는 스타일이에요."}]}
                """);
        var candidates = List.of(candidate(1), candidate(2), candidate(3));
        var context = context(candidates.getFirst().hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(candidates, context);
        var result = adapter(true, "test-key", Duration.ofSeconds(2)).explain(selected, context);
        assertThat(result).hasSize(3);
        assertThat(result.get(selected.getFirst().candidate().hairstyleId())).contains("저장해 둔");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(mapper.readTree(mapper.readTree(received.get()).path("input").asText()).size()).isEqualTo(3);
    }

    @Test
    void disabledOrMissingKeyDoesNotCallApi() {
        var candidate = candidate(1);
        var context = context(candidate.hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(List.of(candidate), context);
        assertThat(adapter(false, "test-key", Duration.ofSeconds(1)).explain(selected, context)).isEmpty();
        assertThat(adapter(true, "", Duration.ofSeconds(1)).explain(selected, context)).isEmpty();
        assertThat(calls.get()).isZero();
    }

    @Test
    void httpFailureDoesNotRetry() {
        status = 429;
        body = "sensitive upstream error must not be logged";
        assertFallback();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void rejectsIncompleteRefusedMalformedAndInvalidMessages() throws Exception {
        for (String invalid : List.of(
                "{\"status\":\"incomplete\"}", "not json",
                "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}",
                completed("{\"items\":[]}"),
                completed("{\"items\":[{\"rank\":2,\"message\":\"다른 후보예요\"}]}"),
                completed("{\"items\":[{\"rank\":1,\"message\":\" \"}]}"),
                completed("{\"items\":[{\"rank\":1,\"message\":\"English only\"}]}"),
                completed("{\"items\":[{\"rank\":1,\"message\":\"" + "가".repeat(91) + "\"}]}"))) {
            body = invalid;
            assertFallback();
        }
    }

    @Test
    void duplicateRanksDiscardWholeResponse() throws Exception {
        body = completed("{\"items\":[{\"rank\":1,\"message\":\"저장한 스타일이에요.\"},{\"rank\":1,\"message\":\"중복된 스타일이에요.\"}]}");
        var context = context(candidate(1).hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(List.of(candidate(1), candidate(2)), context);
        assertThat(adapter(true, "test-key", Duration.ofSeconds(2)).explain(selected, context)).isEmpty();
    }

    @Test
    void enforcesTotalDeadline() {
        delayMs = 500;
        var candidate = candidate(1);
        var context = context(candidate.hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(List.of(candidate), context);
        long started = System.nanoTime();
        assertThat(adapter(true, "test-key", Duration.ofMillis(100)).explain(selected, context)).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_SMOKE_TEST", matches = "true")
    void liveSmokeWithSyntheticFactsOnly() {
        var candidates = List.of(candidate(1), candidate(2), candidate(3));
        var context = context(candidates.getFirst().hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(candidates, context);
        var adapter = new OpenAiRecommendationExplanationAdapter(System.getenv("OPENAI_API_KEY"),
                "gpt-4.1-nano-2025-04-14", true, 8000);
        var result = adapter.explain(selected, context);
        assertThat(result).hasSize(3);
        result.values().forEach(message -> System.out.println("Synthetic explanation: " + message));
    }

    private void assertFallback() {
        var candidate = candidate(1);
        var context = context(candidate.hairstyleId());
        var selected = new RuleBasedV1Scorer().scoreAll(List.of(candidate), context);
        assertThat(adapter(true, "test-key", Duration.ofSeconds(2)).explain(selected, context)).isEmpty();
    }

    private OpenAiRecommendationExplanationAdapter adapter(boolean enabled, String key, Duration timeout) {
        return new OpenAiRecommendationExplanationAdapter(mapper, HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/responses"),
                key, "gpt-4.1-nano-2025-04-14", enabled, timeout);
    }

    private String completed(String json) throws Exception {
        return mapper.writeValueAsString(Map.of("status", "completed", "output", List.of(
                Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", json))))));
    }

    private RecommendationContext context(UUID savedId) {
        var profile = HairProfile.create(UUID.randomUUID(), HairProfile.HairType.STRAIGHT,
                HairProfile.HairCondition.DAMAGED, HairProfile.HairLength.SHORT,
                HairProfile.HairThickness.NORMAL, 10);
        return new RecommendationContext(profile, Set.of(), Set.of(), Set.of(savedId), List.of(), Instant.now());
    }

    private HairstyleCandidate candidate(int index) {
        return new HairstyleCandidate(new UUID(0, index), "테스트 스타일 " + index, "SHORT", UUID.randomUUID(),
                "BOTH", true, "1", Set.of(ServiceType.CUT), Set.of(HairProfile.HairLength.SHORT),
                Set.of(HairProfile.HairType.STRAIGHT), Set.of(HairProfile.HairThickness.NORMAL),
                Set.of(HairProfile.HairCondition.HEALTHY), Set.of(), HairProfile.HairLength.SHORT,
                5, HairstyleCandidate.ManagementDifficulty.LOW, HairstyleCandidate.ChemicalStressLevel.LOW,
                index, "1", Map.of());
    }
}
