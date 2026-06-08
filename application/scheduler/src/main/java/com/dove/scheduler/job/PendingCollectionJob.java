package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.CollectionTaskService;
import com.dove.stockcollection.application.service.InvestorCollectionService;
import com.dove.stockcollection.application.service.PriceCollectionService;
import com.dove.stockcollection.application.service.StockCollectionService;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PENDING 재조회 태스크를 폴링하여 KRX·KIS 트랙으로 분리 실행하는 잡.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingCollectionJob {

    private static final List<CollectionType> KRX_TYPES = List.of(CollectionType.STOCK);
    private static final List<CollectionType> KIS_TYPES = List.of(
            CollectionType.PRICE, CollectionType.EVENT, CollectionType.INVESTOR);

    private final PriceCollectionService priceCollectionService;
    private final StockCollectionService stockCollectionService;
    private final StockEventCollectionService eventCollectionService;
    private final InvestorCollectionService investorCollectionService;
    private final CollectionTaskService taskService;

    private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 기동 시 RUNNING 상태로 남은 태스크를 FAILED로 전환한다(강제 종료 복구).
     */
    @PostConstruct
    void recoverStaleRunning() {
        taskService.failStaleRunning();
    }

    /**
     * 30초마다 PENDING 태스크를 확인하여 KRX·KIS 트랙 각각 하나씩 실행한다.
     * 이미 같은 트랙이 RUNNING 중이면 해당 트랙은 건너뛴다.
     */
    @Scheduled(fixedDelay = 30_000)
    public void poll() {
        if (!taskService.hasRunning(KRX_TYPES)) {
            taskService.findOldestPending(KRX_TYPES).ifPresent(task -> {
                log.info("KRX 트랙 태스크 실행: id={} type={} {}~{}", task.getId(), task.getType(), task.getFromDate(), task.getToDate());
                background.execute(() -> executeTask(task));
            });
        }

        if (!taskService.hasRunning(KIS_TYPES)) {
            taskService.findOldestPending(KIS_TYPES).ifPresent(task -> {
                log.info("KIS 트랙 태스크 실행: id={} type={} {}~{}", task.getId(), task.getType(), task.getFromDate(), task.getToDate());
                background.execute(() -> executeTask(task));
            });
        }
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

    private String resolveErrorCode(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (name.contains("KrxAccessBlocked")) return "KRX_ACCESS_BLOCKED";
        if (name.contains("Krx")) return "KRX_API_ERROR";
        if (name.contains("Kis")) return "KIS_API_ERROR";
        if (name.contains("Feign")) return "EXTERNAL_API_ERROR";
        return "COLLECTION_FAILED";
    }
}
