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

-- Создание таблицы бронирования заказов для доставки
CREATE TABLE IF NOT EXISTS warehouse.order_booking (
    booking_id uuid PRIMARY KEY,
    order_id uuid NOT NULL UNIQUE,
    delivery_id uuid,
    total_weight DOUBLE PRECISION NOT NULL CHECK ( total_weight >= 0 ),
    total_volume DOUBLE PRECISION NOT NULL CHECK ( total_volume >= 0 ),
    fragile BOOLEAN NOT NULL
);

-- Создание таблицы забронированных товаров
CREATE TABLE IF NOT EXISTS warehouse.order_booking_products (
    booking_id uuid NOT NULL REFERENCES warehouse.order_booking(booking_id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES warehouse.product_stock(product_id) ON DELETE CASCADE,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (booking_id, product_id)