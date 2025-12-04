package com.serendibmall.inventory_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.inventory_service.entity.Inventory;
import com.serendibmall.inventory_service.entity.InventoryOutbox;
import com.serendibmall.inventory_service.repository.InventoryOutboxRepository;
import com.serendibmall.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryOrderListener {

    private final InventoryRepository inventoryRepository;
    private final InventoryOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "dbserver1.public.orders", groupId = "inventory-service-saga")
    @Transactional
    public void handleOrderEvent(String message) {
        try {
            log.info("Received order event: {}", message);
            JsonNode payload = objectMapper.readTree(message);

            // Debezium event structure
            JsonNode after = payload.get("after");
            String op = payload.has("op") ? payload.get("op").asText() : "";

            if (!"c".equals(op) || after == null) {
                return; // Only interested in Create events
            }

            String orderId = after.get("id").asText();
            String productId = after.get("product_id").asText();
            int quantity = after.get("quantity").asInt();
            String status = after.get("status").asText();

            if (!"PENDING".equals(status)) {
                return;
            }

            log.info("Processing order: {}, product: {}, quantity: {}", orderId, productId, quantity);

            Optional<Inventory> inventoryOptional = inventoryRepository.findByProductId(productId);

            if (inventoryOptional.isPresent()) {
                Inventory inventory = inventoryOptional.get();
                if (inventory.getQuantity() >= quantity) {
                    // Reserve stock
                    inventory.setQuantity(inventory.getQuantity() - quantity);
                    inventoryRepository.save(inventory);
                    
                    // Write to outbox table (same transaction as inventory update)
                    saveOutboxEvent("InventoryReservedEvent", orderId, productId, quantity);
                    log.info("Stock reserved for order: {}", orderId);
                } else {
                    // Insufficient stock - write failure event to outbox
                    saveOutboxEvent("InventoryFailedEvent", orderId, productId, quantity);
                    log.warn("Insufficient stock for order: {}", orderId);
                }
            } else {
                // Product not found in inventory - write failure event to outbox
                saveOutboxEvent("InventoryFailedEvent", orderId, productId, quantity);
                log.warn("Product not found in inventory for order: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing order event", e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    private void saveOutboxEvent(String eventType, String orderId, String productId, int quantity) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("type", eventType);
            payloadMap.put("orderId", orderId);
            payloadMap.put("productId", productId);
            payloadMap.put("quantity", quantity);
            
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            
            InventoryOutbox outboxEvent = InventoryOutbox.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("Order")
                    .aggregateId(orderId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            outboxRepository.save(outboxEvent);
            log.info("Outbox event saved: {} for order: {}", eventType, orderId);
        } catch (Exception e) {
            log.error("Error saving outbox event", e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
