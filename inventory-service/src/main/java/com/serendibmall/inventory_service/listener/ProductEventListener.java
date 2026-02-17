package com.serendibmall.inventory_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.inventory_service.entity.Inventory;
import com.serendibmall.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product.events", groupId = "inventory-service-product")
    @Transactional
    public void handleProductEvent(String message) {
        try {
            log.info("Received product event: {}", message);

            JsonNode payload = objectMapper.readTree(message);

            // Handle potential double-serialized JSON from Debezium SMT
            if (payload.isTextual()) {
                payload = objectMapper.readTree(payload.asText());
            }

            String eventType = null;
            if (payload.has("eventType")) {
                eventType = payload.get("eventType").asText();
            } else if (payload.has("type")) {
                eventType = payload.get("type").asText();
            }

            if ("ProductCreated".equals(eventType)) {
                handleProductCreated(payload);
            }

        } catch (Exception e) {
            log.error("Error processing product event", e);
        }
    }

    private void handleProductCreated(JsonNode payload) {
        String productId = payload.has("product_id") 
                ? payload.get("product_id").asText() 
                : payload.get("productId").asText();

        // Only create if not already exists (idempotent)
        if (inventoryRepository.findByProductId(productId).isEmpty()) {
            Inventory inventory = Inventory.builder()
                    .id(UUID.randomUUID().toString())
                    .productId(productId)
                    .quantity(0)
                    .build();

            inventoryRepository.save(inventory);
            log.info("Created inventory entry for new product: {} with quantity=0", productId);
        } else {
            log.info("Inventory entry already exists for product: {}", productId);
        }
    }
}
