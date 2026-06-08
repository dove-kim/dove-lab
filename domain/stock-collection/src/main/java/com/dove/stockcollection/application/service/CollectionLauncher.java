package com.dove.stockcollection.application.service;

import com.dove.concurrent.ParallelException;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 수집 태스크 생성(enqueue)과 실행(execute)을 제공하는 진입점.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionLauncher {

    private final PriceCollectionService priceCollectionService;
    private final StockCollectionService stockCollectionService;
    private final StockEventCollectionService eventCollectionService;
    private final InvestorCollectionService investorCollectionService;
    private final CollectionTaskService taskService;
    private final Clock clock;

    /**
     * 주가 재조회 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     * 재조회는 어제까지로 제한한다.
     *
     * @throws IllegalArgumentException 요청 범위 전체가 오늘 이후인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueuePriceCollection(StockExchange exchange, LocalDate from, LocalDate to, Long requestedBy) {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate cappedTo = to.isAfter(yesterday) ? yesterday : to;
        if (from.isAfter(cappedTo)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.PRICE, exchange, from, cappedTo, requestedBy);
    }

    /**
     * 권리 이벤트(KSD) 재조회 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     *
     * @throws IllegalArgumentException 날짜 범위가 역순인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueueEventCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.EVENT, null, from, to, requestedBy);
    }

    /**
     * 종목 재조회 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     *
     * @throws IllegalArgumentException 날짜 범위가 역순인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueueStockCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.STOCK, null, from, to, requestedBy);
    }

    /**
     * 투자자매매동향 재조회 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     *
     * @throws IllegalArgumentException 날짜 범위가 역순인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueueInvestorCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.INVESTOR, null, from, to, requestedBy);
    }

    /**
     * 실패한 태스크를 같은 범위로 다시 PENDING 등록하고 새 작업ID를 반환한다.
     *
     * @throws IllegalArgumentException 원본 태스크가 없는 경우 (TASK_NOT_FOUND)
     */
    public Long reenqueue(Long sourceTaskId, Long requestedBy) {
        CollectionTask source = taskService.find(sourceTaskId)
                .orElseThrow(() -> new IllegalArgumentException("TASK_NOT_FOUND"));
        LocalDate from = source.getFromDate();
        LocalDate to = source.getToDate();
        return switch (source.getType()) {
            case PRICE -> enqueuePriceCollection(source.getExchange(), from, to, requestedBy);
            case STOCK -> enqueueStockCollection(from, to, requestedBy);
            case EVENT -> enqueueEventCollection(from, to, requestedBy);
            case INVESTOR -> enqueueInvestorCollection(from, to, requestedBy);
        };
    }

    /**
     * PENDING 태스크를 동기적으로 실행한다. 호출자(스케줄러)가 스레드를 제공한다.
     */
    public void executeTask(CollectionTask task) {
        runGuarded(task.getId(), () -> {
            CollectionProgress progress = new TaskProgress(taskService, task.getId());
            switch (task.getType()) {
                case PRICE -> priceCollectionService.collect(
                        task.getExchange(), task.getFromDate(), task.getToDate(),
                        progress, DailyPriceFetcher.ADJUSTED_DATA_START);
                case STOCK -> stockCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
                case EVENT -> eventCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
                case INVESTOR -> investorCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
            }
        });
    }

    private void runGuarded(Long taskId, Runnable work) {
        try {
            work.run();
            taskService.complete(taskId);
        } catch (ParallelException e) {
            Throwable cause = e.getCause();
            log.error("수집 실패 (taskId={}): {}", taskId, cause.getMessage(), cause);
            taskService.fail(taskId, resolveErrorCode(cause), cause.getMessage());
        } catch (Exception e) {
            log.error("수집 실패 (taskId={}): {}", taskId, e.getMessage(), e);
            taskService.fail(taskId, resolveErrorCode(e), e.getMessage());
        }
    }

    /**
     * 예외 유형으로 에러 코드를 매핑한다.
     */
    private String resolveErrorCode(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (name.contains("KrxAccessBlocked")) return "KRX_ACCESS_BLOCKED";
        if (name.contains("Krx")) return "KRX_API_ERROR";
        if (name.contains("Kis")) return "KIS_API_ERROR";
        if (name.contains("Feign")) return "EXTERNAL_API_ERROR";
        return "COLLECTION_FAILED";
    }
}
