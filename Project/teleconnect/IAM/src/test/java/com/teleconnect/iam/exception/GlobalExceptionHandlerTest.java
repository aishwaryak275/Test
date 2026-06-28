package com.teleconnect.iam.exception;

import com.teleconnect.iam.dto.response.MessageDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies every handler in {@link GlobalExceptionHandler} — the single point
 * through which IAM exceptions become HTTP responses. All four @ExceptionHandler
 * methods are exercised here, including the per-status mapping of typed
 * {@link IamException}s and the 500 fallback for unexpected RuntimeExceptions.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("each IamException subclass maps to its own HTTP status + keeps its message")
    void handleIam_mapsStatusPerType() {
        assertIam(new ResourceNotFoundException("User not found"),
                HttpStatus.NOT_FOUND, "User not found");
        assertIam(new DuplicateResourceException("Email already in use"),
                HttpStatus.CONFLICT, "Email already in use");
        assertIam(new InvalidCredentialsException("Invalid credentials"),
                HttpStatus.UNAUTHORIZED, "Invalid credentials");
        assertIam(new AccountNotActiveException("Account is not active"),
                HttpStatus.FORBIDDEN, "Account is not active");
        assertIam(new InvalidRequestException("Invalid status: BOGUS"),
                HttpStatus.BAD_REQUEST, "Invalid status: BOGUS");
        assertIam(new AuthenticationRequiredException("Not authenticated"),
                HttpStatus.UNAUTHORIZED, "Not authenticated");
    }

    private void assertIam(IamException ex, HttpStatus expectedStatus, String expectedMessage) {
        ResponseEntity<MessageDTO> response = handler.handleIam(ex);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN with a fixed message")
    void handleAccessDenied_returnsForbidden() {
        ResponseEntity<MessageDTO> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Access denied - insufficient permissions");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 with the first field error")
    void handleValidation_returnsFirstFieldError() {
        FieldError fieldError = new FieldError("registerRequest", "email", "Invalid email format");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<MessageDTO> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("email: Invalid email format");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException with no field errors -> default message")
    void handleValidation_withNoFieldErrors_returnsDefaultMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<MessageDTO> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("unexpected (non-IAM) RuntimeException -> 500 INTERNAL_SERVER_ERROR")
    void handleRuntime_returnsInternalServerError() {
        ResponseEntity<MessageDTO> response =
                handler.handleRuntime(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
    }
}
