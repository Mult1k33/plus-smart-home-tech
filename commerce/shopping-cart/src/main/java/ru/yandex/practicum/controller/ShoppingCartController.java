package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.client.ShoppingCartClient;
import ru.yandex.practicum.service.ShoppingCartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController implements ShoppingCartClient {

    private final ShoppingCartService shoppingCartService;

    @Override
    @GetMapping
    public ShoppingCartDto getCart(@RequestParam(name = "username") String username) {
        return shoppingCartService.getCart(username);
    }

    @Override
    @PutMapping
    public ShoppingCartDto addProduct(@RequestParam(name = "username") String username,
                                      @RequestBody Map<UUID, Long> products) {
        return shoppingCartService.addProducts(username, products);
    }

    @Override
    @DeleteMapping
    public void deactivateCart(@RequestParam(name = "username") String username) {
        shoppingCartService.deactivate(username);
    }

    @Override
    @PostMapping("/remove")
    public ShoppingCartDto removeProducts(@RequestParam(name = "username") String username,
                                          @RequestBody List<UUID> products) {
        return shoppingCartService.removeProducts(username, products);
    }

    @PostMapping("/change-quantity")
    public ShoppingCartDto changeQuantity(@RequestParam(name = "username") String username,
                                          @RequestBody ChangeProductQuantityRequest request) {
        return shoppingCartService.changeQuantity(username, request);
    }
}
