package com.dove.stockcollection.application.service;

import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionStatus;
import com.dove.stockcollection.domain.enums.CollectionType;
import com.dove.stockcollection.domain.repository.CollectionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * COLLECTION_TASK 생성·진행률 갱신·완료/실패 처리.
 */
@Service
@RequiredArgsConstructor
public class CollectionTaskService {

    private final CollectionTaskRepository repository;

    @Transactional
    public Long create(CollectionType type, StockExchange exchange,
                       LocalDate from, LocalDate to, Long requestedBy) {
        CollectionTask task = repository.save(new CollectionTask(type, exchange, from, to, requestedBy));
        return task.getId();
    }

    @Transactional
    public void start(Long taskId, int total) {
        repository.findById(taskId).ifPresent(t -> t.start(total));
    }

    /**
     * 진행률을 갱신한다.
     */
    @Transactional
    public void updateProgress(Long taskId, int done) {
        repository.findById(taskId).ifPresent(t -> t.updateProgress(done));
    }

    /**
     * 수정주가 재조회 대상 수를 설정한다.
     */
    @Transactional
    public void setAdjustedTotal(Long taskId, int total) {
        repository.findById(taskId).ifPresent(t -> t.setAdjustedTotal(total));
    }

    /**
     * 수정주가 재조회 진행을 갱신한다.
     */
    @Transactional
    public void updateAdjustedProgress(Long taskId, int done) {
        repository.findById(taskId).ifPresent(t -> t.updateAdjustedProgress(done));
    }

    @Transactional
    public void complete(Long taskId) {
        repository.findById(taskId).ifPresent(CollectionTask::complete);
    }

    @Transactional
    public void fail(Long taskId, String errorCode, String errorDetail) {
        repository.findById(taskId).ifPresent(t -> t.fail(errorCode, errorDetail));
    }

    @Transactional(readOnly = true)
    public Optional<CollectionTask> find(Long taskId) {
        return repository.findById(taskId);
    }

    @Transactional(readOnly = true)
    public Page<CollectionTask> findRecent(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<CollectionTask> findByStatus(CollectionStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }
}
