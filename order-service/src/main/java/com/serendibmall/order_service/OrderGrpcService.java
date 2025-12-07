package com.serendibmall.order_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.order.v1.CreateOrderRequest;
import com.serendibmall.order.v1.CreateOrderResponse;
import com.serendibmall.order.v1.OrderServiceGrpc;
import com.serendibmall.order_service.entity.Order;
import com.serendibmall.order_service.entity.OrderOutbox;
import com.serendibmall.order_service.repository.OrderOutboxRepository;
import com.serendibmall.order_service.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
        try {
            String orderId = UUID.randomUUID().toString();
            log.info("Creating order: {} for product: {}", orderId, request.getProductId());

            Order order = Order.builder()
                    .id(orderId)
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            orderRepository.save(order);
            log.info("Order saved with status PENDING: {}", orderId);

            // Write OrderCreatedEvent to outbox (same transaction)
            saveOutboxEvent("OrderCreatedEvent", orderId, request.getProductId(), 
                    request.getUserId(), request.getQuantity(), "PENDING");

            CreateOrderResponse response = CreateOrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus("PENDING")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error creating order", e);
            responseObserver.onError(e);
        }
    }

    private void saveOutboxEvent(String eventType, String orderId, String productId, 
                                  String userId, int quantity, String status) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("type", eventType);
            payloadMap.put("orderId", orderId);
            payloadMap.put("productId", productId);
            payloadMap.put("userId", userId);
            payloadMap.put("quantity", quantity);
            payloadMap.put("status", status);

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            OrderOutbox outboxEvent = OrderOutbox.builder()
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

