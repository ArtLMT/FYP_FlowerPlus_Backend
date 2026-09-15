package com.lmt.fyp.flowerplus.module.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A request that matches no endpoint is the client's mistake and must not be
 * reported as a 500. Uses public /api/auth paths: on a protected path a
 * request without a token is a 401 before routing happens.
 */
class RequestErrorMappingTest extends AuthIntegrationSupport {

    @Test
    @DisplayName("an unknown path is a 404, not a 500")
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/auth/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("a wrong HTTP method is a 405 that lists the allowed methods")
    void wrongMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("a body that is not JSON is a 415, not a 500")
    void unsupportedContentTypeIs415() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
