package com.serendibmall.serendibmall_bff.controller;

import com.serendibmall.inventory.v1.*;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import com.serendibmall.product.v1.SearchProductsRequest;
import com.serendibmall.product.v1.SearchProductsResponse;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class InventoryGraphqlController {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;
    private final ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;

    public InventoryGraphqlController(
            @Qualifier("inventoryServiceChannel") ManagedChannel inventoryServiceChannel,
            @Qualifier("productServiceChannel") ManagedChannel productServiceChannel) {
        this.inventoryServiceStub = InventoryServiceGrpc.newBlockingStub(inventoryServiceChannel);
        this.productServiceStub = ProductServiceGrpc.newBlockingStub(productServiceChannel);
    }

    @QueryMapping
    public InventoryPage inventory(@Argument Integer page, @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        ListInventoryResponse response = inventoryServiceStub.listInventory(
                ListInventoryRequest.newBuilder()
                        .setPage(pageNum)
                        .setSize(pageSize)
                        .build());

        // Collect product IDs to fetch names in batch
        List<String> productIds = response.getItemsList().stream()
                .map(StockResponse::getProductId)
                .collect(Collectors.toList());

        // Fetch product names from product-query-service
        Map<String, String> productNames = fetchProductNames(productIds);

        List<InventoryItem> items = response.getItemsList().stream()
                .map(stock -> new InventoryItem(
                        stock.getProductId(),
                        productNames.getOrDefault(stock.getProductId(), "Unknown Product"),
                        stock.getQuantity(),
                        stock.getIsAvailable()))
                .collect(Collectors.toList());

        return new InventoryPage(items, response.getTotalCount());
    }

    @MutationMapping
    public InventoryItem setStock(@Argument String productId, @Argument Integer quantity) {
        log.info("Setting stock for product {}: {}", productId, quantity);

        StockResponse response = inventoryServiceStub.setStock(
                SetStockRequest.newBuilder()
                        .setProductId(productId)
                        .setQuantity(quantity)
                        .build());

        String productName = fetchProductName(productId);

        return new InventoryItem(
                response.getProductId(),
                productName,
                response.getQuantity(),
                response.getIsAvailable());
    }

    @MutationMapping
    public InventoryItem updateStock(@Argument String productId, @Argument Integer delta) {
        log.info("Updating stock for product {} by delta: {}", productId, delta);

        StockResponse response = inventoryServiceStub.updateStock(
                UpdateStockRequest.newBuilder()
                        .setProductId(productId)
                        .setDelta(delta)
                        .build());

        String productName = fetchProductName(productId);

        return new InventoryItem(
                response.getProductId(),
                productName,
                response.getQuantity(),
                response.getIsAvailable());
    }

    private Map<String, String> fetchProductNames(List<String> productIds) {
        try {
            // Use search to get all products, then map by ID
            SearchProductsResponse searchResponse = productServiceStub.searchProducts(
                    SearchProductsRequest.newBuilder()
                            .setQuery("")
                            .setPage(0)
                            .setSize(100)
                            .build());

            return searchResponse.getProductsList().stream()
                    .collect(Collectors.toMap(
                            ProductResponse::getId,
                            ProductResponse::getName,
                            (a, b) -> a));
        } catch (StatusRuntimeException e) {
            log.warn("Failed to fetch product names: {}", e.getMessage());
            return Map.of();
        }
    }

    private String fetchProductName(String productId) {
        try {
            var response = productServiceStub.getProduct(
                    com.serendibmall.product.v1.GetProductRequest.newBuilder()
                            .setProductId(productId)
                            .build());
            return response.getName();
        } catch (StatusRuntimeException e) {
            log.warn("Failed to fetch product name for {}: {}", productId, e.getMessage());
            return "Unknown Product";
        }
    }

    public record InventoryItem(String productId, String productName, int quantity, boolean isAvailable) {}
    public record InventoryPage(List<InventoryItem> items, int totalCount) {}
}
