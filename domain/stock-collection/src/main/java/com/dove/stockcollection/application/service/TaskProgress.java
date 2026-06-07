package com.dove.stockcollection.application.service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * COLLECTION_TASK에 진행률을 반영하는 {@link CollectionProgress} 구현.
 */
class TaskProgress implements CollectionProgress {

    private static final long FLUSH_INTERVAL_MS = 5_000;

    private final CollectionTaskService taskService;
    private final Long taskId;
    private final AtomicLong lastFlush = new AtomicLong(0);
    private final AtomicLong lastAdjFlush = new AtomicLong(0);

    TaskProgress(CollectionTaskService taskService, Long taskId) {
        this.taskService = taskService;
        this.taskId = taskId;
    }

    @Override
    public void onTotal(int total) {
        taskService.start(taskId, total);
    }

    @Override
    public void onProgress(int done) {
        long now = System.currentTimeMillis();
        long last = lastFlush.get();
        if (now - last >= FLUSH_INTERVAL_MS && lastFlush.compareAndSet(last, now)) {
            taskService.updateProgress(taskId, done);
        }
    }

    @Override
    public void onAdjustedTotal(int total) {
        taskService.setAdjustedTotal(taskId, total);
    }

    @Override
    public void onAdjustedProgress(int done) {
        long now = System.currentTimeMillis();
        long last = lastAdjFlush.get();
        if (now - last >= FLUSH_INTERVAL_MS && lastAdjFlush.compareAndSet(last, now)) {
            taskService.updateAdjustedProgress(taskId, done);
        }
    }
}
