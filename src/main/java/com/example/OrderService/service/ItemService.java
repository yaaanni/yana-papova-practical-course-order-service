package com.example.OrderService.service;

import com.example.OrderService.dto.item.ItemRequest;
import com.example.OrderService.dto.item.ItemResponse;
import com.example.OrderService.entity.Item;
import com.example.OrderService.exception.ItemNotFoundException;
import com.example.OrderService.mapper.ItemMapper;
import com.example.OrderService.repository.ItemRepository;
import com.example.OrderService.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final ItemMapper itemMapper;

    public ItemResponse create(ItemRequest request) {

        Item item = itemMapper.toEntity(request);
        Item created = itemRepository.save(item);
        return itemMapper.toResponse(created);
    }

    public ItemResponse getItemById(Long id) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item with id: " + id + " not found"));

        return itemMapper.toResponse(item);
    }

    @Transactional
    public void delete(Long id) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item with id: " + id + " not found"));

        itemRepository.delete(item);
    }
}
