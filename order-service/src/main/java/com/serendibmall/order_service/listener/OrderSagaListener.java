package com.serendibmall.order_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.order_service.entity.Order;
import com.serendibmall.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "dbserver1.public.inventory_outbox", groupId = "order-service-saga")
    @Transactional
    public void handleInventoryOutboxEvent(String message) {
        try {
            log.info("Received inventory outbox event: {}", message);
            JsonNode debeziumEvent = objectMapper.readTree(message);

            // Only process create events from Debezium
            String op = debeziumEvent.has("op") ? debeziumEvent.get("op").asText() : "";
            if (!"c".equals(op)) {
                return;
            }

            JsonNode after = debeziumEvent.get("after");
            if (after == null) {
                return;
            }

            // Extract the payload from the outbox event
            String payloadStr = after.get("payload").asText();
            JsonNode payload = objectMapper.readTree(payloadStr);

            String eventType = payload.get("type").asText();
            String orderId = payload.get("orderId").asText();

            log.info("Processing inventory event: {} for order: {}", eventType, orderId);

            Optional<Order> orderOptional = orderRepository.findById(orderId);
            if (orderOptional.isEmpty()) {
                log.warn("Order not found for event: {}", orderId);
                return;
            }

            Order order = orderOptional.get();

            if ("InventoryReservedEvent".equals(eventType)) {
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                log.info("Order confirmed: {}", orderId);
            } else if ("InventoryFailedEvent".equals(eventType)) {
                order.setStatus("REJECTED");
                orderRepository.save(order);
                log.info("Order rejected: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing inventory outbox event", e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
}
