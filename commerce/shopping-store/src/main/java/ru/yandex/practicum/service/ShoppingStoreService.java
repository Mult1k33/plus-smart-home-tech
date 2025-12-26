package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.enums.*;
import ru.yandex.practicum.exception.NotFoundProductException;
import ru.yandex.practicum.mapper.ProductToDtoMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ShoppingStoreProductRepository;
import ru.yandex.practicum.store.ProductDto;
import ru.yandex.practicum.store.SetProductQuantityStateRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShoppingStoreService {

    private final ShoppingStoreProductRepository repository;
    private final ProductToDtoMapper mapper;

    /**
     * Получение списка товаров по категории в пагинированном виде.
     */
    public Page<ProductDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        log.debug("Getting products by category: {}", category);
        return repository.findByProductCategory(category, pageable)
                .map(mapper::toDto);
    }

    /**
     * Создание нового товара в ассортименте.
     */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = mapper.toEntity(dto);

        if (product.getProductId() == null) {
            product.setProductId(UUID.randomUUID());
        }
        if (product.getProductState() == null) {
            product.setProductState(ProductState.ACTIVE);
        }
        if (product.getQuantityState() == null) {
            product.setQuantityState(QuantityState.ENOUGH);
        }

        Product saved = repository.save(product);
        log.info("Created product: {} - {}", saved.getProductId(), saved.getProductName());
        return mapper.toDto(saved);
    }

    /**
     * Обновление существующего товара.
     * Проверяет существование товара перед обновлением.
     */
    @Transactional
    public ProductDto updateProduct(ProductDto dto) {
        if (dto.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for update");
        }

        Product product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundProductException("Product not found with id: " + dto.getProductId()));

        mapper.updateEntityFromDto(dto, product);

        Product saved = repository.save(product);
        log.info("Updated product: {} - {}", saved.getProductId(), saved.getProductName());
        return mapper.toDto(saved);
    }

    /**
     * Удаление товара из ассортимента (деактивация).
     * Реальное удаление не происходит, товар переходит в состояние DEACTIVATE.
     */
    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new NotFoundProductException("Product not found with id: " + productId));

        // Проверяем, не деактивирован ли уже товар
        if (product.getProductState() == ProductState.DEACTIVATE) {
            log.warn("Product already deactivated: {}", productId);
            return false;
        }

        product.setProductState(ProductState.DEACTIVATE);
        repository.save(product);

        log.info("Deactivated product: {}", productId);
        return true;
    }

    /**
     * Установка статуса по товару.
     * API вызывается со стороны склада для обновления QuantityState.
     */
    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        Product product = repository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundProductException(
                        "Product not found with id: " + request.getProductId()));

        product.setQuantityState(request.getQuantityState());
        repository.save(product);

        log.info("Updated quantity state for product {}: {}",
                product.getProductId(), request.getQuantityState());
        return true;
    }

    /**
     * Получение сведений о товаре по ID из БД.
     */
    public ProductDto getProduct(UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new NotFoundProductException("Product not found with id: " + productId));

        log.debug("Retrieved product: {} - {}", product.getProductId(), product.getProductName());
        return mapper.toDto(product);
    }
}
