package com.swapmarket.service;

import com.swapmarket.dto.SwapRequestDto;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.User;
import com.swapmarket.enums.ItemStatus;
import com.swapmarket.enums.SwapStatus;
import com.swapmarket.exception.ApiException;
import com.swapmarket.repository.ItemRepository;
import com.swapmarket.repository.SwapRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwapService {

    private final SwapRequestRepository swapRequestRepository;
    private final ItemRepository itemRepository;

    /**
     * Anyone can open a swap request - this doesn't touch the item's
     * status yet, so multiple people can request the same item
     * simultaneously without conflict. The lock only happens at accept time.
     */
    public List<SwapRequest> listSent(Long userId) {
        return swapRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
    }

    public List<SwapRequest> listReceived(Long ownerId) {
        return swapRequestRepository.findByItemOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public SwapRequest createRequest(SwapRequestDto dto, User requester) {
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));

        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "Item is not available for swap");
        }

        SwapRequest request = new SwapRequest();
        request.setRequester(requester);
        request.setItem(item);

        if (dto.getOfferedItemId() != null) {
            Item offered = itemRepository.findById(dto.getOfferedItemId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offered item not found"));
            request.setOfferedItem(offered);
        }

        return swapRequestRepository.save(request);
    }

    /**
     * THE concurrency-critical operation in this project.
     *
     * Scenario: two different swap requests exist for the same item, and
     * both requesters (or the owner accepting both) hit "accept" at nearly
     * the same instant. Without protection, both could succeed, leaving the
     * owner having "promised" the same physical item to two people.
     *
     * How this prevents that:
     * 1. @Transactional wraps the read-check-write in one atomic unit.
     * 2. Item carries a @Version column (see Item.java). When this method
     *    loads the item, changes its status, and calls save(), Hibernate
     *    issues an UPDATE that includes "WHERE id = ? AND version = ?".
     * 3. If a concurrent transaction already updated the item (and bumped
     *    the version) between our read and our write, that UPDATE affects
     *    zero rows and Hibernate throws ObjectOptimisticLockingFailureException.
     * 4. GlobalExceptionHandler catches that and returns a clean 409 instead
     *    of silently double-booking the item.
     *
     * Trade-off worth stating out loud in an interview: optimistic locking
     * assumes conflicts are rare, so it doesn't block concurrent readers -
     * it only fails at commit time. If conflicts were expected to be
     * frequent, pessimistic locking (see ItemRepository.findByIdForUpdate,
     * a SELECT ... FOR UPDATE) would reduce wasted work by blocking the
     * second transaction upfront instead of letting it fail late.
     */
    @Transactional
    public SwapRequest acceptSwap(Long swapRequestId, User currentUser) {
        SwapRequest request = swapRequestRepository.findById(swapRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap request not found"));

        if (request.getStatus() != SwapStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Swap request is no longer pending");
        }

        Item item = request.getItem();

        // Only the item's owner may accept a swap for it.
        if (!item.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the item's owner can accept this swap");
        }

        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "Item is no longer available");
        }

        // Lock the item to this swap. If another transaction beat us here,
        // the version check on this save() throws and the accept fails cleanly.
        item.setStatus(ItemStatus.LOCKED);
        itemRepository.save(item);

        request.setStatus(SwapStatus.ACCEPTED);
        request.setUpdatedAt(Instant.now());
        return swapRequestRepository.save(request);
    }

    @Transactional
    public void completeSwap(Long swapRequestId) {
        SwapRequest request = swapRequestRepository.findById(swapRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap request not found"));

        request.setStatus(SwapStatus.COMPLETED);
        request.setUpdatedAt(Instant.now());
        swapRequestRepository.save(request);

        Item item = request.getItem();
        item.setStatus(ItemStatus.SWAPPED);
        itemRepository.save(item);
    }

    @Transactional
    public void cancelSwap(Long swapRequestId) {
        SwapRequest request = swapRequestRepository.findById(swapRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap request not found"));

        request.setStatus(SwapStatus.CANCELLED);
        request.setUpdatedAt(Instant.now());
        swapRequestRepository.save(request);

        // release the item back to the pool
        Item item = request.getItem();
        if (item.getStatus() == ItemStatus.LOCKED) {
            item.setStatus(ItemStatus.AVAILABLE);
            itemRepository.save(item);
        }
    }
}
