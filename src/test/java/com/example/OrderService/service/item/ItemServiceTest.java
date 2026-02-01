package com.example.OrderService.service.item;

import com.example.OrderService.dto.item.ItemRequest;
import com.example.OrderService.dto.item.ItemResponse;
import com.example.OrderService.entity.Item;
import com.example.OrderService.exception.ItemNotFoundException;
import com.example.OrderService.mapper.ItemMapper;
import com.example.OrderService.repository.ItemRepository;
import com.example.OrderService.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void create_success() {
        ItemRequest request = new ItemRequest();

        Item item = new Item();
        Item created = new Item();
        created.setId(1L);

        ItemResponse response = new ItemResponse();
        response.setId(1L);

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(created);
        when(itemMapper.toResponse(created)).thenReturn(response);

        ItemResponse result = itemService.create(request);

        assertEquals(1L, result.getId());

        verify(itemMapper).toEntity(request);
        verify(itemRepository).save(item);
        verify(itemMapper).toResponse(created);
    }

    @Test
    void getItemById_success() {
        Long itemId = 1L;

        Item item = new Item();
        item.setId(itemId);

        ItemResponse response = new ItemResponse();
        response.setId(itemId);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(response);

        ItemResponse result = itemService.getItemById(itemId);

        assertEquals(itemId, result.getId());

        verify(itemRepository).findById(itemId);
        verify(itemMapper).toResponse(item);
    }

    @Test
    void getItemById_shouldThrowItemNotFound() {
        Long itemId = 1L;

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> itemService.getItemById(itemId));

        verify(itemRepository).findById(itemId);
        verifyNoInteractions(itemMapper);
    }

    @Test
    void delete_success() {
        Long itemId = 1L;

        Item item = new Item();
        item.setId(itemId);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        itemService.delete(itemId);

        verify(itemRepository).findById(itemId);
        verify(itemRepository).delete(item);
    }

    @Test
    void delete_shouldThrowItemNotFound() {
        Long itemId = 1L;

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> itemService.delete(itemId));

        verify(itemRepository).findById(itemId);
        verifyNoMoreInteractions(itemRepository);
    }
}
