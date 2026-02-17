package com.serendibmall.serendibmall_bff.controller;

import com.serendibmall.order.v1.CreateOrderRequest;
import com.serendibmall.order.v1.CreateOrderResponse;
import com.serendibmall.order.v1.OrderServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class OrderGraphqlController {

    private final OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub;

    public OrderGraphqlController(@Qualifier("orderServiceChannel") ManagedChannel channel) {
        this.orderServiceStub = OrderServiceGrpc.newBlockingStub(channel);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Order createOrder(@Argument String productId, @Argument Integer quantity,
                             @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Creating order for user {} ({}): product={}, qty={}", username, userId, productId, quantity);

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
