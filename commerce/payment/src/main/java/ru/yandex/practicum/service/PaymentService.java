package ru.yandex.practicum.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.enums.PaymentState;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.mapper.PaymentToDtoMapper;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.order.OrderDto;
import ru.yandex.practicum.payment.PaymentDto;
import ru.yandex.practicum.repository.PaymentRepository;
import ru.yandex.practicum.store.ProductDto;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentToDtoMapper mapper;
    private final OrderClient orderClient;
    private final ShoppingStoreClient shoppingStoreClient;

    /**
     * Формирование оплаты для заказа.
     */
    @Transactional
    public PaymentDto createPayment(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        Double productCost = calculateProductCost(orderDto);
        Double tax = calculateTax(productCost);
        Double delivery = orderDto.getDeliveryPrice();
        Double total = calculateTotal(productCost, tax, delivery);

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId(orderDto.getOrderId())
                .productTotal(productCost)
                .deliveryTotal(delivery)
                .feeTotal(tax)
                .totalPayment(total)
                .state(PaymentState.PENDING)
                .build();

        Payment savedPayment = repository.save(payment);

        log.info("Created payment for order: orderId={}, paymentId={}, total={}",
                orderDto.getOrderId(), savedPayment.getPaymentId(), total);

        return mapper.toDto(savedPayment);
    }

    /**
     * Расчёт стоимости товаров в заказе.
     */
    @Transactional(readOnly = true)
    public Double calculateProductCost(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        double totalCost = 0.0;

        for (Map.Entry<UUID, Long> entry : orderDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            ProductDto product = shoppingStoreClient.getProduct(productId);
            totalCost += product.getPrice() * quantity;
        }

        log.debug("Calculated product cost for order: orderId={}, cost={}",
                orderDto.getOrderId(), totalCost);

        return totalCost;
    }

    /**
     * Расчёт полной стоимости заказа (товары + налог + доставка).
     */
    @Transactional(readOnly = true)
    public Double calculateTotalCost(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        Double productCost = orderDto.getProductPrice();

        if (productCost == null) {
            productCost = calculateProductCost(orderDto);
        }

        Double tax = calculateTax(productCost);
        Double delivery = orderDto.getDeliveryPrice();

        return calculateTotal(productCost, tax, delivery);
    }

    /**
     * Эмуляция успешной оплаты от платежного шлюза.
     * Обновляет статус оплаты и уведомляет сервис заказов.
     */
    @Transactional
    public void processPaymentSuccess(UUID paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment not found with ID: " + paymentId));

        payment.setState(PaymentState.SUCCESS);
        repository.save(payment);

        orderClient.paymentSuccess(payment.getOrderId());

        log.info("Payment marked as successful: paymentId={}, orderId={}",
                paymentId, payment.getOrderId());
    }

    /**
     * Эмуляция отказа в оплате от платежного шлюза.
     * Обновляет статус оплаты и уведомляет сервис заказов.
     */
    @Transactional
    public void processPaymentFailed(UUID paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment not found with ID: " + paymentId));

        payment.setState(PaymentState.FAILED);
        repository.save(payment);

        orderClient.paymentFailed(payment.getOrderId());

        log.info("Payment marked as failed: paymentId={}, orderId={}",
                paymentId, payment.getOrderId());
    }

    /**
     * Валидация заказа для расчёта.
     * Проверяет наличие необходимой информации.
     */
    private void validateOrderForCalculation(OrderDto orderDto) {
        if (orderDto == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Order cannot be null");
        }

        if (orderDto.getProducts() == null || orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Order must contain products for calculation");
        }

        if (orderDto.getDeliveryPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Delivery price is required for calculation");
        }
    }

    /**
     * Расчёт налога (10% от стоимости товаров).
     */
    private Double calculateTax(Double productCost) {
        return productCost * 0.1;
    }

    /**
     * Расчёт общей суммы (товары + налог + доставка).
     */
    private Double calculateTotal(Double productCost, Double tax, Double delivery) {
        return productCost + tax + delivery;
    }
}
