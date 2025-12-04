package com.serendibmall.inventory_service;

import com.serendibmall.inventory.v1.GetStockRequest;
import com.serendibmall.inventory.v1.GetBatchStockRequest;
import com.serendibmall.inventory.v1.BatchStockResponse;
import com.serendibmall.inventory.v1.InventoryServiceGrpc;
import com.serendibmall.inventory.v1.StockResponse;
import com.serendibmall.inventory_service.entity.Inventory;
import com.serendibmall.inventory_service.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    @Override
    public void getStock(GetStockRequest request, StreamObserver<StockResponse> responseObserver) {
        String productId = request.getProductId();
        log.info("Getting stock for product: {}", productId);

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

        StockResponse response;
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            response = StockResponse.newBuilder()
                    .setProductId(productId)
                    .setQuantity(inventory.getQuantity())
                    .setIsAvailable(inventory.getQuantity() > 0)
                    .build();
        } else {
            // Product not found in inventory
            response = StockResponse.newBuilder()
                    .setProductId(productId)
                    .setQuantity(0)
                    .setIsAvailable(false)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBatchStock(GetBatchStockRequest request, StreamObserver<BatchStockResponse> responseObserver) {
        log.info("Getting batch stock for {} products", request.getProductIdsCount());

        BatchStockResponse.Builder batchBuilder = BatchStockResponse.newBuilder();

        for (String productId : request.getProductIdsList()) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

            StockResponse stockResponse;
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                stockResponse = StockResponse.newBuilder()
                        .setProductId(productId)
                        .setQuantity(inventory.getQuantity())
                        .setIsAvailable(inventory.getQuantity() > 0)
                        .build();
            } else {
                stockResponse = StockResponse.newBuilder()
                        .setProductId(productId)
                        .setQuantity(0)
                        .setIsAvailable(false)
                        .build();
            }
            batchBuilder.addStocks(stockResponse);
        }

        responseObserver.onNext(batchBuilder.build());
        responseObserver.onCompleted();
    }
}
