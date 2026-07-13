package com.dove.api.search.stock.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.search.stock.dto.ModelScoreBar;
import com.dove.api.search.stock.dto.ModelSummaryResponse;
import com.dove.modelserving.application.service.ModelQueryService;
import com.dove.modelserving.application.service.ModelScoreQueryService;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.stock.application.service.StockQueryService;
import com.dove.userfeature.application.service.MemberModelGrantQueryService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 종목별 모델 채점 점수 시계열·활성 모델 목록 조회 API.
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@RequireCapability(Capability.MODEL_SCORE)
public class ModelScoreController {

    private final StockQueryService stockQueryService;
    private final ModelScoreQueryService modelScoreQueryService;
    private final ModelQueryService modelQueryService;
    private final MemberModelGrantQueryService modelGrantQueryService;

    /**
     * 한 종목·모델의 점수 시계열을 거래일 오름차순으로 반환한다.
     *
     * @param modelId 조회할 모델 식별자
     * @param from    조회 시작일 (inclusive, 생략 시 무제한)
     * @param to      조회 종료일 (inclusive, 생략 시 무제한)
     * @throws ResponseStatusException 종목이 없으면 404 STOCK_NOT_FOUND, 모델이 미부여면 403 MODEL_NOT_GRANTED
     */
    @GetMapping("/{ticker}/scores")
    public List<ModelScoreBar> getScores(
            @PathVariable String ticker,
            @RequestParam Long modelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (stockQueryService.findByTicker(ticker).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND");
        }
        if (!isVisible(user, modelId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MODEL_NOT_GRANTED");
        }
        return modelScoreQueryService.findByModelTickerAndDateRange(modelId, ticker, from, to).stream()
                .map(ModelScoreBar::from)
                .toList();
    }

    /**
     * 점수 조회·필터 선택용 활성 모델 목록을 반환한다. ROOT는 전체, 그 외는 부여받은 모델만.
     */
    @GetMapping("/models")
    public List<ModelSummaryResponse> getActiveModels(@AuthenticationPrincipal AuthenticatedUser user) {
        boolean root = "ROOT".equals(user.role());
        Set<Long> granted = root ? Set.of() : modelGrantQueryService.findGrantedModelIds(user.memberId());
        return modelQueryService.findByStatus(ModelStatus.ACTIVE).stream()
                .filter(m -> root || granted.contains(m.getId()))
                .map(ModelSummaryResponse::from)
                .toList();
    }

    /**
     * 사용자가 해당 모델 점수를 볼 수 있는지 여부를 반환한다. ROOT는 전체, 그 외는 부여받은 모델만.
     */
    private boolean isVisible(AuthenticatedUser user, Long modelId) {
        return "ROOT".equals(user.role()) || modelGrantQueryService.hasGrant(user.memberId(), modelId);
    }
}
