package com.swapmarket.controller;

import com.swapmarket.dto.SwapRequestDto;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.User;
import com.swapmarket.service.SwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/swap-requests")
@RequiredArgsConstructor
public class SwapController {

    private final SwapService swapService;

    @PostMapping
    public ResponseEntity<SwapRequest> create(@RequestBody SwapRequestDto dto,
                                               @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(swapService.createRequest(dto, requester));
    }

    // The endpoint that exercises the optimistic-locking logic in SwapService.
    // Fire two concurrent requests at this endpoint for the same item's
    // competing swap requests to see the 409 conflict response fire for
    // whichever request loses the race.
    @PatchMapping("/{id}/accept")
    public ResponseEntity<SwapRequest> accept(@PathVariable Long id,
                                               @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(swapService.acceptSwap(id, currentUser));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        swapService.cancelSwap(id);
        return ResponseEntity.noContent().build();
    }
}
