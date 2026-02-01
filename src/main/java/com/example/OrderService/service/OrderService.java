package com.example.OrderService.service;

import com.example.OrderService.client.UserClient;
import com.example.OrderService.dto.order.OrderRequest;
import com.example.OrderService.dto.order.OrderResponse;
import com.example.OrderService.dto.order.OrderUpdateRequest;
import com.example.OrderService.dto.user.UserResponse;
import com.example.OrderService.entity.Item;
import com.example.OrderService.entity.Order;
import com.example.OrderService.entity.OrderItem;
import com.example.OrderService.enums.Status;
import com.example.OrderService.exception.ItemNotFoundException;
import com.example.OrderService.exception.OrderNotFoundException;
import com.example.OrderService.exception.UserNotFoundException;
import com.example.OrderService.mapper.OrderItemMapper;
import com.example.OrderService.mapper.OrderMapper;
import com.example.OrderService.repository.ItemRepository;
import com.example.OrderService.repository.OrderRepository;
import com.example.OrderService.security.model.AuthUser;
import com.example.OrderService.specification.OrderSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final EntityManager entityManager;
    private final UserClient userClient;

    public OrderResponse create(OrderRequest request, AuthUser authUser) {
        Long userId = authUser.getUserId();

        UserResponse user = userClient.getUserById(userId);

        if (user.getName() == null) {
            throw new UserNotFoundException(userId);
        }

        Order order = orderMapper.toEntity(request);

        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItem orderItem = orderItemMapper.toEntity(itemRequest);

                    Item item = itemRepository.findById(itemRequest.getItemId())
                            .orElseThrow(() -> new ItemNotFoundException(itemRequest.getItemId()));

                    orderItem.setOrder(order);
                    orderItem.setItem(item);

                    return orderItem;
                })
                .toList();

        order.setUserId(userId);
        order.setItems(items);

        BigDecimal totalPrice = items.stream()
                .map(i -> i.getItem().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);

        OrderResponse response = orderMapper.toResponse(saved);

        response.setUser(user);

        return response;
    }

    public OrderResponse getOrderById(Long id, AuthUser authUser) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getDeleted()) {
            throw new OrderNotFoundException(id);
        }

        if (authUser.getRole().equals("ROLE_USER") && !authUser.getUserId().equals(order.getUserId())) {
            throw new AccessDeniedException("You do not have permission to access these orders");
        }

        OrderResponse response = orderMapper.toResponse(order);

        UserResponse user = userClient.getUserById(order.getUserId());

        response.setUser(user);

        return response;
    }

    public Page<OrderResponse> findAll(Integer page, Integer size, List<Status> statuses, LocalDateTime start, LocalDateTime end) {

        Specification<Order> spec =
                Specification.where(OrderSpecifications.notDeleted())
                        .and(OrderSpecifications.hasStatuses(statuses))
                        .and(OrderSpecifications.createdAfter(start))
                        .and(OrderSpecifications.createdBefore(end));

        Pageable pageable = PageRequest.of(page, size);

        return orderRepository.findAll(spec, pageable)
                .map(order -> {
                    OrderResponse response = orderMapper.toResponse(order);

                    UserResponse user = userClient.getUserById(order.getUserId());

                    response.setUser(user);

                    return response;
                });
    }

    public List<OrderResponse> getOrdersByUserId(Long id, AuthUser authUser) {
        if (authUser.getRole().equals("ROLE_USER") && !authUser.getUserId().equals(id)) {
            throw new AccessDeniedException("You do not have permission to access these orders");
        }

        UserResponse user = userClient.getUserById(id);

        if (user.getName() == null) {
            throw new UserNotFoundException(id);
        }

        return orderRepository.findAllByUserIdAndDeletedFalse(id)
                .stream()
                .map(order -> {
                    OrderResponse response = orderMapper.toResponse(order);
                    response.setUser(user);
                    return response;
                })
                .toList();
    }

    @Transactional
    public OrderResponse update(OrderUpdateRequest request, Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getDeleted()) {
            throw new OrderNotFoundException(id);
        }

        orderMapper.updateOrderFromRequest(request, order);

        Order updated = orderRepository.save(order);
        entityManager.flush();
        entityManager.refresh(updated);

        OrderResponse response = orderMapper.toResponse(updated);

        UserResponse user = userClient.getUserById(updated.getUserId());

        response.setUser(user);

        return response;
    }

    @Transactional
    public void deleteById(Long id, AuthUser authUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getDeleted()) {
            throw new OrderNotFoundException(id);
        }


        if (authUser.getRole().equals("ROLE_USER") && !order.getUserId().equals(authUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }

        if (authUser.getRole().equals("ROLE_USER") && order.getStatus() != Status.CREATED) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }

        orderRepository.softDelete(id);
    }
}