package com.example.OrderService.dto.order;

import com.example.OrderService.dto.orderItem.OrderItemRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "User ID is required")
    @PositiveOrZero
    private Long userId;

    @NotEmpty(message = "Items list cannot be empty")
    private List<OrderItemRequest> items;
}
