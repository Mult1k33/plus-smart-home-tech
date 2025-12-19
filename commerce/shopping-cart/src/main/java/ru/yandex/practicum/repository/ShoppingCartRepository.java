package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {
    Optional<ShoppingCart> findByUsername(String username);

    @Query("SELECT DISTINCT sc FROM ShoppingCart sc LEFT JOIN FETCH sc.items WHERE sc.username = :username")
    Optional<ShoppingCart> findByUsernameWithItems(@Param("username") String username);
}
