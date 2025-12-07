#!/bin/bash

echo "Waiting for Debezium Connect to be ready..."
until curl -f http://localhost:8087/ > /dev/null 2>&1; do
  echo "Debezium Connect not ready yet, waiting..."
  sleep 2
done

echo "Registering Debezium PostgreSQL Connector for Order DB..."
curl -X POST http://localhost:8087/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-events-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "serendib-postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "password",
      "database.dbname": "order_db",
      "topic.prefix": "dbserver1",
      "database.server.name": "dbserver1",
      "table.include.list": "public.order_outbox",
      "plugin.name": "pgoutput",
      "publication.autocreate.mode": "filtered",
      "slot.name": "debezium_order_slot",
      "key.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "key.converter.schemas.enable": "false",
      "value.converter.schemas.enable": "false",
      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
      "transforms.outbox.table.field.event.key": "aggregate_id",
      "transforms.outbox.table.field.event.type": "event_type",
      "transforms.outbox.table.field.event.payload": "payload",
      "transforms.outbox.route.topic.replacement": "order.events"
    }
  }'

echo ""
echo "Connector registered! Checking status..."
curl -s http://localhost:8087/connectors/order-events-connector/status | jq .
