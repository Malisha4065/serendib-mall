package com.serendibmall.inventory_service;

import com.serendibmall.inventory.v1.*;
import com.serendibmall.inventory_service.entity.Inventory;
import com.serendibmall.inventory_service.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    @Override
    public void getStock(GetStockRequest request, StreamObserver<StockResponse> responseObserver) {
        String productId = request.getProductId();
        log.info("Getting stock for product: {}", productId);

        StockResponse response = buildStockResponse(productId);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBatchStock(GetBatchStockRequest request, StreamObserver<BatchStockResponse> responseObserver) {
        log.info("Getting batch stock for {} products", request.getProductIdsCount());

        BatchStockResponse.Builder batchBuilder = BatchStockResponse.newBuilder();

        for (String productId : request.getProductIdsList()) {
            batchBuilder.addStocks(buildStockResponse(productId));
        }

        responseObserver.onNext(batchBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void setStock(SetStockRequest request, StreamObserver<StockResponse> responseObserver) {
        String productId = request.getProductId();
        int quantity = request.getQuantity();
        log.info("Setting stock for product {}: {}", productId, quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(Inventory.builder()
                        .id(UUID.randomUUID().toString())
                        .productId(productId)
                        .build());

        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);

        responseObserver.onNext(buildStockResponse(productId, quantity));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateStock(UpdateStockRequest request, StreamObserver<StockResponse> responseObserver) {
        String productId = request.getProductId();
        int delta = request.getDelta();
        log.info("Updating stock for product {} by delta: {}", productId, delta);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(Inventory.builder()
                        .id(UUID.randomUUID().toString())
                        .productId(productId)
                        .quantity(0)
                        .build());

        int newQuantity = Math.max(0, inventory.getQuantity() + delta);
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        responseObserver.onNext(buildStockResponse(productId, newQuantity));
        responseObserver.onCompleted();
    }

    @Override
    public void listInventory(ListInventoryRequest request, StreamObserver<ListInventoryResponse> responseObserver) {
        int page = Math.max(0, request.getPage());
        int size = request.getSize() > 0 ? request.getSize() : 20;
        log.info("Listing inventory: page={}, size={}", page, size);

        Page<Inventory> inventoryPage = inventoryRepository.findAll(PageRequest.of(page, size));

        ListInventoryResponse.Builder responseBuilder = ListInventoryResponse.newBuilder()
                .setTotalCount((int) inventoryPage.getTotalElements());

        for (Inventory inv : inventoryPage.getContent()) {
            responseBuilder.addItems(buildStockResponse(inv.getProductId(), inv.getQuantity()));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private StockResponse buildStockResponse(String productId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            return buildStockResponse(productId, inventory.getQuantity());
        }
        return StockResponse.newBuilder()
                .setProductId(productId)
                .setQuantity(0)
                .setIsAvailable(false)
                .build();
    }

    private StockResponse buildStockResponse(String productId, int quantity) {
        return StockResponse.newBuilder()
                .setProductId(productId)
                .setQuantity(quantity)
                .setIsAvailable(quantity > 0)
                .build();
    }
}
