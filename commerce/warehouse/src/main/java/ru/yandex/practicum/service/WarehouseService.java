package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.model.ProductStock;
import ru.yandex.practicum.repository.WarehouseProductStockRepository;
import ru.yandex.practicum.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.warehouse.AddressDto;
import ru.yandex.practicum.warehouse.BookedProductsDto;
import ru.yandex.practicum.warehouse.NewProductInWarehouseRequest;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseProductStockRepository repository;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};

    private static final String CURRENT_ADDRESS =
            ADDRESSES[Random.from(new SecureRandom()).nextInt(0, ADDRESSES.length)];

    /**
     * Добавить новый товар на склад.
     */
    @Transactional
    public void registerNewProductInWarehouse(NewProductInWarehouseRequest request) {
        if (repository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Product with ID " + request.getProductId() + " already exists in warehouse");
        }

        ProductStock newProduct = ProductStock.builder()
                .productId(request.getProductId())
                .fragile(request.getFragile() != null ? request.getFragile() : false)
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .quantity(0L)
                .build();

        repository.save(newProduct);
        log.info("Added new product to warehouse: {} with dimensions: {}x{}x{}, weight: {}, fragile: {}",
                request.getProductId(),
                request.getDimension().getWidth(),
                request.getDimension().getHeight(),
                request.getDimension().getDepth(),
                request.getWeight(),
                request.getFragile());
    }

    /**
     * Проверка, что количество товаров на складе достаточно для данной корзины.
     */
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        Map<UUID, Long> items = cart.getProducts();

        if (items == null || items.isEmpty()) {
            log.debug("Shopping cart {} is empty", cart.getShoppingCartId());
            return BookedProductsDto.builder()
                    .deliveryWeight(0.0)
                    .deliveryVolume(0.0)
                    .fragile(false)
                    .build();
        }

        // Получаем все необходимые товары одним запросом для оптимизации
        Set<UUID> productIds = items.keySet();
        List<ProductStock> stocks = repository.findAllById(productIds);

        // Создаем Map для быстрого поиска по ID товара
        Map<UUID, ProductStock> stockMap = new HashMap<>();
        for (ProductStock stock : stocks) {
            stockMap.put(stock.getProductId(), stock);
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean anyFragile = false;
        Map<UUID, Long> missingProducts = new HashMap<>();

        // Проверяем наличие всех товаров
        for (Map.Entry<UUID, Long> entry : items.entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue() == null ? 0L : entry.getValue();

            ProductStock stock = stockMap.get(productId);
            if (stock == null) {
                throw new NoSpecifiedProductInWarehouseException(
                        "Product with ID " + productId + " not found in warehouse");
            }

            if (stock.getQuantity() < requestedQuantity) {
                long missing = requestedQuantity - stock.getQuantity();
                missingProducts.put(productId, missing);
            }
        }

        // Если есть недостающие товары - бросаем исключение
        if (!missingProducts.isEmpty()) {
            String errorMessage = buildMissingProductsMessage(missingProducts);
            log.warn("Insufficient stock for shopping cart {}: {}",
                    cart.getShoppingCartId(), errorMessage);
            throw new ProductInShoppingCartLowQuantityInWarehouseException(errorMessage);
        }

        // Если все товары в наличии - рассчитываем характеристики доставки
        for (Map.Entry<UUID, Long> entry : items.entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue();

            ProductStock stock = stockMap.get(productId);

            double itemVolume = stock.volume();
            totalVolume += itemVolume * requestedQuantity;
            totalWeight += stock.getWeight() * requestedQuantity;

            if (Boolean.TRUE.equals(stock.getFragile())) {
                anyFragile = true;
            }
        }

        BookedProductsDto result = BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(anyFragile)
                .build();

        log.info("Checked availability for shopping cart {}: weight={}, volume={}, fragile={}",
                cart.getShoppingCartId(), totalWeight, totalVolume, anyFragile);

        return result;
    }

    /**
     * Принять товар на склад (увеличить количество).
     */
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductStock stock = repository.findById(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Product with ID " + request.getProductId() + " not found in warehouse"));

        Long newQuantity = stock.getQuantity() + request.getQuantity();
        stock.setQuantity(newQuantity);

        repository.save(stock);

        log.info("Added {} units to product {} in warehouse. New quantity: {}",
                request.getQuantity(), request.getProductId(), newQuantity);
    }

    /**
     * Предоставить адрес склада для расчёта доставки.
     */
    @Transactional(readOnly = true)
    public AddressDto getWarehouseAddress() {
        AddressDto address = AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();

        log.info("Warehouse address: {}", CURRENT_ADDRESS);
        return address;
    }

    /**
     * Формирует информативное сообщение о недостающих товарах для исключения.
     */
    private String buildMissingProductsMessage(Map<UUID, Long> missingProducts) {
        if (missingProducts == null || missingProducts.isEmpty()) {
            return "Product(s) not available in required quantity in warehouse";
        }

        return missingProducts.entrySet().stream()
                .map(e -> String.format("Product %s — missing %d units", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }
}
