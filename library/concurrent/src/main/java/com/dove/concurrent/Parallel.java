package com.dove.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    /**
     * {@link #run}과 같되, 개별 작업 실패를 모아 나머지를 계속 진행한다 (한 작업 실패가 배치 전체를 죽이지 않음).
     * 실패 수가 {@code maxFailures}에 도달하면 체계적 장애로 보고 남은 작업을 중단하고 예외를 던진다.
     *
     * @param tasks       처리할 작업 목록 (lazy 평가)
     * @param concurrency 동시 실행 가상 스레드 상한
     * @param maxFailures 이 수만큼 실패하면 배치를 중단한다 (1 이상)
     * @param handler     각 작업 처리 로직
     * @return 실패한 작업 목록 (maxFailures 미만으로 끝난 경우)
     * @throws ParallelException 실패 수가 {@code maxFailures}에 도달한 경우
     */
    public static <T> List<T> runResilient(Iterable<T> tasks, int concurrency, int maxFailures, Consumer<T> handler) {
        Semaphore slots = new Semaphore(concurrency);
        Queue<T> failed = new ConcurrentLinkedQueue<>();
        AtomicReference<Throwable> abortError = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (T task : tasks) {
                if (abortError.get() != null) break; // 임계 초과 → 제출 중단

                try {
                    slots.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    abortError.compareAndSet(null, e);
                    break;
                }

                if (abortError.get() != null) {
                    slots.release();
                    break;
                }

                executor.execute(() -> {
                    try {
                        handler.accept(task);
                    } catch (Throwable t) {
                        failed.add(task);
                        if (failed.size() >= maxFailures) abortError.compareAndSet(null, t);
                    } finally {
                        slots.release();
                    }
                });
            }
        } // try-with-resources: 제출된 모든 작업 완료까지 대기

        Throwable error = abortError.get();
        if (error != null) {
            throw new ParallelException(error);
        }
        return new ArrayList<>(failed);
    }
}
