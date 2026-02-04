package com.example.OrderService.mapper;

import com.example.OrderService.dto.order.OrderRequest;
import com.example.OrderService.dto.order.OrderResponse;
import com.example.OrderService.dto.order.OrderUpdateRequest;
import com.example.OrderService.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    Order toEntity(OrderRequest request);

    @Mapping(target = "user", ignore = true)
    OrderResponse toResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    void updateOrderFromRequest(OrderUpdateRequest request, @MappingTarget Order order);
}
