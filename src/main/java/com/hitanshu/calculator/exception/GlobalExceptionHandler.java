package com.hitanshu.calculator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hitanshu.calculator.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns bad input into a structured HTTP 400 JSON body instead of Spring's default error page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ?a=5 (b missing) or ?a=5&b= (b empty)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex,
                                                HttpServletRequest request) {
        return ErrorResponse.badRequest(
                "Required parameter '" + ex.getParameterName() + "' is missing", request.getRequestURI());
    }

    // ?a=abc&b=2
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ErrorResponse.badRequest(
                "Parameter '" + ex.getName() + "' must be a number but was '" + ex.getValue() + "'",
                request.getRequestURI());
    }

    // divide by zero
    @ExceptionHandler(ArithmeticException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArithmetic(ArithmeticException ex, HttpServletRequest request) {
        return ErrorResponse.badRequest(ex.getMessage(), request.getRequestURI());
    }
}
