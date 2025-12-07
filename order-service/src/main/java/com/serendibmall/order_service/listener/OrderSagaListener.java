package com.serendibmall.order_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.order_service.entity.Order;
import com.serendibmall.order_service.entity.OrderOutbox;
import com.serendibmall.order_service.repository.OrderOutboxRepository;
import com.serendibmall.order_service.repository.OrderRepository;
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
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.events", groupId = "order-service-saga")
    @Transactional
    public void handleInventoryOutboxEvent(String message) {
        try {
            log.info("Received inventory event: {}", message);
            
            // SMT has already extracted the payload - single parse is enough!
            JsonNode payload = objectMapper.readTree(message);

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
                order.setStatus("PAYMENT_PENDING");
                orderRepository.save(order);
                saveOutboxEvent("OrderPaymentPendingEvent", order);
                log.info("Order set to PAYMENT_PENDING: {}", orderId);
            } else if ("InventoryFailedEvent".equals(eventType)) {
                order.setStatus("REJECTED");
                orderRepository.save(order);
                saveOutboxEvent("OrderRejectedEvent", order);
                log.info("Order rejected: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing inventory event", e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }

    @KafkaListener(topics = "payment.events", groupId = "order-service-saga")
    @Transactional
    public void handlePaymentEvent(String message) {
        try {
            log.info("Received payment event: {}", message);
            JsonNode event = objectMapper.readTree(message);

            String orderId = event.has("orderId") ? event.get("orderId").asText() : null;
            if (orderId == null) {
                log.warn("Payment event missing orderId");
                return;
            }

            Optional<Order> orderOptional = orderRepository.findById(orderId);
            if (orderOptional.isEmpty()) {
                log.warn("Order not found for payment event: {}", orderId);
                return;
            }

            Order order = orderOptional.get();

            // Check if this is a success or failure event based on presence of transactionId
            if (event.has("transactionId")) {
                // PaymentProcessedEvent - payment successful
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                saveOutboxEvent("OrderConfirmedEvent", order);
                log.info("Order confirmed after payment: {}", orderId);
            } else if (event.has("reason")) {
                // PaymentFailedEvent - payment failed
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                saveOutboxEvent("OrderCancelledEvent", order);
                log.info("Order cancelled due to payment failure: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    private void saveOutboxEvent(String eventType, Order order) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("type", eventType);
            payloadMap.put("orderId", order.getId());
            payloadMap.put("productId", order.getProductId());
            payloadMap.put("userId", order.getUserId());
            payloadMap.put("quantity", order.getQuantity());
            payloadMap.put("status", order.getStatus());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            OrderOutbox outboxEvent = OrderOutbox.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("Order")
                    .aggregateId(order.getId())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox event saved: {} for order: {}", eventType, order.getId());
        } catch (Exception e) {
            log.error("Error saving outbox event", e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}

