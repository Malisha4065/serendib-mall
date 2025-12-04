CREATE DATABASE product_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;

-- Order database setup
\c order_db;

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255),
    user_id VARCHAR(255),
    quantity INT,
    status VARCHAR(50),
    created_at TIMESTAMP
);

-- Inventory database setup
\c inventory_db;

CREATE TABLE IF NOT EXISTS inventory (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE NOT NULL,
    quantity INT DEFAULT 0
);

-- Seed inventory data for testing
INSERT INTO inventory (id, product_id, quantity) VALUES
    ('inv-001', 'product-001', 100),
    ('inv-002', 'product-002', 50),
    ('inv-003', 'product-003', 25),
    ('inv-004', 'product-004', 0)
ON CONFLICT (product_id) DO NOTHING;

