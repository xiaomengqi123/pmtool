package com.pmtool;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class BusinessException extends RuntimeException {
    final int code; final HttpStatus status;
    BusinessException(int code, HttpStatus status, String message) { super(message); this.code = code; this.status = status; }
}

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.status(e.status).body(ApiResponse.fail(e.code, e.getMessage()));
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(40001, e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(50000, "系统繁忙，请稍后重试"));
    }
}

