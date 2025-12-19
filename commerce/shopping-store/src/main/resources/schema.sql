-- Создание схемы витрины товаров
create SCHEMA IF NOT EXISTS shopping_store;

-- Таблица товаров магазина
create TABLE IF NOT EXISTS shopping_store.products
(
    product_id UUID PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_src VARCHAR(1024),
    quantity_state VARCHAR(30) NOT NULL CHECK ( quantity_state IN ('ENDED', 'FEW', 'ENOUGH', 'MANY')),
    product_state VARCHAR(30) NOT NULL CHECK ( product_state IN ('ACTIVE', 'DEACTIVATE')),
    product_category VARCHAR(30) NOT NULL CHECK (product_category IN ('LIGHTING', 'CONTROL', 'SENSORS')),
    price NUMERIC(12, 2) NOT NULL CHECK ( price >= 1 )
)