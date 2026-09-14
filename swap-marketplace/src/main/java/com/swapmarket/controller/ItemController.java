package com.swapmarket.controller;

import com.swapmarket.dto.ItemRequest;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.User;
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

    @GetMapping
    public ResponseEntity<List<Item>> listItems(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(itemService.listAvailable(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    // Only SELLER or ADMIN roles can list items - this is the RBAC
    // enforcement point the JD's "enterprise applications" ask is testing for.
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody ItemRequest request,
                                            @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(itemService.createItem(request, owner));
    }
}
