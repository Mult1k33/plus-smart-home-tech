package ru.yandex.practicum.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {

    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();

    String error;

    String message;

    String detail;
}
