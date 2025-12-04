package com.serendibmall.order_service;

import com.serendibmall.order.v1.CreateOrderRequest;
import com.serendibmall.order.v1.CreateOrderResponse;
import com.serendibmall.order.v1.OrderServiceGrpc;
import com.serendibmall.order_service.entity.Order;
import com.serendibmall.order_service.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderRepository orderRepository;

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
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

        CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus("PENDING")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
