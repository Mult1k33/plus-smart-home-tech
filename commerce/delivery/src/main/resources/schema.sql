-- Создание схемы для сервиса доставки
CREATE SCHEMA IF NOT EXISTS delivery_service;

-- Создание таблицы адресов
CREATE TABLE IF NOT EXISTS delivery_service.addresses (
    address_id uuid PRIMARY KEY,
    country VARCHAR(64),
    city VARCHAR(200),
    street VARCHAR(200),
    house VARCHAR(64),
    flat VARCHAR(64)
);

-- Создание таблицы доставок
CREATE TABLE IF NOT EXISTS delivery_service.deliveries (
    delivery_id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    total_weight DOUBLE PRECISION NOT NULL,
    total_volume DOUBLE PRECISION NOT NULL,
    fragile BOOLEAN NOT NULL,
    delivery_state VARCHAR(20) NOT NULL,
    from_address_id uuid REFERENCES delivery_service.addresses(address_id) ON DELETE CASCADE,
    to_address_id uuid REFERENCES delivery_service.addresses(address_id) ON DELETE CASCADE
);