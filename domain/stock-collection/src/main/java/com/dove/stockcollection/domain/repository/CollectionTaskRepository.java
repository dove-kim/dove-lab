package com.dove.stockcollection.domain.repository;

import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionStatus;
import com.dove.stockcollection.domain.enums.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * COLLECTION_TASK 저장소.
 */
@Repository
public interface CollectionTaskRepository extends JpaRepository<CollectionTask, Long> {
    Page<CollectionTask> findByStatus(CollectionStatus status, Pageable pageable);
    Page<CollectionTask> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<CollectionTask> findAllByStatus(CollectionStatus status);
    Optional<CollectionTask> findFirstByTypeInAndStatusOrderByCreatedAtAsc(List<CollectionType> types, CollectionStatus status);
    boolean existsByTypeInAndStatus(List<CollectionType> types, CollectionStatus status);
}
