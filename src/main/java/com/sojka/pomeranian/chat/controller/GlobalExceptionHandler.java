package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.lib.BaseExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@Order(Integer.MAX_VALUE)
@ControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return super.handleException(e);
    }

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(BindException e) {
        return super.handleValidationException(e);
    }

    @Override
    @ExceptionHandler(exception = {AuthorizationDeniedException.class})
    protected ResponseEntity<ProblemDetail> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return super.handleAuthorizationDeniedException(e);
    }

    @Override
    protected void logInput(Exception e) {
        super.logInput(e);
    }
}