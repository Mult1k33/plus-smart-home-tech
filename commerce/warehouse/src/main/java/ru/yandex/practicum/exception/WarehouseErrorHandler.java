package ru.yandex.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class WarehouseErrorHandler {

    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoSpecifiedProduct(NoSpecifiedProductInWarehouseException e) {
        log.warn("Product not found in warehouse");
        return ErrorResponse.builder()
                .error("NO_PRODUCT_IN_WAREHOUSE")
                .message("Product not found in warehouse")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(ProductInShoppingCartNotInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleProductNotInWarehouse(ProductInShoppingCartNotInWarehouseException e) {
        log.warn("Product not in warehouse: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("PRODUCT_NOT_IN_WAREHOUSE")
                .message("Product not in warehouse")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLowQuantity(ProductInShoppingCartLowQuantityInWarehouseException e) {
        log.warn("Insufficient stock in warehouse: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("INSUFFICIENT_STOCK")
                .message("Insufficient stock in warehouse")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleProductAlreadyExists(SpecifiedProductAlreadyInWarehouseException e) {
        log.warn("Product already exists in warehouse: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("PRODUCT_ALREADY_EXISTS")
                .message("Product already exists in warehouse")
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
}
