package com.swapmarket.repository;

import com.swapmarket.entity.SwapTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {
    Optional<SwapTransaction> findByRazorpayOrderId(String razorpayOrderId);
    Optional<SwapTransaction> findBySwapRequestId(Long swapRequestId);
}
