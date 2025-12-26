package ru.yandex.practicum.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DimensionDto {

    @NotNull
    @Min(value = 1)
    Double width;

    @NotNull
    @Min(value = 1)
    Double height;

    @NotNull
    @Min(value = 1)
    Double depth;
}
