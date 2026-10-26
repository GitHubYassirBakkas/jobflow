package com.yassir.jobflow.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        Map<String, String> error = new HashMap<>();

        error.put("message", exception.getMessage());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<Map<String, String>> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception) {

    Map<String, String> error = new HashMap<>();

    error.put(
            "message",
            "Invalid value for parameter: " + exception.getName()
    );

    return ResponseEntity.badRequest().body(error);
}

@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<Map<String, String>> handleResourceNotFoundException(
        ResourceNotFoundException exception) {

    Map<String, String> error = new HashMap<>();

    error.put("message", exception.getMessage());

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
}

@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<Map<String, String>> handleForbiddenException(
        ForbiddenException exception) {

    Map<String, String> error = new HashMap<>();

    error.put("message", exception.getMessage());

    return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(error);
}

@ExceptionHandler(BusinessConflictException.class)
public ResponseEntity<Map<String, String>> handleBusinessConflictException(
        BusinessConflictException exception) {

    Map<String, String> error = new HashMap<>();

    error.put("message", exception.getMessage());

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(error);
}
}
