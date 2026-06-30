package com.teleConnect.billing.inovice.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @RestController @RequestMapping("/test")
    static class TestController {
        @GetMapping("/business") void business() { throw new BusinessRuleException("rule violated"); }
        @GetMapping("/notfound") void notFound() { throw new ResourceNotFoundException("item not found"); }
        @GetMapping("/duplicate") void duplicate() { throw new DuplicateResourceException("duplicate key"); }
        @GetMapping("/general") void general() throws Exception { throw new Exception("unexpected error"); }
        @PostMapping("/validation") @ResponseBody
        String validation(@Valid @RequestBody ValidatedRequest body) { return "ok"; }
        record ValidatedRequest(@NotBlank(message = "name must not be blank") String name) {}
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test @DisplayName("BusinessRuleException — HTTP 400")
    void businessRule_400() throws Exception {
        mockMvc.perform(get("/test/business")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("BusinessRuleException — statusCode 400 in body")
    void businessRule_bodyStatusCode() throws Exception {
        mockMvc.perform(get("/test/business")).andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test @DisplayName("BusinessRuleException — message in body")
    void businessRule_bodyMessage() throws Exception {
        mockMvc.perform(get("/test/business")).andExpect(jsonPath("$.message").value("rule violated"));
    }

    @Test @DisplayName("ResourceNotFoundException — HTTP 404")
    void notFound_404() throws Exception {
        mockMvc.perform(get("/test/notfound")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("ResourceNotFoundException — statusCode 404 in body")
    void notFound_bodyStatusCode() throws Exception {
        mockMvc.perform(get("/test/notfound")).andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test @DisplayName("ResourceNotFoundException — message in body")
    void notFound_bodyMessage() throws Exception {
        mockMvc.perform(get("/test/notfound")).andExpect(jsonPath("$.message").value("item not found"));
    }

    @Test @DisplayName("DuplicateResourceException — HTTP 409")
    void duplicate_409() throws Exception {
        mockMvc.perform(get("/test/duplicate")).andExpect(status().isConflict());
    }

    @Test @DisplayName("DuplicateResourceException — statusCode 409 in body")
    void duplicate_bodyStatusCode() throws Exception {
        mockMvc.perform(get("/test/duplicate")).andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test @DisplayName("DuplicateResourceException — message in body")
    void duplicate_bodyMessage() throws Exception {
        mockMvc.perform(get("/test/duplicate")).andExpect(jsonPath("$.message").value("duplicate key"));
    }

    @Test @DisplayName("RuntimeException — HTTP 500")
    void general_500() throws Exception {
        mockMvc.perform(get("/test/general")).andExpect(status().isInternalServerError());
    }

    @Test @DisplayName("RuntimeException — statusCode 500 in body")
    void general_bodyStatusCode() throws Exception {
        mockMvc.perform(get("/test/general")).andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test @DisplayName("Validation failure — HTTP 400")
    void validation_400() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Validation failure — statusCode 400 in body")
    void validation_bodyStatusCode() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")).andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test @DisplayName("Validation failure — message in body")
    void validation_bodyMessage() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")).andExpect(jsonPath("$.message").exists());
    }
}
