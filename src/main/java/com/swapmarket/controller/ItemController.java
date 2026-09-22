package com.swapmarket.controller;

import com.swapmarket.dto.ItemRequest;
import com.swapmarket.dto.ItemResponse;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.ResponseMapper;
import com.swapmarket.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ResponseMapper mapper;

    @GetMapping
    public ResponseEntity<List<ItemResponse>> listItems(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(itemService.listAvailable(category).stream().map(mapper::toItem).toList());
    }

    // The logged-in user's own items (used by the UI to pick an item to offer in a swap).
    @GetMapping("/mine")
    public ResponseEntity<List<ItemResponse>> myItems(@AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(itemService.listByOwner(owner.getId()).stream().map(mapper::toItem).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toItem(itemService.getById(id)));
    }

    // Only SELLER or ADMIN roles can list items - this is the RBAC
    // enforcement point the JD's "enterprise applications" ask is testing for.
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request,
                                                    @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(mapper.toItem(itemService.createItem(request, owner)));
    }
}
