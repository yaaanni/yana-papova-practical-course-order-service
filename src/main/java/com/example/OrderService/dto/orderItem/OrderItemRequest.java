package com.example.OrderService.dto.orderItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "Quantity is required")
    @PositiveOrZero
    private Integer quantity;

    @NotNull(message = "Item ID is required")
    private Long itemId;
}
