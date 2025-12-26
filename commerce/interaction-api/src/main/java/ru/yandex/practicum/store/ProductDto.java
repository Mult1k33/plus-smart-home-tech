package ru.yandex.practicum.store;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDto {

    UUID productId;

    @NotNull
    String productName;

    @NotNull
    String description;

    String imageSrc;

    @NotNull
    QuantityState quantityState;

    @NotNull
    ProductState productState;

    @NotNull
    ProductCategory productCategory;

    @NotNull
    @Min(value = 1)
    Double price;
}
