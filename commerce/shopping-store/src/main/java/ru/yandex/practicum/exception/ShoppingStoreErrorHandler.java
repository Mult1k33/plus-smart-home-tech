package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ShoppingStoreErrorHandler {

    @ExceptionHandler(NotFoundProductException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundProduct(NotFoundProductException e) {
        log.warn("Product not found: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("PRODUCT_NOT_FOUND")
                .message("Product not found")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Invalid request parameter")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalState(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Invalid operation")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("Internal server error")
                .detail(e.getMessage())
                .build();
    }
}
