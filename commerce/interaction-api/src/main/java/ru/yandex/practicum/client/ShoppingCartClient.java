package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cart.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "shopping-cart", path = "/api/v1/shopping-cart")
public interface ShoppingCartClient {

    @GetMapping
    ShoppingCartDto getCart(@RequestParam(name = "username") String username);

    @PutMapping
    ShoppingCartDto addProduct(@RequestParam(name = "username") String username,
                               @RequestBody Map<UUID, Long> products);

    @DeleteMapping
    void deactivateCart(@RequestParam(name = "username") String username);

    @PostMapping("/remove")
    ShoppingCartDto removeProducts(@RequestParam(name = "username") String username,
                                   @RequestBody List<UUID> products);

    @PostMapping("/change-quantity")
    ShoppingCartDto changeQuantity(@RequestParam(name = "username") String username,
                                   @RequestBody ChangeProductQuantityRequest request);
}
