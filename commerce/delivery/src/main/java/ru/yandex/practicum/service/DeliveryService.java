package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.*;
import ru.yandex.practicum.delivery.*;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.mapper.DeliveryToDtoMapper;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.order.OrderDto;
import ru.yandex.practicum.repository.DeliveryRepository;
import ru.yandex.practicum.warehouse.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository repository;
    private final DeliveryToDtoMapper mapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    /**
     * Создание новой доставки в БД.
     */
    @Transactional
    public DeliveryDto createDelivery(NewDeliveryRequestDto request) {
        validateDeliveryRequest(request);

        Delivery delivery = mapper.toEntity(request);
        Delivery savedDelivery = repository.save(delivery);

        log.info("Delivery created: deliveryId={}, orderId={}, state={}",
                savedDelivery.getDeliveryId(), savedDelivery.getOrderId(), savedDelivery.getDeliveryState());

        return mapper.toDto(savedDelivery);
    }

    /**
     * Эмуляция успешной доставки товара.
     * Обновляет статус доставки и уведомляет order сервис.
     */
    @Transactional
    public DeliveryDto markDeliverySuccessful(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        Delivery updatedDelivery = repository.save(delivery);

        orderClient.delivery(orderId);

        log.info("Delivery marked as successful: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    /**
     * Эмуляция получения товара в доставку.
     * Обновляет статус доставки и уведомляет warehouse и order сервисы.
     */
    @Transactional
    public DeliveryDto markDeliveryPicked(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        Delivery updatedDelivery = repository.save(delivery);

        warehouseClient.shippedToDelivery(
                ShippedToDeliveryRequest.builder()
                        .orderId(orderId)
                        .deliveryId(delivery.getDeliveryId())
                        .build()
        );

        orderClient.assembly(orderId);

        log.info("Delivery picked up: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    /**
     * Эмуляция неудачной доставки товара.
     * Обновляет статус доставки и уведомляет order сервис.
     */
    @Transactional
    public DeliveryDto markDeliveryFailed(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.FAILED);
        Delivery updatedDelivery = repository.save(delivery);

        orderClient.deliveryFailed(orderId);

        log.info("Delivery marked as failed: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    /**
     * Расчёт полной стоимости доставки заказа.
     */
    @Transactional(readOnly = true)
    public Double calculateDeliveryCost(OrderDto orderDto) {
        if (orderDto == null || orderDto.getDeliveryVolume() == null || orderDto.getDeliveryWeight() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Not enough info in order to calculate delivery cost");
        }

        Delivery delivery = getDeliveryByOrderIdOrThrow(orderDto.getOrderId());
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        Double cost = calculateDeliveryPrice(
                warehouseAddress,
                delivery.getToAddress(),
                orderDto.getDeliveryWeight(),
                orderDto.getDeliveryVolume(),
                orderDto.getFragile()
        );

        log.debug("Delivery cost calculated: orderId={}, cost={}",
                orderDto.getOrderId(), cost);

        return cost;
    }

    /**
     * Получение доставки по orderId или исключение.
     */
    private Delivery getDeliveryByOrderIdOrThrow(UUID orderId) {
        if (orderId == null) {
            throw new NoOrderFoundException("Order ID cannot be null");
        }

        return repository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Delivery not found for order: {}", orderId);
                    return new NoDeliveryFoundException("Delivery not found for order: " + orderId);
                });
    }

    /**
     * Валидация запроса на создание доставки.
     */
    private void validateDeliveryRequest(NewDeliveryRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Delivery request cannot be null");
        }

        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (request.getFromAddress() == null) {
            throw new IllegalArgumentException("From address is required");
        }

        if (request.getToAddress() == null) {
            throw new IllegalArgumentException("To address is required");
        }

        if (request.getTotalWeight() == null || request.getTotalWeight() <= 0) {
            throw new IllegalArgumentException("Total weight must be positive");
        }

        if (request.getTotalVolume() == null || request.getTotalVolume() <= 0) {
            throw new IllegalArgumentException("Total volume must be positive");
        }

        if (request.getFragile() == null) {
            throw new IllegalArgumentException("Fragile flag is required");
        }
    }

    /**
     * Алгоритм расчета стоимости доставки согласно заданию.
     */
    private Double calculateDeliveryPrice(AddressDto fromAddress, Address toAddress,
                                          Double weight, Double volume, Boolean fragile) {
        double baseCost = 5.0;
        double cost = baseCost;

        // Умножаем базовую стоимость на число, зависящее от адреса склада
        if (fromAddress.getStreet().contains("ADDRESS_2")) {
            cost += baseCost * 2;
        } else if (fromAddress.getStreet().contains("ADDRESS_1")) {
            cost += baseCost * 1;
        }

        // Хрупкость
        if (Boolean.TRUE.equals(fragile)) {
            cost += cost * 0.2;
        }

        // Вес
        cost += weight * 0.3;

        // Объем
        cost += volume * 0.2;

        // Разные улицы
        if (!fromAddress.getStreet().equalsIgnoreCase(toAddress.getStreet())) {
            cost += cost * 0.2;
        }

        // Округление до 2 знаков
        return Math.round(cost * 100.0) / 100.0;
    }
}
