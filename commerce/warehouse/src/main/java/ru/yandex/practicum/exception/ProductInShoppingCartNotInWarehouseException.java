package ru.yandex.practicum.exception;

public class ProductInShoppingCartNotInWarehouseException extends RuntimeException {
  public ProductInShoppingCartNotInWarehouseException(String message) {
    super(message);
  }
}
