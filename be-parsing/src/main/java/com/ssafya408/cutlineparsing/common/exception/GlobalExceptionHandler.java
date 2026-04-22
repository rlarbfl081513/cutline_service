package com.ssafya408.cutlineparsing.common.exception;

import com.ssafya408.cutlineparsing.common.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String GENERIC_MESSAGE = "요청을 처리하는 중 오류가 발생했습니다.";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = Optional.ofNullable(HttpStatus.resolve(ex.getStatusCode().value()))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        String message = Optional.ofNullable(ex.getReason()).filter(s -> !s.isBlank()).orElse(GENERIC_MESSAGE);
        log.warn("[ResponseStatusException] status={}, message={}", status.value(), message, ex);
        return buildResponse(status, message);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        String message = Optional.ofNullable(ex.getMessage()).filter(s -> !s.isBlank()).orElse("요청한 대상을 찾을 수 없습니다.");
        log.warn("[EntityNotFoundException] message={}", message, ex);
        return buildResponse(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = Optional.ofNullable(ex.getMessage()).filter(s -> !s.isBlank()).orElse(GENERIC_MESSAGE);
        log.warn("[IllegalArgumentException] message={}", message, ex);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<String>> handleIOException(IOException ex) {
        String message = Optional.ofNullable(ex.getMessage()).filter(s -> !s.isBlank()).orElse("파일을 처리하는 중 문제가 발생했습니다.");
        log.error("[IOException] message={}", message, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        log.error("[UnhandledException]", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_MESSAGE);
    }

    private ResponseEntity<ApiResponse<String>> buildResponse(HttpStatusCode status, String message) {
        String effectiveMessage = (message == null || message.isBlank()) ? GENERIC_MESSAGE : message;
        return ResponseEntity.status(status).body(ApiResponse.errorWithMessage(effectiveMessage));
    }
}