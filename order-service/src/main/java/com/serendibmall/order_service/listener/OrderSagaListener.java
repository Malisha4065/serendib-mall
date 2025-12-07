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
                // Changed: Set to PAYMENT_PENDING instead of CONFIRMED
                order.setStatus("PAYMENT_PENDING");
                orderRepository.save(order);
                log.info("Order set to PAYMENT_PENDING: {}", orderId);
            } else if ("InventoryFailedEvent".equals(eventType)) {
                order.setStatus("REJECTED");
                orderRepository.save(order);
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
                log.info("Order confirmed after payment: {}", orderId);
            } else if (event.has("reason")) {
                // PaymentFailedEvent - payment failed
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                log.info("Order cancelled due to payment failure: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
}
