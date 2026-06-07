package com.dove.concurrent;

/**
 * {@link Parallel#run} 중 발생한 첫 예외를 감싼다.
 */
public class ParallelException extends RuntimeException {
    public ParallelException(Throwable cause) {
        super("병렬 작업 중 오류 발생 — 이후 작업 중단됨", cause);
    }
}
