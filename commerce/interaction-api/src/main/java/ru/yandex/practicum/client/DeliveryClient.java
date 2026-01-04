package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.delivery.DeliveryDto;
import ru.yandex.practicum.delivery.NewDeliveryRequestDto;
import ru.yandex.practicum.order.OrderDto;

import java.util.UUID;

@FeignClient(name = "delivery", path = "/api/v1/delivery")
public interface DeliveryClient {

    @PutMapping
    DeliveryDto createDelivery(@RequestBody NewDeliveryRequestDto request);

    @PostMapping("/successful")
    DeliveryDto success(@RequestBody UUID orderId);

    @PostMapping("/picked")
    DeliveryDto pickOrder(@RequestBody UUID orderId);

    @PostMapping("/failed")
    DeliveryDto failed(@RequestBody UUID orderId);

    @PostMapping("/cost")
    Double calculateDeliveryCost(@RequestBody OrderDto orderDto);
}
