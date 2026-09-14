package com.swapmarket.repository;

import com.swapmarket.entity.SwapRequest;
import com.swapmarket.enums.SwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {
    List<SwapRequest> findByRequesterId(Long requesterId);
    List<SwapRequest> findByItemIdAndStatus(Long itemId, SwapStatus status);
}
