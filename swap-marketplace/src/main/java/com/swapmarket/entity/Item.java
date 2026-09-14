package com.swapmarket.entity;

import com.swapmarket.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String category;
    private String size;
    private String condition;

    @ElementCollection
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.AVAILABLE;

    /**
     * Optimistic locking version.
     * When two requests try to update the same Item row concurrently (e.g. two
     * users both trying to lock this item into a swap at the same instant),
     * Hibernate checks this version on UPDATE. Whichever transaction commits
     * first wins; the second gets an ObjectOptimisticLockingFailureException,
     * which the service layer turns into a clean "item no longer available"
     * response instead of silently corrupting state.
     */
    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
