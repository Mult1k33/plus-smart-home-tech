-- Создание схемы для сервиса заказов
CREATE SCHEMA IF NOT EXISTS order_service;

-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS order_service.orders (
    order_id UUID PRIMARY KEY,
    shopping_cart_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    payment_id UUID,
    delivery_id UUID,
    state varchar(30) NOT NULL,
    delivery_weight DOUBLE PRECISION,
    delivery_volume DOUBLE PRECISION,
    fragile BOOLEAN,
    total_price DOUBLE PRECISION,
    delivery_price DOUBLE PRECISION,
    product_price DOUBLE PRECISION
);

-- Создание таблицы товаров в заказе
CREATE TABLE IF NOT EXISTS order_service.order_products (
    order_id UUID REFERENCES order_service.orders(order_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL CHECK ( quantity >= 0 ),
    PRIMARY KEY (order_id, product_id)
);