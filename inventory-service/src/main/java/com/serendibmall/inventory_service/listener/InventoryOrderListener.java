package com.serendibmall.inventory_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.inventory_service.entity.Inventory;
import com.serendibmall.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryOrderListener {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
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
                    
                    // Emit Reserved Event
                    emitEvent("InventoryReservedEvent", orderId);
                    log.info("Stock reserved for order: {}", orderId);
                } else {
                    // Insufficient stock
                    emitEvent("InventoryFailedEvent", orderId);
                    log.warn("Insufficient stock for order: {}", orderId);
                }
            } else {
                // Product not found in inventory
                emitEvent("InventoryFailedEvent", orderId);
                log.warn("Product not found in inventory for order: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }

    private void emitEvent(String type, String orderId) {
        try {
            Map<String, String> event = new HashMap<>();
            event.put("type", type);
            event.put("orderId", orderId);
            
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("inventory.events", orderId, json);
        } catch (Exception e) {
            log.error("Error emitting event", e);
        }
    }
}
