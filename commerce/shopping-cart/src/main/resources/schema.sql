-- Создание схемы для корзины пользователя
CREATE SCHEMA IF NOT EXISTS shopping_cart;

-- Таблица корзин пользователей
CREATE TABLE IF NOT EXISTS shopping_cart.carts (
    shopping_cart_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    state VARCHAR(30) NOT NULL CHECK (state IN ('ACTIVE', 'DEACTIVATED')) DEFAULT 'ACTIVE'
);

-- Таблица продуктов в корзине
CREATE TABLE IF NOT EXISTS shopping_cart.cart_items (
    id UUID PRIMARY KEY,
    shopping_cart_id UUID NOT NULL REFERENCES shopping_cart.carts (shopping_cart_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL CHECK ( quantity >= 0 ),
    CONSTRAINT unique_cart_product UNIQUE (shopping_cart_id, product_id)
)