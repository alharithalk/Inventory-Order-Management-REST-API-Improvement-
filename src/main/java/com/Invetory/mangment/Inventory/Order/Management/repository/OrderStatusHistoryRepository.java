package com.Invetory.mangment.Inventory.Order.Management.repository;

import com.Invetory.mangment.Inventory.Order.Management.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderOrdId(Long ordId);
}
