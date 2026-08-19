package com.heddy.global.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderAbsent() throws Exception {
        MockHttpServletResponse response = doFilter(new MockHttpServletRequest());

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    void reusesClientHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "client-request-1");

        assertThat(doFilter(request).getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-1");
    }

    @Test
    void rejectsUnsafeHeaderValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "bad value\nInjected: 1");

        assertThat(doFilter(request).getHeader(RequestIdFilter.REQUEST_ID_HEADER)).doesNotContain("Injected");
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        doFilter(new MockHttpServletRequest());

        assertThat(RequestIdFilter.currentRequestId()).isNull();
    }

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
