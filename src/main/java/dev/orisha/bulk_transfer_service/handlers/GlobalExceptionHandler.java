package dev.orisha.bulk_transfer_service.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.RejectedExecutionException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<String> handleRejectedExecutionException(RejectedExecutionException e, HttpServletRequest request) {
        log.error("Task was rejected in request path {} {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Server is overloaded. Please try again later.");
    }
}
