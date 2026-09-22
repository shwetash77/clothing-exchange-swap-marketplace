package com.swapmarket.repository;

import com.swapmarket.entity.Item;
import com.swapmarket.enums.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByStatus(ItemStatus status);

    List<Item> findByOwnerId(Long ownerId);

    List<Item> findByCategoryAndStatus(String category, ItemStatus status);

    /**
     * Pessimistic row lock alternative to the @Version optimistic approach
     * on Item. Useful where you want to block concurrent readers outright
     * (SELECT ... FOR UPDATE) rather than let both proceed and fail one at
     * commit time. Kept here so the codebase demonstrates both strategies -
     * be ready to explain the trade-off (optimistic = better throughput,
     * pessimistic = simpler reasoning, more contention) in an interview.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(Long id);
}
