package com.example.OrderService.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class OrderUpdateRequest {

    @NotBlank(message = "Order is required")
    private String status;
}
