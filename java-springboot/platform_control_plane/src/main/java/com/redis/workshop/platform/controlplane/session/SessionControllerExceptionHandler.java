package com.redis.workshop.platform.controlplane.session;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SessionController.class)
public class SessionControllerExceptionHandler {

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<ProblemDetail> handleErrorResponseException(ErrorResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(exception.getBody());
    }
}
