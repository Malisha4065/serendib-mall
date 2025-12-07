package com.serendibmall.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOutbox {
    @Id
    private String id;
    
    @Column(name = "aggregate_type")
    private String aggregateType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "event_type")
    private String eventType;
    
    private String payload;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

