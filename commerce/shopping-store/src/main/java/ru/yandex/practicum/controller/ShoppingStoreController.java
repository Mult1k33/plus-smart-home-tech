package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.service.ShoppingStoreService;
import ru.yandex.practicum.store.ProductDto;
import ru.yandex.practicum.store.SetProductQuantityStateRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ShoppingStoreController implements ShoppingStoreClient {

    private final ShoppingStoreService shoppingStoreService;

    @Override
    @GetMapping
    public Page<ProductDto> getProducts(@RequestParam(name = "category") ProductCategory category,
                                          Pageable pageable) {
        return shoppingStoreService.getProductsByCategory(category, pageable);
    }

    @Override
    @PutMapping
    public ProductDto createProduct(@RequestBody ProductDto productDto) {
        return shoppingStoreService.createProduct(productDto);
    }

    @Override
    @PostMapping
    public ProductDto updateProduct(@RequestBody ProductDto productDto) {
        return shoppingStoreService.updateProduct(productDto);
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public boolean removeProduct(@RequestBody UUID productId) {
        return shoppingStoreService.removeProductFromStore(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public boolean updateQuantity(SetProductQuantityStateRequest request) {
        return shoppingStoreService.setProductQuantityState(request);
    }

    @Override
    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return shoppingStoreService.getProduct(productId);
    }
}
