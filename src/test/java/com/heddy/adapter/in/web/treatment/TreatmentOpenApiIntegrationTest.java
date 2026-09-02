package com.heddy.adapter.in.web.treatment;

import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TreatmentOpenApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;

    /**
     * 목록의 분석 상태는 스펙에서 열거형으로 나가야 한다. 문자열로만 나가면 클라이언트가
     * 허용값을 설명 문구에서 읽어 손으로 옮겨 적게 되고, 값이 늘거나 이름이 바뀌어도
     * 스펙은 그대로라 어긋난 것이 드러나지 않는다.
     *
     * <p>기대값을 열거형에서 직접 뽑는다. 여기에 여섯 개를 적어두면 이 테스트 자체가
     * 두 번째 목록이 되어 같은 문제를 반복한다.
     */
    @Test
    void publishesAnalysisStatusAsAnEnumeration() throws Exception {
        List<String> declared = Arrays.stream(AnalysisJobStatus.values()).map(Enum::name).toList();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.TreatmentRecordSummaryResponse"
                                + ".properties.analysis_status.enum",
                        containsInAnyOrder(declared.toArray())));
    }
}
