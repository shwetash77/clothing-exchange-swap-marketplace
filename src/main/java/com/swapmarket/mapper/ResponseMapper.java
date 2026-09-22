package com.swapmarket.mapper;

import com.swapmarket.dto.ItemResponse;
import com.swapmarket.dto.PaymentSummary;
import com.swapmarket.dto.SwapRequestResponse;
import com.swapmarket.dto.UserSummary;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.SwapTransaction;
import com.swapmarket.entity.User;
import com.swapmarket.repository.SwapTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps JPA entities to API responses. Returning entities directly would leak
 * User.passwordHash through Item.owner and break on lazy Hibernate proxies.
 * Relies on Open-Session-In-View (Spring Boot default) for lazy loading.
 */
@Component
@RequiredArgsConstructor
public class ResponseMapper {

    private final SwapTransactionRepository transactionRepository;

    public UserSummary toUser(User user) {
        return new UserSummary(user.getId(), user.getName());
    }

    public ItemResponse toItem(Item item) {
        List<String> images = item.getImages() == null ? List.of() : List.copyOf(item.getImages());
        return new ItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getCategory(),
                item.getSize(), item.getCondition(), images, item.getStatus(),
                toUser(item.getOwner()), item.getCreatedAt());
    }

    public PaymentSummary toPayment(SwapTransaction tx) {
        return new PaymentSummary(tx.getStatus(), tx.getDepositAmount(), tx.getRazorpayOrderId());
    }

    public SwapRequestResponse toSwap(SwapRequest swap) {
        // One extra query per swap: fine for a demo, batch it if lists get large.
        PaymentSummary payment = transactionRepository.findBySwapRequestId(swap.getId())
                .map(this::toPayment)
                .orElse(null);
        ItemResponse offered = swap.getOfferedItem() == null ? null : toItem(swap.getOfferedItem());
        return new SwapRequestResponse(swap.getId(), swap.getStatus(), toItem(swap.getItem()), offered,
                toUser(swap.getRequester()), payment, swap.getCreatedAt());
    }
}
