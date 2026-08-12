package com.rally.catalog.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CatalogExceptionHandlerTest {

    @RestController
    static class StubController {
        @PostMapping("/test/body")
        public void body(@RequestBody Map<String, Object> payload) {
        }

        @GetMapping("/test/path/{id}")
        public void path(@PathVariable UUID id) {
        }

        @PostMapping("/test/integrity")
        public void integrity(@RequestBody Map<String, Object> payload) {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }
    }

    @Test
    void malformedJsonBodyIs400() throws Exception {
        standaloneSetup(new StubController())
                .setControllerAdvice(new CatalogExceptionHandler())
                .build()
                .perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is not valid JSON"));
    }

    @Test
    void invalidUuidPathVariableIs400() throws Exception {
        standaloneSetup(new StubController())
                .setControllerAdvice(new CatalogExceptionHandler())
                .build()
                .perform(get("/test/path/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Parameter"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void dataIntegrityViolationIs400() throws Exception {
        standaloneSetup(new StubController())
                .setControllerAdvice(new CatalogExceptionHandler())
                .build()
                .perform(post("/test/integrity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Data Integrity Violation"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
