package ru.yandex.practicum.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.warehouse.AddressDto;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewDeliveryRequestDto {

    @NotNull
    UUID orderId;

    @NotNull
    AddressDto toAddress;

    AddressDto fromAddress;

    Double totalWeight;

    Double totalVolume;

    Boolean fragile;
}
