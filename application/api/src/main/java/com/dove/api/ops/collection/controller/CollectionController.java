package com.dove.api.ops.collection.controller;

import com.dove.api.global.dto.PageResponse;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.ops.collection.dto.CollectionTaskResponse;
import com.dove.api.ops.collection.dto.EventCollectionRequest;
import com.dove.api.ops.collection.dto.InvestorCollectionRequest;
import com.dove.api.ops.collection.dto.PriceCollectionRequest;
import com.dove.api.ops.collection.dto.StockCollectionRequest;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.service.CollectionLauncher;
import com.dove.stockcollection.application.service.CollectionTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

/**
 * ROOT 전용 — 수집 재조회 관리 API.
 */
@RestController
@RequestMapping("/admin/ops/collection")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class CollectionController {

    private final CollectionLauncher launcher;
    private final CollectionTaskService taskService;

    /**
     * 주가 재조회 시작 → 작업ID 반환.
     */
    @PostMapping("/price")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> collectPrice(@Valid @RequestBody PriceCollectionRequest req,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        StockExchange exchange;
        try {
            exchange = StockExchange.valueOf(req.exchange().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EXCHANGE");
        }
        LocalDate adjustedFrom = req.adjustedFromYear() != null
                ? LocalDate.of(req.adjustedFromYear(), 1, 1)
                : null;
        Long taskId = launcher.enqueuePriceCollection(exchange, req.from(), req.to(), user.memberId(), adjustedFrom);
        return Map.of("taskId", taskId);
    }

    /**
     * 권리 이벤트(KSD) 재조회 시작 → 작업ID 반환.
     */
    @PostMapping("/event")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> collectEvent(@Valid @RequestBody EventCollectionRequest req,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        Long taskId = launcher.enqueueEventCollection(req.from(), req.to(), user.memberId());
        return Map.of("taskId", taskId);
    }

    /**
     * 종목 목록(KRX) 재조회 시작 → 작업ID 반환.
     */
    @PostMapping("/stock-sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> collectStockSync(@Valid @RequestBody StockCollectionRequest req,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        Long taskId = launcher.enqueueStockSyncCollection(req.from(), req.to(), user.memberId());
        return Map.of("taskId", taskId);
    }

    /**
     * 종목 상세(KIS) 재수집 시작 → 작업ID 반환. 날짜 무관.
     */
    @PostMapping("/stock-detail")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> collectStockDetail(@AuthenticationPrincipal AuthenticatedUser user) {
        Long taskId = launcher.enqueueStockDetailCollection(user.memberId());
        return Map.of("taskId", taskId);
    }

    /**
     * 투자자매매동향 재조회 시작 → 작업ID 반환.
     */
    @PostMapping("/investor")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> collectInvestor(@Valid @RequestBody InvestorCollectionRequest req,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        Long taskId = launcher.enqueueInvestorCollection(req.from(), req.to(), user.memberId());
        return Map.of("taskId", taskId);
    }

    /**
     * 작업 목록 (최신순, 페이지).
     */
    @GetMapping("/tasks")
    public PageResponse<CollectionTaskResponse> tasks(Pageable pageable) {
        return PageResponse.from(taskService.findRecent(pageable).map(CollectionTaskResponse::from));
    }

    /**
     * 단일 작업 상태.
     */
    @GetMapping("/tasks/{id}")
    public CollectionTaskResponse task(@PathVariable Long id) {
        return taskService.find(id)
                .map(CollectionTaskResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND"));
    }

    /**
     * 실패 작업 재시도 → 새 작업ID 반환.
     */
    @PostMapping("/tasks/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> retry(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        if (taskService.find(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND");
        }
        Long newTaskId = launcher.reenqueue(id, user.memberId());
        return Map.of("taskId", newTaskId);
    }
}
