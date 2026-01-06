package ru.yandex.practicum.payment;

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
public class PaymentDto {

    @NotNull
    UUID paymentId;

    Double totalPayment;

    Double deliveryTotal;

    Double feeTotal;
}
