package com.swapmarket.service;

import com.swapmarket.dto.ItemRequest;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.User;
import com.swapmarket.enums.ItemStatus;
import com.swapmarket.exception.ApiException;
import com.swapmarket.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item createItem(ItemRequest request, User owner) {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setSize(request.getSize());
        item.setCondition(request.getCondition());
        item.setImages(request.getImages());
        item.setStatus(ItemStatus.AVAILABLE);
        return itemRepository.save(item);
    }

    public List<Item> listAvailable(String category) {
        if (category != null && !category.isBlank()) {
            return itemRepository.findByCategoryAndStatus(category, ItemStatus.AVAILABLE);
        }
        return itemRepository.findByStatus(ItemStatus.AVAILABLE);
    }

    public List<Item> listByOwner(Long ownerId) {
        return itemRepository.findByOwnerId(ownerId);
    }

    public Item getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
    }
}
