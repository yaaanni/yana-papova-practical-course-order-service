package com.example.OrderService.kafka.consumer;

import com.example.OrderService.dto.order.OrderUpdateRequest;
import com.example.OrderService.entity.Order;
import com.example.OrderService.enums.Status;
import com.example.OrderService.exception.OrderAccessDeniedException;
import com.example.OrderService.exception.OrderNotFoundException;
import com.example.OrderService.exception.PaymentAmountMismatchException;
import com.example.OrderService.exception.UserNotFoundException;
import com.example.OrderService.kafka.dto.PaymentEvent;
import com.example.OrderService.repository.OrderRepository;
import com.example.OrderService.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    //new

    @KafkaListener(topics = "CREATE_PAYMENT", groupId = "${spring.kafka.consumer.group-id}")
    public void handle(PaymentEvent paymentEvent) {
        Order order = orderRepository.findById(paymentEvent.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + paymentEvent.getOrderId() + " not found"));

        if (Status.PAID.equals(order.getStatus())) {
            return;
        }

        if (!order.getUserId().equals(paymentEvent.getUserId())) {
            throw new OrderAccessDeniedException("User with id: " + paymentEvent.getUserId() + " does not own the order with id: " + paymentEvent.getOrderId());
        }

        if (!order.getTotalPrice().equals(paymentEvent.getPaymentAmount())) {
            throw new PaymentAmountMismatchException("The provided payment amount is invalid for order with id: " + paymentEvent.getOrderId());
        }

        OrderUpdateRequest request = new OrderUpdateRequest();

        if("SUCCESS".equals(paymentEvent.getPaymentStatus())){
            request.setStatus(Status.PAID.name());
            orderService.update(request, paymentEvent.getOrderId());
        }
    }

    //new

//    @KafkaListener(topics = "CREATE_PAYMENT", groupId = "${spring.kafka.consumer.group-id}")
//    public void handle(PaymentEvent paymentEvent) {
//        Order order = orderRepository.findById(paymentEvent.getOrderId())
//                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + paymentEvent.getOrderId() + " not found"));
//
//        if (!order.getUserId().equals(paymentEvent.getUserId())) {
//            throw new OrderAccessDeniedException("User with id: " + paymentEvent.getUserId() + " does not own the order with id: " + paymentEvent.getOrderId());
//        }
//
//        if (!order.getTotalPrice().equals(paymentEvent.getPaymentAmount())) {
//            throw new PaymentAmountMismatchException("The provided payment amount is invalid for order with id: " + paymentEvent.getOrderId());
//        }
//
//        OrderUpdateRequest request = new OrderUpdateRequest();
//
//        if(paymentEvent.getPaymentStatus().equals("SUCCESS")){
//            request.setStatus(Status.PAID.name());
//            orderService.update(request, paymentEvent.getOrderId());
//        }
//    }
}
