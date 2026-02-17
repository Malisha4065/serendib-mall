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

-- Outbox table for transactional messaging (Debezium CDC)
CREATE TABLE IF NOT EXISTS order_outbox (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Inventory database setup
\c inventory_db;

CREATE TABLE IF NOT EXISTS inventory (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE NOT NULL,
    quantity INT DEFAULT 0
);

-- Outbox table for transactional messaging (Debezium CDC)
CREATE TABLE IF NOT EXISTS inventory_outbox (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);


-- Payment database setup
CREATE DATABASE payment_db;

\c payment_db;

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2),
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payment_outbox (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
