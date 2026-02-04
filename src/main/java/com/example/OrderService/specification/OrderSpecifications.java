package com.example.OrderService.specification;

import com.example.OrderService.entity.Order;
import com.example.OrderService.enums.Status;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecifications {

    public static Specification<Order> hasStatuses(List<Status> statuses) {
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty()
                        ? null
                        : root.get("status").in(statuses);
    }

    public static Specification<Order> createdAfter(LocalDateTime start) {
        return ((root, query, cb) ->
                start == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), start));
    }

    public static Specification<Order> createdBefore(LocalDateTime end) {
        return ((root, query, cb) ->
                end == null
                        ? null
                        : cb.lessThanOrEqualTo(root.get("createdAt"), end));
    }

    public static Specification<Order> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }
}
