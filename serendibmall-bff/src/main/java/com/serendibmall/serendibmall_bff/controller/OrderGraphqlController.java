package com.serendibmall.serendibmall_bff.controller;

import com.serendibmall.order.v1.CreateOrderRequest;
import com.serendibmall.order.v1.CreateOrderResponse;
import com.serendibmall.order.v1.OrderServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class OrderGraphqlController {

    private final OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub;

    public OrderGraphqlController(@Qualifier("orderServiceChannel") ManagedChannel channel) {
        this.orderServiceStub = OrderServiceGrpc.newBlockingStub(channel);
    }

    @MutationMapping
    public Order createOrder(@Argument String productId, @Argument Integer quantity) {
        // Hardcoded userId for now as per instructions
        String userId = "user-123";

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setUserId(userId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build();

        CreateOrderResponse response = orderServiceStub.createOrder(request);

        return new Order(response.getOrderId(), response.getStatus(), productId);
    }

    public record Order(String id, String status, String productId) {}
}
