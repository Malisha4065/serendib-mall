CREATE DATABASE product_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;

\c order_db;

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255),
    user_id VARCHAR(255),
    quantity INT,
    status VARCHAR(50),
    created_at TIMESTAMP
);

