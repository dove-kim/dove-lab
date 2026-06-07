package com.dove.stockcollection.domain.entity;

import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.enums.CollectionStatus;
import com.dove.stockcollection.domain.enums.CollectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollectionTask")
class CollectionTaskTest {

    private static final LocalDate FROM = LocalDate.of(2026, 5, 1);
    private static final LocalDate TO = LocalDate.of(2026, 5, 30);

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("PENDING 상태와 0 진행값으로 초기화한다")
        void shouldInitializePendingWithZeroProgressWhenCreated() {
            CollectionTask task = new CollectionTask(
                    CollectionType.PRICE, StockExchange.KOSPI, FROM, TO, 7L);

            assertThat(task.getStatus()).isEqualTo(CollectionStatus.PENDING);
            assertThat(task.getTotal()).isZero();
            assertThat(task.getDone()).isZero();
            assertThat(task.getAdjustedTotal()).isZero();
            assertThat(task.getAdjustedDone()).isZero();
            assertThat(task.getRequestedBy()).isEqualTo(7L);
            assertThat(task.getStartedAt()).isNull();
            assertThat(task.getFinishedAt()).isNull();
        }

        @Test
        @DisplayName("PRICE는 거래소를 포함한 범위 문자열을 만든다")
        void shouldBuildScopeWithExchangeWhenPrice() {
            CollectionTask task = new CollectionTask(
                    CollectionType.PRICE, StockExchange.KOSPI, FROM, TO, 7L);

            assertThat(task.getScope()).isEqualTo("PRICE/KOSPI/2026-05-01~2026-05-30");
        }

        @Test
        @DisplayName("거래소가 null인 STOCK은 거래소 없는 범위 문자열을 만든다")
        void shouldBuildScopeWithoutExchangeWhenStockHasNullExchange() {
            CollectionTask task = new CollectionTask(
                    CollectionType.STOCK, null, FROM, TO, 7L);

            assertThat(task.getScope()).isEqualTo("STOCK/2026-05-01~2026-05-30");
        }

        @Test
        @DisplayName("거래소가 null인 EVENT는 거래소 없는 범위 문자열을 만든다")
        void shouldBuildScopeWithoutExchangeWhenEventHasNullExchange() {
            CollectionTask task = new CollectionTask(
                    CollectionType.EVENT, null, FROM, TO, 7L);

            assertThat(task.getScope()).isEqualTo("EVENT/2026-05-01~2026-05-30");
        }
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        @DisplayName("RUNNING 상태로 전환하고 total과 startedAt을 설정한다")
        void shouldTransitionToRunningWithTotalWhenStarted() {
            CollectionTask task = newPriceTask();

            task.start(120);

            assertThat(task.getStatus()).isEqualTo(CollectionStatus.RUNNING);
            assertThat(task.getTotal()).isEqualTo(120);
            assertThat(task.getDone()).isZero();
            assertThat(task.getStartedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("진행률 갱신")
    class ProgressUpdates {

        @Test
        @DisplayName("updateProgress는 완료 수를 반영한다")
        void shouldReflectDoneWhenUpdateProgress() {
            CollectionTask task = newPriceTask();

            task.updateProgress(42);

            assertThat(task.getDone()).isEqualTo(42);
        }

        @Test
        @DisplayName("setAdjustedTotal은 재조회 대상 수를 반영한다")
        void shouldReflectAdjustedTotalWhenSet() {
            CollectionTask task = newPriceTask();

            task.setAdjustedTotal(15);

            assertThat(task.getAdjustedTotal()).isEqualTo(15);
        }

        @Test
        @DisplayName("updateAdjustedProgress는 재조회 완료 수를 반영한다")
        void shouldReflectAdjustedDoneWhenUpdateAdjustedProgress() {
            CollectionTask task = newPriceTask();

            task.updateAdjustedProgress(9);

            assertThat(task.getAdjustedDone()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("DONE 상태로 전환하고 done/adjustedDone을 total값으로 채운다")
        void shouldTransitionToDoneFillingProgressWhenCompleted() {
            CollectionTask task = newPriceTask();
            task.start(100);
            task.setAdjustedTotal(20);

            task.complete();

            assertThat(task.getStatus()).isEqualTo(CollectionStatus.DONE);
            assertThat(task.getDone()).isEqualTo(100);
            assertThat(task.getAdjustedDone()).isEqualTo(20);
            assertThat(task.getFinishedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("FAILED 상태로 전환하고 에러 코드·상세를 기록한다")
        void shouldTransitionToFailedRecordingErrorWhenFailed() {
            CollectionTask task = newPriceTask();

            task.fail("KIS_TIMEOUT", "외부 호출 시간 초과");

            assertThat(task.getStatus()).isEqualTo(CollectionStatus.FAILED);
            assertThat(task.getErrorCode()).isEqualTo("KIS_TIMEOUT");
            assertThat(task.getErrorDetail()).isEqualTo("외부 호출 시간 초과");
            assertThat(task.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("1000자를 초과하는 상세는 1000자로 잘린다")
        void shouldTruncateDetailWhenExceeds1000Chars() {
            CollectionTask task = newPriceTask();
            String detail = "x".repeat(1500);

            task.fail("ERR", detail);

            assertThat(task.getErrorDetail()).hasSize(1000);
        }

        @Test
        @DisplayName("null 상세는 그대로 null로 둔다")
        void shouldKeepNullWhenDetailIsNull() {
            CollectionTask task = newPriceTask();

            task.fail("ERR", null);

            assertThat(task.getErrorDetail()).isNull();
        }
    }

    private static CollectionTask newPriceTask() {
        return new CollectionTask(CollectionType.PRICE, StockExchange.KOSPI, FROM, TO, 7L);
    }
}
