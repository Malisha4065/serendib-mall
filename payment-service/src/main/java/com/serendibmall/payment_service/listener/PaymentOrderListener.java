package com.serendibmall.payment_service.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.payment_service.entity.Payment;
import com.serendibmall.payment_service.entity.PaymentOutbox;
import com.serendibmall.payment_service.repository.PaymentOutboxRepository;
import com.serendibmall.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentOrderListener {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @KafkaListener(topics = "dbserver1.public.orders", groupId = "payment-service-saga")
    @Transactional
    public void handleOrderEvent(String message) {
        try {
            log.info("Received order event: {}", message);
            JsonNode payload = objectMapper.readTree(message);

            String op = payload.has("op") ? payload.get("op").asText() : "";
            
            // Only interested in UPDATE events
            if (!"u".equals(op)) {
                return;
            }

            JsonNode before = payload.get("before");
            JsonNode after = payload.get("after");
            
            if (after == null) {
                return;
            }

            String newStatus = after.get("status").asText();
            String oldStatus = before != null && before.has("status") ? before.get("status").asText() : "";

            // Only process when status changes to PAYMENT_PENDING
            if (!"PAYMENT_PENDING".equals(newStatus) || "PAYMENT_PENDING".equals(oldStatus)) {
                return;
            }

            String orderId = after.get("id").asText();
            
            // Idempotency check: Skip if payment already exists for this order
            if (paymentRepository.existsByOrderId(orderId)) {
                log.info("Payment already exists for order: {}, skipping duplicate processing", orderId);
                return;
            }
            
            log.info("Processing payment for order: {}", orderId);

            // Simulate payment processing (80% success rate)
            boolean paymentSuccessful = random.nextDouble() < 0.8;
            
            String paymentId = UUID.randomUUID().toString();
            String transactionId = UUID.randomUUID().toString();
            
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .amount(new BigDecimal("99.99")) // Simulated amount
                    .status(paymentSuccessful ? "SUCCESS" : "FAILED")
                    .transactionId(paymentSuccessful ? transactionId : null)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            paymentRepository.save(payment);
            
            // Create outbox event for Debezium to pick up
            String eventType = paymentSuccessful ? "PaymentProcessedEvent" : "PaymentFailedEvent";
            
            // Use ObjectMapper for safer JSON serialization (handles special characters)
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("orderId", orderId);
            if (paymentSuccessful) {
                payloadMap.put("paymentId", paymentId);
                payloadMap.put("transactionId", transactionId);
            } else {
                payloadMap.put("reason", "Payment declined");
            }
            String eventPayload = objectMapper.writeValueAsString(payloadMap);
            
            PaymentOutbox outboxEvent = PaymentOutbox.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("Order")
                    .aggregateId(orderId)
                    .eventType(eventType)
                    .payload(eventPayload)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            outboxRepository.save(outboxEvent);
            
            if (paymentSuccessful) {
                log.info("Payment successful for order: {}, transactionId: {}", orderId, transactionId);
            } else {
                log.warn("Payment failed for order: {}", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing order event for payment", e);
        }
    }
}

