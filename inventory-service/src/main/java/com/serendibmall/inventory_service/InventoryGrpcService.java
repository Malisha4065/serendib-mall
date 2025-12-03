package com.serendibmall.inventory_service;

import com.serendibmall.inventory.v1.GetStockRequest;
import com.serendibmall.inventory.v1.InventoryServiceGrpc;
import com.serendibmall.inventory.v1.StockResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    @Override
    public void getStock(GetStockRequest request, StreamObserver<StockResponse> responseObserver) {
        // Hardcoded Dummy Stock
        StockResponse response = StockResponse.newBuilder()
                .setProductId(request.getProductId())
                .setQuantity(10)
                .setIsAvailable(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
