package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.delivery.DeliveryDto;
import ru.yandex.practicum.delivery.NewDeliveryRequestDto;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.model.Address;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.warehouse.AddressDto;

import java.util.UUID;

@Component
public class DeliveryToDtoMapper {

    /**
     * Преобразование Delivery в DeliveryDto
     */
    public DeliveryDto toDto(Delivery delivery) {
        if (delivery == null) return null;

        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .fromAddress(toAddressDto(delivery.getFromAddress()))
                .toAddress(toAddressDto(delivery.getToAddress()))
                .deliveryState(delivery.getDeliveryState())
                .build();
    }

    /**
     * Преобразование DeliveryDto в Delivery (без веса, объема, fragile)
     */
    public Delivery toEntity(DeliveryDto dto) {
        if (dto == null) return null;
        return Delivery.builder()
                .deliveryId(dto.getDeliveryId())
                .orderId(dto.getOrderId())
                .deliveryState(dto.getDeliveryState() != null ? dto.getDeliveryState() : DeliveryState.CREATED)
                .fromAddress(toAddress(dto.getFromAddress()))
                .toAddress(toAddress(dto.getToAddress()))
                .build();
    }

    /**
     * Преобразование NewDeliveryRequestDto в Delivery
     */
    public Delivery toEntity(NewDeliveryRequestDto request) {
        if (request == null) return null;

        return Delivery.builder()
                .deliveryId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .totalWeight(request.getTotalWeight())
                .totalVolume(request.getTotalVolume())
                .fragile(request.getFragile())
                .deliveryState(DeliveryState.CREATED)
                .fromAddress(toAddress(request.getFromAddress()))
                .toAddress(toAddress(request.getToAddress()))
                .build();
    }

    /**
     * Преобразование Address в AddressDto
     */
    private AddressDto toAddressDto(Address address) {
        if (address == null) return null;

        return AddressDto.builder()
                .country(address.getCountry())
                .city(address.getCity())
                .street(address.getStreet())
                .house(address.getHouse())
                .flat(address.getFlat())
                .build();
    }

    /**
     * Преобразование AddressDto в Address
     */
    private Address toAddress(AddressDto dto) {
        if (dto == null) return null;

        return Address.builder()
                .addressId(UUID.randomUUID())
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }
}
