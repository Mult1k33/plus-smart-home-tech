package ru.yandex.practicum.warehouse;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressDto {

    String country;

    String city;

    String street;

    String house;

    String flat;
}
