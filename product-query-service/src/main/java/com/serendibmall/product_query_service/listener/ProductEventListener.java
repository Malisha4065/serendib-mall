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

    @KafkaListener(topics = "product.events", groupId = "product-query-service")
    public void handleProductEvent(String message) {
        try {
            log.info("Received event: {}", message);

            // SMT has already extracted the payload - single parse is enough!
            JsonNode payload = objectMapper.readTree(message);

            if (payload == null) {
                log.warn("Event is null");
                return;
            }

            // Get the event type from the payload
            String eventType = payload.has("event_type") ? payload.get("event_type").asText() : null;
            
            // Also check for "type" field for consistency with other services
            if (eventType == null && payload.has("type")) {
                eventType = payload.get("type").asText();
            }

            log.info("Processing event type: {}", eventType);

            if ("ProductCreated".equals(eventType)) {
                // Extract product data directly from the clean payload
                String productId = payload.has("product_id") ? payload.get("product_id").asText() : null;
                
                if (productId == null) {
                    log.warn("ProductCreated event missing product_id");
                    return;
                }

                ProductDocument productDoc = ProductDocument.builder()
                        .id(productId)
                        .name(payload.get("name").asText())
                        .description(payload.has("description") ? payload.get("description").asText() : "")
                        .price(payload.get("price").asDouble())
                        .currency(payload.has("currency") ? payload.get("currency").asText() : "USD")
                        .category(payload.has("category") ? payload.get("category").asText() : "")
                        .build();

                productRepository.save(productDoc);
                log.info("Product indexed in Elasticsearch: {}", productDoc.getId());
            }

        } catch (Exception e) {
            log.error("Error processing product event", e);
        }
    }
}

