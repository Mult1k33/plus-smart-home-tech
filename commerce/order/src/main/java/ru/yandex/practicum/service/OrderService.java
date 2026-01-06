package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.*;
import ru.yandex.practicum.delivery.*;
import ru.yandex.practicum.enums.OrderState;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.mapper.OrderToDtoMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.order.*;
import ru.yandex.practicum.repository.OrderRepository;
import ru.yandex.practicum.warehouse.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository repository;
    private final OrderToDtoMapper mapper;
    private final DeliveryClient deliveryClient;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    /**
     * Получения списка заказов пользователя.
     */
    public List<OrderDto> getUserOrders(String username) {
        validateUsername(username);
        List<Order> orders = repository.findAllByUsername(username);

        log.debug("Found {} orders for user: {}", orders.size(), username);

        return orders.stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Создание нового заказа из корзины покупок.
     */
    @Transactional
    public OrderDto createOrder(CreateNewOrderRequest request, String username) {
        validateUsername(username);

        // Создание заказа
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .shoppingCartId(request.getShoppingCartDto().getShoppingCartId())
                .username(username)
                .products(request.getShoppingCartDto().getProducts())
                .state(OrderState.NEW)
                .fragile(false)
                .totalPrice(0.0)
                .deliveryPrice(0.0)
                .productPrice(0.0)
                .build();

        repository.save(order);
        log.info("Order created: {} with state: {}", order.getOrderId(), order.getState());

        // Бронирование товаров на складе
        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getOrderId())
                .products(request.getShoppingCartDto().getProducts())
                .build();

        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(assemblyRequest);
        log.debug("Products booked for order: {}", order.getOrderId());

        // Создание доставки
        NewDeliveryRequestDto deliveryRequest = NewDeliveryRequestDto.builder()
                .orderId(order.getOrderId())
                .toAddress(request.getDeliveryAddress())
                .fromAddress(warehouseClient.getWarehouseAddress())
                .totalWeight(booked.getDeliveryWeight())
                .totalVolume(booked.getDeliveryVolume())
                .fragile(booked.getFragile())
                .build();

        DeliveryDto delivery = deliveryClient.createDelivery(deliveryRequest);
        log.debug("Delivery created: {}", delivery.getDeliveryId());

        // Обновление заказа
        order.setState(OrderState.ASSEMBLED);
        order.setDeliveryId(delivery.getDeliveryId());
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.getFragile());

        Order savedOrder = repository.save(order);
        log.info("Order assembled: {}", order.getOrderId());

        return mapper.toDto(savedOrder);
    }

    /**
     * Возврат товаров по заказу.
     */
    @Transactional
    public OrderDto returnProducts(ProductReturnRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new IllegalArgumentException("Return request cannot be null");
        }

        Order order = getOrderOrThrow(request.getOrderId());

        warehouseClient.returnProduct(request.getProducts());
        log.debug("Products returned to warehouse for order: {}", request.getOrderId());

        order.setState(OrderState.PRODUCT_RETURNED);
        Order updatedOrder = repository.save(order);

        log.info("Order marked as returned: {}", request.getOrderId());

        return mapper.toDto(updatedOrder);
    }

    /**
     * Расчет общей стоимости заказа.
     */
    @Transactional
    public OrderDto calculateTotal(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        OrderDto orderDto = mapper.toDto(order);

        // Расчет стоимости товаров и доставки
        Double productCost = paymentClient.productCost(orderDto);
        Double deliveryCost = deliveryClient.calculateDeliveryCost(orderDto);

        // Расчет итоговой стоимости
        Double tax = productCost * 0.1;
        Double totalCost = productCost + tax + deliveryCost;

        order.setProductPrice(productCost);
        order.setDeliveryPrice(deliveryCost);
        order.setTotalPrice(totalCost);
        order.setState(OrderState.ON_PAYMENT);

        Order updatedOrder = repository.save(order);
        log.info("Total cost calculated: {} for order: {}", totalCost, orderId);

        return mapper.toDto(updatedOrder);
    }

    /**
     * Расчет стоимости доставки заказа.
     */
    @Transactional
    public OrderDto calculateDelivery(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        Double deliveryCost = deliveryClient.calculateDeliveryCost(mapper.toDto(order));
        order.setDeliveryPrice(deliveryCost);

        Order updatedOrder = repository.save(order);
        log.info("Delivery cost calculated: {} for order: {}", deliveryCost, orderId);

        return mapper.toDto(updatedOrder);
    }

    /**
     * Обработка оплаты заказа.
     */
    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        paymentClient.createPayment(mapper.toDto(order));
        log.debug("Payment created for order: {}", orderId);

        order.setState(OrderState.ON_PAYMENT);
        Order updatedOrder = repository.save(order);

        log.info("Order marked as on payment: {}", orderId);

        return mapper.toDto(updatedOrder);
    }

    /**
     * Успешная оплата заказа.
     */
    @Transactional
    public OrderDto paymentSuccess(UUID orderId) {
        return updateOrderState(orderId, OrderState.PAID);
    }

    /**
     * Неудачная оплата заказа.
     */
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.PAYMENT_FAILED);
    }

    /**
     * Успешная доставка заказа.
     */
    @Transactional
    public OrderDto delivery(UUID orderId) {
        return updateOrderState(orderId, OrderState.DELIVERED);
    }

    /**
     * Неудачная доставка заказа.
     */
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.DELIVERY_FAILED);
    }

    /**
     * Завершение заказа.
     */
    @Transactional
    public OrderDto completed(UUID orderId) {
        return updateOrderState(orderId, OrderState.COMPLETED);
    }

    /**
     * Сборка заказа.
     */
    @Transactional
    public OrderDto assembly(UUID orderId) {
        return updateOrderState(orderId, OrderState.ASSEMBLED);
    }

    /**
     * Неудачная сборка заказа.
     */
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.ASSEMBLY_FAILED);
    }

    /**
     * Валидация имени пользователя.
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
    }

    /**
     * Получение заказа или исключение.
     */
    private Order getOrderOrThrow(UUID orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", orderId);
                    return new NoOrderFoundException("Order not found: " + orderId);
                });
    }

    /**
     * Обновление состояния заказа.
     */
    private OrderDto updateOrderState(UUID orderId, OrderState newState) {
        Order order = getOrderOrThrow(orderId);
        order.setState(newState);

        Order updatedOrder = repository.save(order);
        log.info("Order {} state updated to: {}", orderId, newState);

        return mapper.toDto(updatedOrder);
    }
}
