package com.dove.stockcollection.application.service;

import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 수집 태스크를 PENDING으로 등록하는 진입점.
 */
@Service
@RequiredArgsConstructor
public class CollectionLauncher {

    private final CollectionTaskService taskService;
    private final Clock clock;
    /**
     * KIS 상세 수집 — KisStockDetailFetcher가 없는 컨텍스트(API 등)에선 empty.
     */
    private final Optional<StockDetailCollectionService> stockDetailCollectionService;

    /**
     * 주가 재조회 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     * 재조회는 어제까지로 제한한다.
     *
     * @param adjustedFrom 수정주가 재조회 시작일. null이면 이벤트 감지되어도 재조회 생략.
     * @throws IllegalArgumentException 요청 범위 전체가 오늘 이후인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueuePriceCollection(StockExchange exchange, LocalDate from, LocalDate to,
                                       Long requestedBy, LocalDate adjustedFrom) {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate cappedTo = to.isAfter(yesterday) ? yesterday : to;
        if (from.isAfter(cappedTo)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.PRICE, exchange, from, cappedTo, requestedBy, adjustedFrom);
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
     * 종목 목록 재조회(KRX) 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     *
     * @throws IllegalArgumentException 날짜 범위가 역순인 경우 (INVALID_BACKFILL_RANGE)
     */
    public Long enqueueStockSyncCollection(LocalDate from, LocalDate to, Long requestedBy) {
        if (from.isAfter(to)) throw new IllegalArgumentException("INVALID_BACKFILL_RANGE");
        return taskService.create(CollectionType.STOCK_SYNC, null, from, to, requestedBy);
    }

    /**
     * 종목 상세 재수집(KIS) 태스크를 PENDING으로 등록하고 작업ID를 반환한다.
     * 날짜 무관 — 전 종목 현재 상태를 upsert한다.
     */
    public Long enqueueStockDetailCollection(Long requestedBy) {
        LocalDate today = LocalDate.now(clock);
        return taskService.create(CollectionType.STOCK_DETAIL, null, today, today, requestedBy);
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
            case PRICE -> enqueuePriceCollection(source.getExchange(), from, to, requestedBy, source.getAdjustedFrom());
            case STOCK, STOCK_SYNC -> enqueueStockSyncCollection(from, to, requestedBy);
            case STOCK_DETAIL -> enqueueStockDetailCollection(requestedBy);
            case EVENT -> enqueueEventCollection(from, to, requestedBy);
            case INVESTOR -> enqueueInvestorCollection(from, to, requestedBy);
        };
    }
}
