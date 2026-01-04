package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.model.OrderBooking;
import ru.yandex.practicum.model.ProductStock;
import ru.yandex.practicum.repository.OrderBookingRepository;
import ru.yandex.practicum.repository.WarehouseProductStockRepository;
import ru.yandex.practicum.warehouse.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseProductStockRepository repository;
    private final OrderBookingRepository bookingRepository;

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
          * Собрать товары к заказу для подготовки к отправке.
          */
    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        Map<UUID, Long> products = request.getProducts();

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Products cannot be null or empty");
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            ProductStock productStock = repository.findById(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Product with ID " + productId + " not found in warehouse"));

            if (productStock.getQuantity() < quantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Insufficient stock for product: " + productId);
            }

            productStock.setQuantity(productStock.getQuantity() - quantity);
            repository.save(productStock);

            totalWeight += productStock.getWeight() * quantity;
            totalVolume += productStock.volume() * quantity;
            fragile = fragile || Boolean.TRUE.equals(productStock.getFragile());
        }

        OrderBooking booking = OrderBooking.builder()
                .bookingId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .totalWeight(totalWeight)
                .totalVolume(totalVolume)
                .fragile(fragile)
                .products(products)
                .build();

        bookingRepository.save(booking);

        log.info("Order assembled: orderId={}, productsCount={}, weight={}, volume={}, fragile={}",
                request.getOrderId(), products.size(), totalWeight, totalVolume, fragile);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    /**
     * Передать товары в доставку.
     */
    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        OrderBooking booking = bookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order booking not found for order: " + request.getOrderId()));

        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);

        log.info("Products shipped to delivery: orderId={}, deliveryId={}",
                request.getOrderId(), request.getDeliveryId());
    }

    /**
     * Принять возврат товаров на склад.
     */
    @Transactional
    public void returnProduct(Map<UUID, Long> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Products cannot be null or empty");
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            ProductStock stock = repository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Product with ID " + entry.getKey() + " not found in warehouse"));

            stock.setQuantity(stock.getQuantity() + entry.getValue());
            repository.save(stock);

            log.debug("Product returned: productId={}, quantity={}", entry.getKey(), entry.getValue());
        }

        log.info("Products returned to warehouse: {} products", products.size());
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
