package com.serendibmall.product_query_service;

import com.serendibmall.product.v1.GetProductRequest;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class ProductQueryGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        // Hardcoded Dummy Product
        ProductResponse response = ProductResponse.newBuilder()
                .setId("123")
                .setName("Test Phone")
                .setDescription("A dummy product for testing")
                .setPrice(999.00)
                .setCurrency("USD")
                .setCategory("Electronics")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
