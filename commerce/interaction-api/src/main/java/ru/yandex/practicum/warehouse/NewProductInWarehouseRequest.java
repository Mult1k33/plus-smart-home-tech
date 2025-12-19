package ru.yandex.practicum.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewProductInWarehouseRequest {

    @NotNull
    UUID productId;

    Boolean fragile;

    @NotNull
    DimensionDto dimension;

    @NotNull
    @Min(value = 1)
    Double weight;
}
