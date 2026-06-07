package com.dove.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 동시 실행 수를 최대 {@code concurrency}개로 제한하는 가상 스레드 병렬 실행기 (fail-fast).
 */
public final class Parallel {

    private Parallel() {}

    /**
     * 각 작업을 최대 {@code concurrency}개의 가상 스레드로 병렬 실행한다.
     *
     * @param tasks       처리할 작업 목록 (lazy 평가 — 미리 전부 메모리에 올리지 않음)
     * @param concurrency 동시 실행 가상 스레드 상한
     * @param handler     각 작업 처리 로직
     * @throws ParallelException 작업 중 처리되지 않은 예외가 발생한 경우
     */
    public static <T> void run(Iterable<T> tasks, int concurrency, Consumer<T> handler) {
        Semaphore slots = new Semaphore(concurrency);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (T task : tasks) {
                if (firstError.get() != null) break; // fail-fast: 이미 실패 → 제출 중단

                try {
                    slots.acquire(); // 슬롯 없으면 생산자 블로킹 (스레드 생성 억제)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    firstError.compareAndSet(null, e);
                    break;
                }

                if (firstError.get() != null) { // 대기 중 실패 발생 가능
                    slots.release();
                    break;
                }

                executor.execute(() -> {
                    try {
                        handler.accept(task);
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        slots.release();
                    }
                });
            }
        } // try-with-resources: 제출된 모든 작업 완료까지 대기

        Throwable error = firstError.get();
        if (error != null) {
            throw new ParallelException(error);
        }
    }
}
