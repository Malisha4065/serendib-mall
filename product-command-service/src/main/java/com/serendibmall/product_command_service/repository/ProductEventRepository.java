package com.serendibmall.product_command_service.repository;

import com.serendibmall.product_command_service.entity.ProductEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductEventRepository extends JpaRepository<ProductEventEntity, Long> {
    
    List<ProductEventEntity> findByAggregateIdOrderByCreatedAtAsc(UUID aggregateId);
}
