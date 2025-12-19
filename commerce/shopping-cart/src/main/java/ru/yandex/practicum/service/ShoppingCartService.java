package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.mapper.ShoppingCartToDtoMapper;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShoppingCartService {

    private final ShoppingCartRepository repository;
    private final ShoppingCartToDtoMapper mapper;
    private final WarehouseClient warehouseClient;

    /**
     * Получение актуальной корзины пользователя.
     * Если корзины не существует, создать новую.
     */
    public ShoppingCartDto getCart(String username) {
        validateUsername(username);

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        log.debug("Retrieved cart: {} for user: {}", shoppingCart.getShoppingCartId(), username);

        return mapper.toDto(shoppingCart);
    }

    /**
     * Добавление товаров в корзину.
     * Проверка доступности товаров на складе.
     */
    @Transactional
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        validateUsername(username);

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Products cannot be null or empty");
        }

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        // Проверка активности корзины
        checkCartActive(shoppingCart);

        // Добавление товаров в корзину
        addProductsToCart(shoppingCart, products);

        ShoppingCart savedCart = repository.save(shoppingCart);

        // Проверка доступности товаров на складе
        checkProductsAvailability(shoppingCart.getShoppingCartId(), products);

        log.info("Added products to cart: username={}, cartId={}, products={}",
                username, shoppingCart.getShoppingCartId(), products);

        return mapper.toDto(savedCart);
    }

    /**
     * Деактивация корзины.
     * После деактивации корзина становится недоступной для изменений.
     */
    @Transactional
    public void deactivate(String username) {
        validateUsername(username);

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        shoppingCart.setState(ShoppingCartState.DEACTIVATED);
        repository.save(shoppingCart);

        log.info("Deactivated cart: username={}, cartId={}",
                username, shoppingCart.getShoppingCartId());
    }

    /**
     * Удаление товаров из корзины.
     * Проверка, что все указанные товары присутствуют в корзине.
     */
    @Transactional
    public ShoppingCartDto removeProducts(String username, List<UUID> productIds) {
        validateUsername(username);

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseThrow(() -> new NotFoundCartException("Cart not found for user: " + username));

        checkCartActive(shoppingCart);

        // Проверка, что товары есть в корзине
        if (!shoppingCart.getItems().removeIf(i -> productIds.contains(i.getProductId()))) {
            throw new NoProductsInShoppingCartException("Products not found in cart");
        }

        ShoppingCart saved = repository.save(shoppingCart);
        log.info("Removed products: user={}, productIds={}", username, productIds);

        return mapper.toDto(saved);
    }

    /**
     * Изменение количества товаров в корзине
     * При увеличении количества проверяет доступность на складе.
     */
    @Transactional
    public ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        if (request.getNewQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseThrow(() -> new NotFoundCartException("Cart not found for user: " + username));

        checkCartActive(shoppingCart);

        CartItem item = shoppingCart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new NoProductsInShoppingCartException("Product not found in cart"));

        Long oldQuantity = item.getQuantity();
        item.setQuantity(request.getNewQuantity());

        ShoppingCart saved = repository.save(shoppingCart);

        // Проверить доступность при увеличении
        if (request.getNewQuantity() > oldQuantity) {
            ShoppingCartDto checkDto = mapper.toDto(saved);
            warehouseClient.checkAvailability(checkDto);
        }

        item.setQuantity(request.getNewQuantity());

        log.info("Changed quantity: user={}, product={}, newQty={}",
                username, request.getProductId(), request.getNewQuantity());

        return mapper.toDto(saved);
    }

    /**
     * Проверка, что имя пользователя не пустое.
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Username cannot be empty");
        }
    }

    /**
     * Проверка, что корзина активна.
     */
    private void checkCartActive(ShoppingCart cart) {
        if (cart.getState() == ShoppingCartState.DEACTIVATED) {
            cart.setState(ShoppingCartState.ACTIVE);
            log.info("Reactivated cart: cartId={}, user={}",
                    cart.getShoppingCartId(), cart.getUsername());
        }
    }

    /**
     * Проверка доступности товаров на складе.
     * Обрабатывает исключения от warehouse сервиса.
     */
    private void checkProductsAvailability(UUID cartId, Map<UUID, Long> products) {
        ShoppingCartDto checkDto = ShoppingCartDto.builder()
                .shoppingCartId(cartId)
                .products(products)
                .build();

        warehouseClient.checkAvailability(checkDto);

        log.debug("Products availability checked - cartId: {}, productsCount: {}",
                cartId, products.size());
    }

    /**
     * Создание новой корзины для пользователя.
     */
    private ShoppingCart createNewCart(String username) {
        ShoppingCart newCart = ShoppingCart.builder()
                .shoppingCartId(UUID.randomUUID())
                .username(username)
                .state(ShoppingCartState.ACTIVE)
                .items(new HashSet<>())
                .build();

        ShoppingCart savedCart = repository.save(newCart);
        log.info("Created new cart: username={}, cartId={}",
                username, savedCart.getShoppingCartId());

        return savedCart;
    }

    /**
     * Добавление товаров в корзину.
     * Если товар уже есть - увеличивает количество.
     * Если товара нет - добавляет новый.
     */
    private void addProductsToCart(ShoppingCart shoppingCart, Map<UUID, Long> products) {
        Map<UUID, CartItem> itemMap = new HashMap<>();
        for (CartItem item : shoppingCart.getItems()) {
            itemMap.put(item.getProductId(), item);
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            // Валидация количества
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for product: " + productId);
            }

            CartItem existingItem = itemMap.get(productId);

            if (existingItem != null) {
                // Увеличить количество существующего товара
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
            } else {
                // Добавить новый товар
                CartItem newItem = CartItem.builder()
                        .id(UUID.randomUUID())
                        .shoppingCart(shoppingCart)
                        .productId(productId)
                        .quantity(quantity)
                        .build();
                shoppingCart.getItems().add(newItem);
                itemMap.put(productId, newItem);
            }
        }
    }
}
