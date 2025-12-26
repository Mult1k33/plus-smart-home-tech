package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.store.ProductDto;
import ru.yandex.practicum.store.SetProductQuantityStateRequest;

import java.util.UUID;

@FeignClient(name = "shopping-store", path = "/api/v1/shopping-store")
public interface ShoppingStoreClient {

    @GetMapping
    Page<ProductDto> getProducts(@RequestParam(name = "category") ProductCategory category, Pageable pageable);

    @PutMapping
    ProductDto createProduct(@RequestBody ProductDto productDto);

    @PostMapping
    ProductDto updateProduct(@RequestBody ProductDto productDto);

    @PostMapping("/removeProductFromStore")
    boolean removeProduct(@RequestBody UUID productId);

    @PostMapping("/quantityState")
    boolean updateQuantity(SetProductQuantityStateRequest request);

    @GetMapping("/{productId}")
    ProductDto getProduct(@PathVariable UUID productId);
}
