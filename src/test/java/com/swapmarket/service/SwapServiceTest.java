package com.swapmarket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.swapmarket.dto.SwapRequestDto;
import com.swapmarket.dto.ValueComparison;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.User;
import com.swapmarket.enums.ItemStatus;
import com.swapmarket.enums.SwapStatus;
import com.swapmarket.enums.ValueVerdict;
import com.swapmarket.exception.ApiException;
import com.swapmarket.repository.ItemRepository;
import com.swapmarket.repository.SwapRequestRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class SwapServiceTest {

    @Mock private SwapRequestRepository swapRequestRepository;
    @Mock private ItemRepository itemRepository;
    private SwapService swapService;

    private User owner;
    private User stranger;
    private User thirdParty;
    private Item item;
    private SwapRequest request;

    @BeforeEach
    void setUp() {
        // Built by hand (rather than @InjectMocks) so the real ValueCalculator is used.
        swapService = new SwapService(swapRequestRepository, itemRepository, new ValueCalculator());

        owner = new User();
        owner.setId(1L);

        stranger = new User();
        stranger.setId(2L);

        thirdParty = new User();
        thirdParty.setId(3L);

        item = new Item();
        item.setOwner(owner);
        item.setStatus(ItemStatus.AVAILABLE);

        request = new SwapRequest();
        request.setRequester(stranger);
        request.setItem(item);
        request.setStatus(SwapStatus.PENDING);
    }

    private Item itemOwnedBy(User user, String originalPrice, ItemStatus status) {
        Item i = new Item();
        i.setOwner(user);
        i.setStatus(status);
        i.setCondition("Good");
        i.setOriginalPrice(originalPrice == null ? null : new BigDecimal(originalPrice));
        return i;
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

    @Test
    void createRequest_forYourOwnItem_isRejected() {
        SwapRequestDto dto = new SwapRequestDto();
        dto.setItemId(5L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.createRequest(dto, owner));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_offeringAnItemYouDontOwn_isRejected() {
        Item someoneElses = itemOwnedBy(thirdParty, "1000", ItemStatus.AVAILABLE);
        SwapRequestDto dto = new SwapRequestDto();
        dto.setItemId(5L);
        dto.setOfferedItemId(6L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(itemRepository.findById(6L)).thenReturn(Optional.of(someoneElses));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.createRequest(dto, stranger));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_offeringAnItemThatIsNotAvailable_isRejected() {
        Item locked = itemOwnedBy(stranger, "1000", ItemStatus.LOCKED);
        SwapRequestDto dto = new SwapRequestDto();
        dto.setItemId(5L);
        dto.setOfferedItemId(6L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(itemRepository.findById(6L)).thenReturn(Optional.of(locked));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.createRequest(dto, stranger));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_withAValidOffer_savesAPendingRequest() {
        Item mine = itemOwnedBy(stranger, "1000", ItemStatus.AVAILABLE);
        SwapRequestDto dto = new SwapRequestDto();
        dto.setItemId(5L);
        dto.setOfferedItemId(6L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(itemRepository.findById(6L)).thenReturn(Optional.of(mine));
        when(swapRequestRepository.save(any(SwapRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SwapRequest saved = swapService.createRequest(dto, stranger);

        assertEquals(SwapStatus.PENDING, saved.getStatus());
        assertSame(mine, saved.getOfferedItem());
        assertSame(stranger, saved.getRequester());
    }

    // ---------- previewValue ----------

    @Test
    void previewValue_comparesTheRequestedAndOfferedItems() {
        Item requested = itemOwnedBy(owner, "2500", ItemStatus.AVAILABLE);   // worth 1000
        Item offered = itemOwnedBy(stranger, "1250", ItemStatus.AVAILABLE);  // worth 500
        when(itemRepository.findById(5L)).thenReturn(Optional.of(requested));
        when(itemRepository.findById(6L)).thenReturn(Optional.of(offered));

        ValueComparison result = swapService.previewValue(5L, 6L);

        assertEquals(ValueVerdict.OFFER_LOWER, result.verdict());
        assertEquals(0, new BigDecimal("500").compareTo(result.suggestedTopUp()));
    }

    @Test
    void previewValue_withoutAnOfferedItem_isDepositOnly() {
        Item requested = itemOwnedBy(owner, "2500", ItemStatus.AVAILABLE);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(requested));

        ValueComparison result = swapService.previewValue(5L, null);

        assertEquals(ValueVerdict.DEPOSIT_ONLY, result.verdict());
    }

    @Test
    void previewValue_forAnUnknownItem_isNotFound() {
        when(itemRepository.findById(5L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> swapService.previewValue(5L, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- rejectSwap ----------

    @Test
    void rejectSwap_byOwner_marksRequestRejectedWithTrimmedReason() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(swapRequestRepository.save(any(SwapRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SwapRequest result = swapService.rejectSwap(10L, owner, "  Not my size  ");

        assertEquals(SwapStatus.REJECTED, result.getStatus());
        assertEquals("Not my size", result.getRejectionReason());
        assertNotNull(result.getUpdatedAt());
        // Nothing was locked yet, so the item must be left alone.
        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void rejectSwap_withBlankReason_storesNoReason() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(swapRequestRepository.save(any(SwapRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SwapRequest result = swapService.rejectSwap(10L, owner, "   ");

        assertEquals(SwapStatus.REJECTED, result.getStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    void rejectSwap_byNonOwner_isForbidden() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.rejectSwap(10L, stranger, "no"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(SwapStatus.PENDING, request.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void rejectSwap_whenNoLongerPending_isConflict() {
        request.setStatus(SwapStatus.ACCEPTED);
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.rejectSwap(10L, owner, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(SwapStatus.ACCEPTED, request.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    // ---------- cancelSwap (user-facing) ----------

    @Test
    void cancelSwap_byRequester_cancelsAPendingRequest() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        swapService.cancelSwap(10L, stranger);

        assertEquals(SwapStatus.CANCELLED, request.getStatus());
    }

    @Test
    void cancelSwap_byItemOwner_isAllowed() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        swapService.cancelSwap(10L, owner);

        assertEquals(SwapStatus.CANCELLED, request.getStatus());
    }

    @Test
    void cancelSwap_byThirdParty_isForbidden() {
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.cancelSwap(10L, thirdParty));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(SwapStatus.PENDING, request.getStatus());
        verify(swapRequestRepository, never()).save(any());
    }

    @Test
    void cancelSwap_whenAlreadyCompleted_isConflict() {
        request.setStatus(SwapStatus.COMPLETED);
        when(swapRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        ApiException ex = assertThrows(ApiException.class, () -> swapService.cancelSwap(10L, stranger));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(SwapStatus.COMPLETED, request.getStatus());
    }

    // ---------- cancelSwap (internal, used by the refund flow) ----------

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