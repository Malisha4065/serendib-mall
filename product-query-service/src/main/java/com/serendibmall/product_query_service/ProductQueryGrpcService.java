package com.serendibmall.product_query_service;

import com.serendibmall.product.v1.GetProductRequest;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import com.serendibmall.product.v1.SearchProductsRequest;
import com.serendibmall.product.v1.SearchProductsResponse;
import io.grpc.stub.StreamObserver;
import com.serendibmall.product_query_service.document.ProductDocument;
import com.serendibmall.product_query_service.repository.ProductRepository;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public void searchProducts(SearchProductsRequest request, StreamObserver<SearchProductsResponse> responseObserver) {
        String query = request.getQuery();
        int page = Math.max(0, request.getPage());
        int size = request.getSize() > 0 ? request.getSize() : 20;

        log.info("Searching products: query='{}', page={}, size={}", query, page, size);

        try {
            Page<ProductDocument> productPage;

            if (query == null || query.isBlank()) {
                // Return all products when no query
                productPage = productRepository.findAll(PageRequest.of(page, size));
            } else {
                // Search by name or description
                productPage = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query, query, PageRequest.of(page, size));
            }

            SearchProductsResponse.Builder responseBuilder = SearchProductsResponse.newBuilder()
                    .setTotalCount((int) productPage.getTotalElements())
                    .setTotalPages(productPage.getTotalPages());

            for (ProductDocument product : productPage.getContent()) {
                responseBuilder.addProducts(ProductResponse.newBuilder()
                        .setId(product.getId())
                        .setName(product.getName() != null ? product.getName() : "")
                        .setDescription(product.getDescription() != null ? product.getDescription() : "")
                        .setPrice(product.getPrice() != null ? product.getPrice() : 0.0)
                        .setCurrency(product.getCurrency() != null ? product.getCurrency() : "USD")
                        .setCategory(product.getCategory() != null ? product.getCategory() : "")
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error searching products", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error searching products: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}

