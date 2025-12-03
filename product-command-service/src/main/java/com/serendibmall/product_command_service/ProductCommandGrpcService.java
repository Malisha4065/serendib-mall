package com.serendibmall.product_command_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendibmall.product.v1.CreateProductRequest;
import com.serendibmall.product.v1.CreateProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import com.serendibmall.product_command_service.entity.ProductEventEntity;
import com.serendibmall.product_command_service.repository.ProductEventRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductCommandGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductEventRepository productEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void createProduct(CreateProductRequest request, StreamObserver<CreateProductResponse> responseObserver) {
        try {
            // Generate new product ID
            UUID productId = UUID.randomUUID();
            log.info("Creating product with ID: {}", productId);

            // Build event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("product_id", productId.toString());
            eventPayload.put("name", request.getName());
            eventPayload.put("description", request.getDescription());
            eventPayload.put("price", request.getPrice());
            eventPayload.put("currency", request.getCurrency().isEmpty() ? "USD" : request.getCurrency());
            eventPayload.put("category", request.getCategory());

            // Serialize to JSON
            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            // Create and save event
            ProductEventEntity event = ProductEventEntity.builder()
                    .aggregateId(productId)
                    .eventType("ProductCreated")
                    .payload(payloadJson)
                    .build();

            productEventRepository.save(event);

            log.info("Product event saved: {}", event.getId());

            // Return response
            CreateProductResponse response = CreateProductResponse.newBuilder()
                    .setProductId(productId.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error creating product", e);
            responseObserver.onError(e);
        }
    }
}
