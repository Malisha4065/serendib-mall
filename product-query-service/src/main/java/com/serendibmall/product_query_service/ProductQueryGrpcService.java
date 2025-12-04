package com.serendibmall.product_query_service;

import com.serendibmall.product.v1.GetProductRequest;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import io.grpc.stub.StreamObserver;
import com.serendibmall.product_query_service.document.ProductDocument;
import com.serendibmall.product_query_service.repository.ProductRepository;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductQueryGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository productRepository;

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        String productId = request.getProductId();
        log.info("Fetching product with ID: {}", productId);

        Optional<ProductDocument> productOptional = productRepository.findById(productId);

        if (productOptional.isPresent()) {
            ProductDocument product = productOptional.get();
            ProductResponse response = ProductResponse.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setDescription(product.getDescription())
                    .setPrice(product.getPrice())
                    .setCurrency(product.getCurrency())
                    .setCategory(product.getCategory())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            log.warn("Product not found with ID: {}", productId);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Product not found with ID: " + productId)
                    .asRuntimeException());
        }
    }
}
