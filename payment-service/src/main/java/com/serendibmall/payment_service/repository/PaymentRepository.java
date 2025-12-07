package com.serendibmall.payment_service.repository;

import com.serendibmall.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    boolean existsByOrderId(String orderId);
}
