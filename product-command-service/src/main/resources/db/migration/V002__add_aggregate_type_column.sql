-- Add aggregate_type column for Debezium Outbox pattern
ALTER TABLE product_events 
ADD COLUMN IF NOT EXISTS aggregate_type VARCHAR(255) NOT NULL DEFAULT 'Product';
