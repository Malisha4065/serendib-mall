package com.serendibmall.product_query_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.product_query_service.document.ProductDocument;
import com.serendibmall.product_query_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "dbserver1.public.product_events", groupId = "product-query-service")
    public void handleProductEvent(String message) {
        try {
            log.info("Received event: {}", message);

            // Parse Debezium CDC event (Schema-less JSON)
            JsonNode payload = objectMapper.readTree(message);

            if (payload == null) {
                log.warn("Event is null");
                return;
            }

            // Get the operation type (c = create, u = update, d = delete)
            String op = payload.has("op") ? payload.get("op").asText() : null;
            
            if (!"c".equals(op) && !"u".equals(op) && !"r".equals(op)) {
                log.info("Skipping non-create/update operation: {}", op);
                return;
            }

            // Get the after state (new row data)
            JsonNode after = payload.get("after");
            if (after == null) {
                log.warn("No 'after' data in event");
                return;
            }

            // Extract event data
            String eventType = after.get("event_type").asText();
            String eventPayloadStr = after.get("payload").asText();

            log.info("Processing event type: {}", eventType);

            if ("ProductCreated".equals(eventType)) {
                // Parse the event payload
                JsonNode eventData = objectMapper.readTree(eventPayloadStr);

                // Create Elasticsearch document
                ProductDocument productDoc = ProductDocument.builder()
                        .id(eventData.get("product_id").asText())
                        .name(eventData.get("name").asText())
                        .description(eventData.has("description") ? eventData.get("description").asText() : "")
                        .price(eventData.get("price").asDouble())
                        .currency(eventData.has("currency") ? eventData.get("currency").asText() : "USD")
                        .category(eventData.has("category") ? eventData.get("category").asText() : "")
                        .build();

                productRepository.save(productDoc);

                log.info("Product indexed in Elasticsearch: {}", productDoc.getId());
            }

        } catch (Exception e) {
            log.error("Error processing product event", e);
        }
    }
}
