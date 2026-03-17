package com.example.OrderService.kafka.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentEvent {
    private BigDecimal paymentAmount;
    private Long userId;
    private Long orderId;
    private String paymentStatus;
}
