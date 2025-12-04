#!/bin/bash

echo "Waiting for Debezium Connect to be ready..."
until curl -f http://localhost:8087/ > /dev/null 2>&1; do
  echo "Debezium Connect not ready yet, waiting..."
  sleep 2
done

echo "Registering Debezium PostgreSQL Connector for Inventory Outbox..."
curl -X POST http://localhost:8087/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "inventory-outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "serendib-postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "password",
      "database.dbname": "inventory_db",
      "topic.prefix": "dbserver1",
      "database.server.name": "dbserver1",
      "table.include.list": "public.inventory_outbox",
      "plugin.name": "pgoutput",
      "publication.autocreate.mode": "filtered",
      "slot.name": "debezium_inventory_outbox_slot",
      "key.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "key.converter.schemas.enable": "false",
      "value.converter.schemas.enable": "false"
    }
  }'

echo ""
echo "Connector registered! Checking status..."
curl -s http://localhost:8087/connectors/inventory-outbox-connector/status | jq .
