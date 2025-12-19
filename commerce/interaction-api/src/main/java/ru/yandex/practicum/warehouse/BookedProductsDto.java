package ru.yandex.practicum.warehouse;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookedProductsDto {

    @NotNull
    Double deliveryWeight;

    @NotNull
    Double deliveryVolume;

    @NotNull
    Boolean fragile;
}
