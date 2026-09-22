package com.swapmarket.controller;

import com.swapmarket.dto.SwapRequestDto;
import com.swapmarket.dto.SwapRequestResponse;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.ResponseMapper;
import com.swapmarket.service.SwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/swap-requests")
@RequiredArgsConstructor
public class SwapController {

    private final SwapService swapService;
    private final ResponseMapper mapper;

    @PostMapping
    public ResponseEntity<SwapRequestResponse> create(@RequestBody SwapRequestDto dto,
                                                       @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(mapper.toSwap(swapService.createRequest(dto, requester)));
    }

    // Requests I have sent.
    @GetMapping("/mine")
    public ResponseEntity<List<SwapRequestResponse>> sent(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(swapService.listSent(user.getId()).stream().map(mapper::toSwap).toList());
    }

    // Requests other people have sent for items I own.
    @GetMapping("/incoming")
    public ResponseEntity<List<SwapRequestResponse>> received(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(swapService.listReceived(user.getId()).stream().map(mapper::toSwap).toList());
    }

    // The endpoint that exercises the optimistic-locking logic in SwapService.
    // Fire two concurrent requests at this endpoint for the same item's
    // competing swap requests to see the 409 conflict response fire for
    // whichever request loses the race.
    @PatchMapping("/{id}/accept")
    public ResponseEntity<SwapRequestResponse> accept(@PathVariable Long id,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(mapper.toSwap(swapService.acceptSwap(id, currentUser)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        swapService.cancelSwap(id);
        return ResponseEntity.noContent().build();
    }
}
