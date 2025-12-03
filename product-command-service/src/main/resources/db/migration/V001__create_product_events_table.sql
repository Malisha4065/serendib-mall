-- Create product_events table for Event Sourcing
CREATE TABLE IF NOT EXISTS product_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_product_events_aggregate_id ON product_events(aggregate_id);
CREATE INDEX idx_product_events_created_at ON product_events(created_at DESC);
CREATE INDEX idx_product_events_event_type ON product_events(event_type);
