package com.serendibmall.serendibmall_bff.controller;

import com.serendibmall.inventory.v1.GetStockRequest;
import com.serendibmall.inventory.v1.InventoryServiceGrpc;
import com.serendibmall.inventory.v1.StockResponse;
import com.serendibmall.product.v1.GetProductRequest;
import com.serendibmall.product.v1.ProductResponse;
import com.serendibmall.product.v1.ProductServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductGraphqlController {

    private final ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;
    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    public ProductGraphqlController(
            @Qualifier("productServiceChannel") ManagedChannel productServiceChannel,
            @Qualifier("inventoryServiceChannel") ManagedChannel inventoryServiceChannel) {
        this.productServiceStub = ProductServiceGrpc.newBlockingStub(productServiceChannel);
        this.inventoryServiceStub = InventoryServiceGrpc.newBlockingStub(inventoryServiceChannel);
    }

    @QueryMapping
    public ProductDetails product(@Argument String id) {
        GetProductRequest request = GetProductRequest.newBuilder()
                .setProductId(id)
                .build();
        
        ProductResponse response = productServiceStub.getProduct(request);
        
        return new ProductDetails(
                response.getId(),
                response.getName(),
                response.getDescription(),
                response.getPrice()
        );
    }

    @SchemaMapping(typeName = "ProductDetails")
    public String stockLevel(ProductDetails product) {
        GetStockRequest request = GetStockRequest.newBuilder()
                .setProductId(product.id())
                .build();
        
        StockResponse response = inventoryServiceStub.getStock(request);
        
        return response.getIsAvailable() ? "IN_STOCK" : "OUT_OF_STOCK";
    }

    public record ProductDetails(String id, String name, String description, double price) {}
}
