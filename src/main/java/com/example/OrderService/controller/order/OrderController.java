package com.example.OrderService.controller.order;

import com.example.OrderService.dto.order.OrderRequest;
import com.example.OrderService.dto.order.OrderResponse;
import com.example.OrderService.dto.order.OrderUpdateRequest;
import com.example.OrderService.enums.Status;
import com.example.OrderService.security.model.AuthUser;
import com.example.OrderService.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request, @AuthenticationPrincipal AuthUser authUser) {
        OrderResponse response = orderService.create(request, authUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {
        OrderResponse response = orderService.getOrderById(id, authUser);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(@RequestParam Integer page, @RequestParam Integer size,
                                                         @RequestParam(required = false) List<Status> statuses,
                                                         @RequestParam(required = false) LocalDateTime start,
                                                         @RequestParam(required = false) LocalDateTime end) {
        Page<OrderResponse> orders = orderService.findAll(page, size, statuses, start, end);
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @GetMapping("/{id}/all")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {
        List<OrderResponse> response = orderService.getOrdersByUserId(id, authUser);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> update(@Valid @RequestBody OrderUpdateRequest request, @PathVariable Long id) {
        OrderResponse response = orderService.update(request, id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {
        orderService.deleteById(id, authUser);
        return ResponseEntity.noContent().build();
    }
}