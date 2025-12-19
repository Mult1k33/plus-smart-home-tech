package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "shopping_cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingCart {

    @Id
    @Column(name = "shopping_cart_id", nullable = false)
    UUID shoppingCartId;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    ShoppingCartState state = ShoppingCartState.ACTIVE;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<CartItem> items = new HashSet<>();
}
