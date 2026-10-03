package com.swapmarket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.swapmarket.dto.SwapRequestDto;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.User;
import com.swapmarket.enums.ItemStatus;
import com.swapmarket.enums.SwapStatus;
import com.swapmarket.exception.ApiException;
import com.swapmarket.repository.ItemRepository;
import com.swapmarket.repository.SwapRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class SwapServiceTest {

    @Mock private SwapRequestRepository swapRequestRepository;
    @Mock private ItemRepository itemRepository;
    @InjectMocks private SwapService swapService;

    private User owner;
    private User stranger;
    private Item item;
    private SwapRequest request;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);

        stranger = new User();
        stranger.setId(2L);

        item = new Item();
        item.setOwner(owner);
        item.setStatus(ItemStatus.AVAILABLE);

        request = new SwapRequest();
        request.setItem(item);
        request.setStatus(SwapStatus.PENDING);
    }

    // ---------- acceptSwap ----------

    @Test
    void acceptSwap_byOwner_locksItemAndAcceptsRequest() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(swapRequestRepository.save(any(SwapRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SwapRequest result = swapService.acceptSwap(10L, owner);

        assertEquals(SwapStatus.ACCEPTED, result.getStatus());
        assertEquals(ItemStatus.LOCKED, item.getStatus());
        verify(itemRepository).save(item);
    }

    @Test
    void acceptSwap_byNonOwner_isRejectedAndItemUntouched() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> swapService.acceptSwap(10L, stranger));

        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void acceptSwap_whenRequestNoLongerPending_isRejected() {
        request.setStatus(SwapStatus.CANCELLED);
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> swapService.acceptSwap(10L, owner));

        verify(itemRepository, never()).save(any());
    }

    @Test
    void acceptSwap_whenItemAlreadyLocked_isRejected() {
        item.setStatus(ItemStatus.LOCKED);
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> swapService.acceptSwap(10L, owner));

        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void acceptSwap_whenConcurrentUpdateDetected_throwsOptimisticLockFailure() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        // Simulates Hibernate's @Version check failing because another
        // transaction updated the item first.
        when(itemRepository.save(item))
                .thenThrow(new ObjectOptimisticLockingFailureException(Item.class, 1L));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> swapService.acceptSwap(10L, owner));

        // The request must NOT be marked accepted when the item lock fails.
        verify(swapRequestRepository, never()).save(any());
        assertEquals(SwapStatus.PENDING, request.getStatus());
    }

    // ---------- createRequest ----------

    @Test
    void createRequest_whenItemNotAvailable_isRejected() {
        item.setStatus(ItemStatus.LOCKED);
        SwapRequestDto dto = new SwapRequestDto();
        dto.setItemId(5L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThrows(ApiException.class, () -> swapService.createRequest(dto, stranger));

        verify(swapRequestRepository, never()).save(any());
    }

    // ---------- cancelSwap ----------

    @Test
    void cancelSwap_releasesLockedItemBackToAvailable() {
        item.setStatus(ItemStatus.LOCKED);
        request.setStatus(SwapStatus.ACCEPTED);
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        swapService.cancelSwap(10L);

        assertEquals(SwapStatus.CANCELLED, request.getStatus());
        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
        verify(itemRepository).save(item);
    }
}