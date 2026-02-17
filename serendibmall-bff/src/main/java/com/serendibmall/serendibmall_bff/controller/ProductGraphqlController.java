package com.serendibmall.serendibmall_bff.controller;

import com.serendibmall.inventory.v1.GetStockRequest;
import com.serendibmall.inventory.v1.InventoryServiceGrpc;
import com.serendibmall.inventory.v1.SetStockRequest;
import com.serendibmall.inventory.v1.StockResponse;
import com.serendibmall.product.v1.CreateProductRequest;
import com.serendibmall.product.v1.CreateProductResponse;
import com.serendibmall.product.v1.GetProductRequest;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import com.serendibmall.product.v1.SearchProductsRequest;
import com.serendibmall.product.v1.SearchProductsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class ProductGraphqlController {

    private final ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;
    private final ProductServiceGrpc.ProductServiceBlockingStub productCommandServiceStub;
    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    public ProductGraphqlController(
            @Qualifier("productServiceChannel") ManagedChannel productServiceChannel,
            @Qualifier("productCommandServiceChannel") ManagedChannel productCommandServiceChannel,
            @Qualifier("inventoryServiceChannel") ManagedChannel inventoryServiceChannel) {
        this.productServiceStub = ProductServiceGrpc.newBlockingStub(productServiceChannel);
        this.productCommandServiceStub = ProductServiceGrpc.newBlockingStub(productCommandServiceChannel);
        this.inventoryServiceStub = InventoryServiceGrpc.newBlockingStub(inventoryServiceChannel);
    }

    @QueryMapping
    @CircuitBreaker(name = "product-service", fallbackMethod = "productFallback")
    public ProductDetails product(@Argument String id) {
        GetProductRequest request = GetProductRequest.newBuilder()
                .setProductId(id)
                .build();
        
        ProductResponse response = productServiceStub.getProduct(request);
        
        return new ProductDetails(
                response.getId(),
                response.getName(),
                response.getDescription(),
                response.getPrice(),
                response.getCurrency(),
                response.getCategory()
        );
    }

    private ProductDetails productFallback(String id, Exception ex) {
        return new ProductDetails(id, "Service Unavailable", "Please try again later", 0.0, "USD", "");
    }

    @QueryMapping
    @CircuitBreaker(name = "product-service", fallbackMethod = "productsFallback")
    public ProductSearchResult products(@Argument String query, @Argument Integer page, @Argument Integer size) {
        SearchProductsRequest.Builder requestBuilder = SearchProductsRequest.newBuilder();
        
        if (query != null) requestBuilder.setQuery(query);
        requestBuilder.setPage(page != null ? page : 0);
        requestBuilder.setSize(size != null ? size : 20);

        SearchProductsResponse response = productServiceStub.searchProducts(requestBuilder.build());

        List<ProductDetails> productList = response.getProductsList().stream()
                .map(p -> new ProductDetails(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getCurrency(),
                        p.getCategory()))
                .collect(Collectors.toList());

        return new ProductSearchResult(productList, response.getTotalCount(), response.getTotalPages());
    }

    private ProductSearchResult productsFallback(String query, Integer page, Integer size, Exception ex) {
        return new ProductSearchResult(Collections.emptyList(), 0, 0);
    }

    @SchemaMapping(typeName = "ProductDetails")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "stockLevelFallback")
    public String stockLevel(ProductDetails product) {
        GetStockRequest request = GetStockRequest.newBuilder()
                .setProductId(product.id())
                .build();
        
        StockResponse response = inventoryServiceStub.getStock(request);
        
        return response.getIsAvailable() ? "IN_STOCK" : "OUT_OF_STOCK";
    }

    private String stockLevelFallback(ProductDetails product, Exception ex) {
        return "UNKNOWN";
    }

    @MutationMapping
    @CircuitBreaker(name = "product-service", fallbackMethod = "createProductFallback")
    public ProductDetails createProduct(@Argument Map<String, Object> input) {
        CreateProductRequest request = CreateProductRequest.newBuilder()
                .setName((String) input.get("name"))
                .setDescription(input.getOrDefault("description", "").toString())
                .setPrice(Double.parseDouble(input.get("price").toString()))
                .setCurrency(input.getOrDefault("currency", "USD").toString())
                .setCategory(input.getOrDefault("category", "").toString())
                .build();

        CreateProductResponse response = productCommandServiceStub.createProduct(request);

        String productId = response.getProductId();

        // Set initial stock if provided
        Object initialStockObj = input.get("initialStock");
        if (initialStockObj != null) {
            int initialStock = Integer.parseInt(initialStockObj.toString());
            if (initialStock > 0) {
                try {
                    inventoryServiceStub.setStock(
                            SetStockRequest.newBuilder()
                                    .setProductId(productId)
                                    .setQuantity(initialStock)
                                    .build());
                    log.info("Set initial stock for product {}: {}", productId, initialStock);
                } catch (Exception e) {
                    log.warn("Failed to set initial stock for product {}: {}", productId, e.getMessage());
                }
            }
        }

        return new ProductDetails(
                productId,
                (String) input.get("name"),
                input.getOrDefault("description", "").toString(),
                Double.parseDouble(input.get("price").toString()),
                input.getOrDefault("currency", "USD").toString(),
                input.getOrDefault("category", "").toString()
        );
    }

    private ProductDetails createProductFallback(Map<String, Object> input, Exception ex) {
        throw new RuntimeException("Product creation failed: " + ex.getMessage(), ex);
    }

    public record ProductDetails(String id, String name, String description, double price, String currency, String category) {}
    public record ProductSearchResult(List<ProductDetails> products, int totalCount, int totalPages) {}
}
