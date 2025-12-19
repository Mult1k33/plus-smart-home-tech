package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductState;
import ru.yandex.practicum.enums.QuantityState;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "shopping_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @Column(name = "product_id", nullable = false)
    UUID productId;

    @Column(name = "product_name", nullable = false)
    @NotBlank
    @Size(max = 255)
    String productName;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    String description;

    @Column(name = "image_src")
    @Size(max = 1024)
    String imageSrc;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_state", nullable = false)
    @NotNull
    QuantityState quantityState;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_state", nullable = false)
    @NotNull
    ProductState productState;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", nullable = false)
    @NotNull
    ProductCategory productCategory;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    @DecimalMin(value = "1.00", inclusive = true)
    BigDecimal price;
}
