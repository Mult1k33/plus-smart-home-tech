package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.store.ProductDto;

import java.math.BigDecimal;

@Component
public class ProductToDtoMapper {

    public ProductDto toDto(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .imageSrc(product.getImageSrc())
                .quantityState(product.getQuantityState())
                .productState(product.getProductState())
                .productCategory(product.getProductCategory())
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .build();
    }

    public Product toEntity(ProductDto productDto) {
        return Product.builder()
                .productId(productDto.getProductId())
                .productName(productDto.getProductName())
                .description(productDto.getDescription())
                .imageSrc(productDto.getImageSrc())
                .quantityState(productDto.getQuantityState())
                .productState(productDto.getProductState())
                .productCategory(productDto.getProductCategory())
                .price(productDto.getPrice() != null ? BigDecimal.valueOf(productDto.getPrice()) : null)
                .build();
    }

    public void updateEntityFromDto(ProductDto dto, Product entity) {
        if (dto.getProductName() != null) {
            entity.setProductName(dto.getProductName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getImageSrc() != null) {
            entity.setImageSrc(dto.getImageSrc());
        }
        if (dto.getPrice() != null) {
            entity.setPrice(BigDecimal.valueOf(dto.getPrice()));
        }
        if (dto.getProductCategory() != null) {
            entity.setProductCategory(dto.getProductCategory());
        }
        if (dto.getProductState() != null) {
            entity.setProductState(dto.getProductState());
        }
        if (dto.getQuantityState() != null) {
            entity.setQuantityState(dto.getQuantityState());
        }
    }
}
