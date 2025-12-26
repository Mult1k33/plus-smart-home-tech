package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ShoppingCartErrorHandler {

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoProductsInShoppingCart(NoProductsInShoppingCartException e) {
        log.warn("No products in shopping cart: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("No products found in cart")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(NotAuthorizedUserException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleNotAuthorizedUser(NotAuthorizedUserException e) {
        log.warn("User not authorized: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("UNAUTHORIZED")
                .message("User not authorized")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(NotFoundCartException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCartNotFound(NotFoundCartException e) {
        log.warn("Cart not found: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("CART_NOT_FOUND")
                .message("Shopping cart not found")
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEmptyBody(HttpMessageNotReadableException e) {
        log.warn("Empty or invalid request body: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Request body is required")
                .detail("Please provide a valid JSON array of product IDs")
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
