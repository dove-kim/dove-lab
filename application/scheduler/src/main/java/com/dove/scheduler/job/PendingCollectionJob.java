package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.fundamental.domain.enums.ReportCode;
import com.dove.scheduler.fundamental.DailyValuationService;
import com.dove.scheduler.fundamental.FundamentalCollectionService;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.CollectionTaskService;
import com.dove.stockcollection.application.service.InvestorCollectionService;
import com.dove.stockcollection.application.service.PriceCollectionService;
import com.dove.stockcollection.application.service.StockCollectionService;
import com.dove.stockcollection.application.service.StockDetailCollectionService;
import com.dove.stockcollection.application.service.StockEventCollectionService;
import com.dove.stockcollection.application.service.TaskProgress;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PENDING 재조회 태스크를 폴링하여 유형에 상관없이 한 번에 하나씩 직렬 실행하는 잡.
 * STOCK 유형이 KIS를 함께 사용하므로 PRICE·EVENT와 병렬 실행 시 율제한이 충돌한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingCollectionJob {

    private static final List<CollectionType> ALL_TYPES = Arrays.asList(CollectionType.values());

    private final PriceCollectionService priceCollectionService;
    private final StockCollectionService stockCollectionService;
    private final StockEventCollectionService eventCollectionService;
    private final InvestorCollectionService investorCollectionService;
    private final FundamentalCollectionService fundamentalCollectionService;
    private final DailyValuationService dailyValuationService;
    private final CollectionTaskService taskService;
    /**
     * KIS 상세 수집 — KisStockDetailFetcher가 없는 컨텍스트에선 empty.
     */
    private final Optional<StockDetailCollectionService> stockDetailCollectionService;

    private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 기동 시 RUNNING 상태로 남은 태스크를 FAILED로 전환한다(강제 종료 복구).
     */
    @PostConstruct
    void recoverStaleRunning() {
        taskService.failStaleRunning();
    }

    /**
     * 30초마다 PENDING 태스크를 확인하여 실행 중인 태스크가 없을 때 가장 오래된 태스크를 하나 실행한다.
     */
    @Scheduled(fixedDelay = 30_000)
    public void poll() {
        if (taskService.hasRunning(ALL_TYPES)) return;
        taskService.findOldestPending(ALL_TYPES).ifPresent(task -> {
            log.info("재조회 태스크 실행: id={} type={} {}~{}", task.getId(), task.getType(), task.getFromDate(), task.getToDate());
            background.execute(() -> executeTask(task));
        });
    }

    @PreDestroy
    void shutdown() {
        background.shutdown();
    }

    private void executeTask(CollectionTask task) {
        runGuarded(task.getId(), () -> {
            CollectionProgress progress = new TaskProgress(taskService, task.getId());
            switch (task.getType()) {
                case PRICE -> priceCollectionService.collect(
                        task.getExchange(), task.getFromDate(), task.getToDate(),
                        progress, task.getAdjustedFrom()); // null이면 수정주가 재조회 생략
                case STOCK -> {
                    // legacy: KRX + KIS 통합 (기존 레코드 역호환)
                    AtomicInteger krxTotal = new AtomicInteger();
                    stockCollectionService.collect(task.getFromDate(), task.getToDate(),
                            CollectionProgress.capturing(progress, krxTotal::set));
                    stockDetailCollectionService.ifPresent(s ->
                            s.updateAll(CollectionProgress.offset(progress, krxTotal.get())));
                }
                case STOCK_SYNC ->
                    // KRX 종목 목록 동기화만
                    stockCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
                case STOCK_DETAIL ->
                    // KIS 종목 상세 upsert만 (날짜 무관)
                    stockDetailCollectionService.ifPresent(s -> s.updateAll(progress));
                case EVENT -> eventCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
                case INVESTOR -> investorCollectionService.collect(task.getFromDate(), task.getToDate(), progress);
                case FUNDAMENTAL -> fundamentalCollectionService.backfill(
                        task.getFromDate().getYear(), task.getToDate().getYear(),
                        List.of(ReportCode.ANNUAL), progress);
                case VALUATION -> dailyValuationService.computeRange(
                        task.getFromDate(), task.getToDate(), progress);
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

    private String resolveErrorCode(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (name.contains("KrxAccessBlocked")) return "KRX_ACCESS_BLOCKED";
        if (name.contains("Krx")) return "KRX_API_ERROR";
        if (name.contains("Kis")) return "KIS_API_ERROR";
        if (name.contains("Feign")) return "EXTERNAL_API_ERROR";
        return "COLLECTION_FAILED";
    }
}
