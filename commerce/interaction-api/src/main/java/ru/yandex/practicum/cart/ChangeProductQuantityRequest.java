package ru.yandex.practicum.cart;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeProductQuantityRequest {

    @NotNull
    UUID productId;

    @NotNull
    Long newQuantity;
}
