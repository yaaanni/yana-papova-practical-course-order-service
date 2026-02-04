package com.example.OrderService.repository;

import com.example.OrderService.entity.Order;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    List<Order> findAllByUserIdAndDeletedFalse(Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE orders SET deleted = true, status = 'CANCELLED' WHERE id = :id", nativeQuery = true)
    void softDelete(@Param("id") Long id);
}
