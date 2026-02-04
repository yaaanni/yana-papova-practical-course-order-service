package com.example.OrderService.service.order;

import com.example.OrderService.client.UserClient;
import com.example.OrderService.dto.order.OrderRequest;
import com.example.OrderService.dto.order.OrderResponse;
import com.example.OrderService.dto.order.OrderUpdateRequest;
import com.example.OrderService.dto.orderItem.OrderItemRequest;
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
import com.example.OrderService.service.OrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserClient userClient;

    @Mock
    private EntityManager entityManager;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldSaveOrder() {
        Long userId = 1L;

        OrderRequest request = new OrderRequest();

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setItemId(10L);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        AuthUser authUser = new AuthUser(userId, "USER");

        UserResponse userResponse = new UserResponse();
        userResponse.setName("Nastya");

        Order order = new Order();
        order.setUserId(userId);

        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(2);

        Item item = new Item();
        item.setId(10L);
        item.setPrice(BigDecimal.valueOf(100));

        Order savedOrder = new Order();
        savedOrder.setId(5L);
        savedOrder.setUserId(userId);
        savedOrder.setTotalPrice(BigDecimal.valueOf(200));

        OrderResponse mappedResponse = new OrderResponse();
        mappedResponse.setId(5L);

        when(userClient.getUserById(userId)).thenReturn(userResponse);
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(orderItemMapper.toEntity(itemReq)).thenReturn(orderItem);
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(mappedResponse);

        OrderResponse response = orderService.create(request, authUser);

        assertEquals(5L, response.getId());
        assertEquals(userResponse, response.getUser());

        verify(userClient).getUserById(userId);
        verify(orderRepository).save(order);
        verify(itemRepository).findById(10L);
    }

    @Test
    public void create_shouldThrowUserNotFoundException() {
        Long userId = 1L;

        OrderRequest request = new OrderRequest();

        AuthUser authUser = new AuthUser(userId, "USER");

        UserResponse response = new UserResponse();

        when(userClient.getUserById(userId)).thenReturn(response);

        assertThrows(UserNotFoundException.class,
                () -> orderService.create(request, authUser));

        verify(userClient).getUserById(userId);
        verifyNoInteractions(orderMapper);
    }

    @Test
    public void create_shouldThrowItemNotFoundException() {
        Long userId = 1L;

        OrderRequest request = new OrderRequest();

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setItemId(10L);
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));

        AuthUser authUser = new AuthUser(userId, "USER");

        UserResponse response = new UserResponse();
        response.setName("Nastya");

        Order order = new Order();

        when(userClient.getUserById(userId)).thenReturn(response);
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> orderService.create(request, authUser));

        verify(userClient).getUserById(userId);
    }

    @Test
    void getOrderById_success() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setDeleted(false);

        OrderResponse mapped = new OrderResponse();
        mapped.setId(orderId);

        UserResponse user = new UserResponse();
        user.setName("Nastya");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(mapped);
        when(userClient.getUserById(10L)).thenReturn(user);

        OrderResponse response = orderService.getOrderById(orderId, authUser);

        assertEquals(orderId, response.getId());
        assertEquals(user, response.getUser());

        verify(orderRepository).findById(orderId);
        verify(orderMapper).toResponse(order);
        verify(userClient).getUserById(10L);
    }

    @Test
    void getOrderById_shouldThrowOrderNotFound() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId, authUser));

        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrderById_shouldThrowWhenDeleted() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setDeleted(true);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId, authUser));

        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrderById_shouldThrowAccessDenied_whenUserNotOwner() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(99L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setDeleted(false);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.getOrderById(orderId, authUser));

        verify(orderRepository).findById(orderId);
    }

    @Test
    void findAll_success() {
        Integer page = 0;
        Integer size = 10;

        List<Status> statuses = List.of(Status.CREATED);
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();

        Order order = new Order();
        order.setId(1L);
        order.setUserId(100L);

        Page<Order> orderPage = new PageImpl<>(List.of(order));

        OrderResponse mapped = new OrderResponse();
        mapped.setId(1L);

        UserResponse user = new UserResponse();
        user.setName("Nastya");

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        when(orderMapper.toResponse(order)).thenReturn(mapped);
        when(userClient.getUserById(100L)).thenReturn(user);

        Page<OrderResponse> result = orderService.findAll(page, size, statuses, start, end);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(user, result.getContent().get(0).getUser());

        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(orderMapper).toResponse(order);
        verify(userClient).getUserById(100L);
    }

    @Test
    void getOrdersByUserId_success() {
        Long userId = 1L;

        AuthUser authUser = new AuthUser(userId, "ROLE_USER");

        UserResponse user = new UserResponse();
        user.setName("Nastya");

        Order order = new Order();
        order.setId(10L);
        order.setUserId(userId);
        order.setDeleted(false);

        OrderResponse mapped = new OrderResponse();
        mapped.setId(10L);

        when(userClient.getUserById(userId)).thenReturn(user);
        when(orderRepository.findAllByUserIdAndDeletedFalse(userId))
                .thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(mapped);

        List<OrderResponse> result = orderService.getOrdersByUserId(userId, authUser);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(user, result.get(0).getUser());

        verify(userClient).getUserById(userId);
        verify(orderRepository).findAllByUserIdAndDeletedFalse(userId);
        verify(orderMapper).toResponse(order);
    }

    @Test
    void getOrdersByUserId_shouldThrowAccessDenied() {
        Long userId = 1L;

        AuthUser authUser = new AuthUser(99L, "ROLE_USER");

        assertThrows(AccessDeniedException.class,
                () -> orderService.getOrdersByUserId(userId, authUser));
    }

    @Test
    void getOrdersByUserId_shouldThrowUserNotFound() {
        Long userId = 1L;

        AuthUser authUser = new AuthUser(userId, "ROLE_USER");

        UserResponse empty = new UserResponse();

        when(userClient.getUserById(userId)).thenReturn(empty);

        assertThrows(UserNotFoundException.class,
                () -> orderService.getOrdersByUserId(userId, authUser));

        verify(userClient).getUserById(userId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void update_success() {
        Long orderId = 1L;

        OrderUpdateRequest request = new OrderUpdateRequest();

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setDeleted(false);

        Order updated = new Order();
        updated.setId(orderId);
        updated.setUserId(10L);

        OrderResponse mapped = new OrderResponse();
        mapped.setId(orderId);

        UserResponse user = new UserResponse();
        user.setName("Nastya");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderMapper).updateOrderFromRequest(request, order);
        when(orderRepository.save(order)).thenReturn(updated);
        when(orderMapper.toResponse(updated)).thenReturn(mapped);
        when(userClient.getUserById(10L)).thenReturn(user);

        OrderResponse response = orderService.update(request, orderId);

        assertEquals(orderId, response.getId());
        assertEquals(user, response.getUser());

        verify(orderRepository).findById(orderId);
        verify(orderMapper).updateOrderFromRequest(request, order);
        verify(orderRepository).save(order);
        verify(orderMapper).toResponse(updated);
        verify(userClient).getUserById(10L);
    }

    @Test
    void update_shouldThrowOrderNotFound() {
        Long orderId = 1L;
        OrderUpdateRequest request = new OrderUpdateRequest();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.update(request, orderId));

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void update_shouldThrowWhenDeleted() {
        Long orderId = 1L;
        OrderUpdateRequest request = new OrderUpdateRequest();

        Order order = new Order();
        order.setId(orderId);
        order.setDeleted(true);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(OrderNotFoundException.class,
                () -> orderService.update(request, orderId));

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void deleteById_success() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(Status.CREATED);
        order.setDeleted(false);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.deleteById(orderId, authUser);

        verify(orderRepository).findById(orderId);
        verify(orderRepository).softDelete(orderId);
    }

    @Test
    void deleteById_shouldThrowAccessDenied_whenUserNotOwner() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(99L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(Status.CREATED);
        order.setDeleted(false);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.deleteById(orderId, authUser));

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).softDelete(anyLong());
    }

    @Test
    void deleteById_shouldThrowAccessDenied_whenStatusNotCreated() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(Status.PAID);
        order.setDeleted(false);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.deleteById(orderId, authUser));

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).softDelete(anyLong());
    }

    @Test
    void deleteById_shouldThrowOrderNotFound() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.deleteById(orderId, authUser));

        verify(orderRepository).findById(orderId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void deleteById_shouldThrowWhenDeleted() {
        Long orderId = 1L;

        AuthUser authUser = new AuthUser(10L, "ROLE_USER");

        Order order = new Order();
        order.setId(orderId);
        order.setDeleted(true);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(OrderNotFoundException.class,
                () -> orderService.deleteById(orderId, authUser));

        verify(orderRepository).findById(orderId);
        verifyNoMoreInteractions(orderRepository);
    }
}
