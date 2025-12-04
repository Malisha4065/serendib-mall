package com.serendibmall.order_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.order_service.entity.Order;
import com.serendibmall.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.events", groupId = "order-service-saga")
    public void handleInventoryEvent(String message) {
        try {
            log.info("Received inventory event: {}", message);
            JsonNode event = objectMapper.readTree(message);

            String eventType = event.get("type").asText();
            String orderId = event.get("orderId").asText();

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
            log.error("Error processing inventory event", e);
        }
    }
}
