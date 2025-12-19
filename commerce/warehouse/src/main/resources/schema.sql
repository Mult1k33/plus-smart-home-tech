-- Создание схемы для склада товаров
CREATE SCHEMA IF NOT EXISTS warehouse;

-- Таблица остатков товаров на складе
CREATE TABLE IF NOT EXISTS warehouse.product_stock
(
    product_id UUID PRIMARY KEY,
    fragile BOOLEAN NOT NULL DEFAULT FALSE,
    width DOUBLE PRECISION NOT NULL CHECK ( width >= 1 ),
    height DOUBLE PRECISION NOT NULL CHECK ( height >= 1 ),
    depth  DOUBLE PRECISION NOT NULL CHECK ( depth  >= 1 ),
    weight   DOUBLE PRECISION NOT NULL CHECK ( weight   >= 1 ),
    quantity BIGINT NOT NULL DEFAULT 0 CHECK ( quantity >= 0 )
);