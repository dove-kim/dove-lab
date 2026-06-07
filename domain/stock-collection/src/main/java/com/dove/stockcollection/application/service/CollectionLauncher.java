package com.dove.stockcollection.application.service;

import com.dove.concurrent.ParallelException;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 수집 작업을 백그라운드로 띄우는 진입점.
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

    private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 컨텍스트 종료 시 백그라운드 실행기를 닫는다(진행 중 작업 완료 후 정리).
     */
    @PreDestroy
    void shutdown() {
        background.shutdown();
    }

    /**
     * 주가 재조회를 백그라운드로 시작하고 작업ID를 반환한다.
     * 재조회는 어제까지로 제한한다(오늘은 일일 수집 잡 전담 → 두 경로가 같은 날짜를 안 건드림).
     *
     * @throws IllegalArgumentException 요청 범위 전체가 오늘 이후인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long launchPriceCollection(StockExchange exchange, LocalDate from, LocalDate to,
                                      LocalDate adjustedFrom, Long requestedBy) {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate cappedTo = to.isAfter(yesterday) ? yesterday : to;
        if (from.isAfter(cappedTo)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");

        Long taskId = taskService.create(CollectionType.PRICE, exchange, from, cappedTo, requestedBy);
        background.execute(() -> runGuarded(taskId, () ->
                priceCollectionService.collect(exchange, from, cappedTo, new TaskProgress(taskService, taskId), adjustedFrom)));
        return taskId;
    }

    /**
     * 권리 이벤트(KSD) 재조회를 백그라운드로 시작하고 작업ID를 반환한다.
     */
    public Long launchEventCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        Long taskId = taskService.create(CollectionType.EVENT, null, from, to, requestedBy);
        background.execute(() -> runGuarded(taskId, () ->
                eventCollectionService.collect(from, to, new TaskProgress(taskService, taskId))));
        return taskId;
    }

    /**
     * 종목 재조회를 백그라운드로 시작하고 작업ID를 반환한다.
     */
    public Long launchStockCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        Long taskId = taskService.create(CollectionType.STOCK, null, from, to, requestedBy);
        background.execute(() -> runGuarded(taskId, () ->
                stockCollectionService.collect(from, to, new TaskProgress(taskService, taskId))));
        return taskId;
    }

    /**
     * 투자자매매동향 재조회를 백그라운드로 시작하고 작업ID를 반환한다.
     */
    public Long launchInvestorCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        Long taskId = taskService.create(CollectionType.INVESTOR, null, from, to, requestedBy);
        background.execute(() -> runGuarded(taskId, () ->
                investorCollectionService.collect(from, to, new TaskProgress(taskService, taskId))));
        return taskId;
    }

    /**
     * 실패한(또는 임의의) 작업을 같은 범위로 다시 실행한다.
     */
    public Long relaunch(Long sourceTaskId, Long requestedBy) {
        CollectionTask source = taskService.find(sourceTaskId)
                .orElseThrow(() -> new IllegalArgumentException("TASK_NOT_FOUND"));
        LocalDate from = source.getFromDate();
        LocalDate to = source.getToDate();
        return switch (source.getType()) {
            // 재시도는 정확성 우선 — 전체 ADJUSTED 재조회.
            case PRICE -> launchPriceCollection(source.getExchange(), from, to,
                    DailyPriceFetcher.ADJUSTED_DATA_START, requestedBy);
            case STOCK -> launchStockCollection(from, to, requestedBy);
            case EVENT -> launchEventCollection(from, to, requestedBy);
            case INVESTOR -> launchInvestorCollection(from, to, requestedBy);
        };
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
