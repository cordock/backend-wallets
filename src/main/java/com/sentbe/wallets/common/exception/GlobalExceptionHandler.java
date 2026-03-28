package com.sentbe.wallets.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.sentbe.wallets.common.response.CustomResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponse<>(
            HttpStatus.BAD_REQUEST,
            ResponseCode.INVALID_INPUT_VALUE.getCode(),
            ResponseCode.INVALID_INPUT_VALUE.getMessage(),
            errors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CustomResponse<Map<String, String>>> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        Map<String, String> errors = new HashMap<>();
        exception.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });

        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponse<>(
            HttpStatus.BAD_REQUEST,
            ResponseCode.INVALID_INPUT_VALUE.getCode(),
            ResponseCode.INVALID_INPUT_VALUE.getMessage(),
            errors
        ));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CustomResponse<?>> handleBusinessException(BusinessException exception) {
        ResponseCode responseCode = exception.getResponseCode();
        int code = responseCode.getCode();
        String message = exception.getMessage();
        log.error("CustomException occurred - code: {}, message: {}", code, message);

        return ResponseEntity
            .status(responseCode.getHttpStatus())
            .body(new CustomResponse<>(responseCode.getHttpStatus(), code, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Map<String, Object>>> handleException(Exception exception) {
        log.error("Unexpected error occurred", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CustomResponse<>(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ResponseCode.INTERNAL_ERROR.getCode(),
            ResponseCode.INTERNAL_ERROR.getMessage()
        ));
    }
}
