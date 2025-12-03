package com.serendibmall.serendibmall_bff.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel productServiceChannel(GrpcChannelFactory factory) {
        return factory.createChannel("product-service");
    }

    @Bean
    public ManagedChannel inventoryServiceChannel(GrpcChannelFactory factory) {
        return factory.createChannel("inventory-service");
    }
}
