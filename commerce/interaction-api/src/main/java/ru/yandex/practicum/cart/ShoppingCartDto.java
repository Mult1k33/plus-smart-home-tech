package ru.yandex.practicum.cart;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingCartDto {

    @NotNull
    UUID shoppingCartId;

    @NotNull
    Map<UUID, Long> products;
}
