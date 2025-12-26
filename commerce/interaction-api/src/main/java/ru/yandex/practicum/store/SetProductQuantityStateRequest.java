package ru.yandex.practicum.store;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.QuantityState;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SetProductQuantityStateRequest {

    @NotNull
    UUID productId;

    @NotNull
    QuantityState quantityState;
}
