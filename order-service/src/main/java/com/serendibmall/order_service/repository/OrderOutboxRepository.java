package com.serendibmall.order_service.repository;

import com.serendibmall.order_service.entity.OrderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, String> {
}
