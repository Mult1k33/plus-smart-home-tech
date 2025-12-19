package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.service.WarehouseService;
import ru.yandex.practicum.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.warehouse.AddressDto;
import ru.yandex.practicum.warehouse.BookedProductsDto;
import ru.yandex.practicum.warehouse.NewProductInWarehouseRequest;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseClient {

    private final WarehouseService warehouseService;

    @Override
    @PutMapping
    public void registerNewProduct(@Valid @RequestBody NewProductInWarehouseRequest request) {
        warehouseService.registerNewProductInWarehouse(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkAvailability(@Valid @RequestBody ShoppingCartDto cart) {
        return warehouseService.checkProductQuantityEnoughForShoppingCart(cart);
    }

    @Override
    @PostMapping("/add")
    public void receiveProduct(@Valid @RequestBody AddProductToWarehouseRequest request) {
        warehouseService.addProductToWarehouse(request);
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return warehouseService.getWarehouseAddress();
    }
}
