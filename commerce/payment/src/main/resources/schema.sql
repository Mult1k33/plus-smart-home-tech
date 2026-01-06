-- Создание схемы для сервиса оплаты
CREATE SCHEMA IF NOT EXISTS payment_service;

-- Создание таблицы платежей
CREATE TABLE IF NOT EXISTS payment_service.payments (
    payment_id UUID NOT NULL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_total DOUBLE PRECISION,
    delivery_total DOUBLE PRECISION,
    fee_total DOUBLE PRECISION,
    total_payment DOUBLE PRECISION,
    state VARCHAR(30) NOT NULL
);