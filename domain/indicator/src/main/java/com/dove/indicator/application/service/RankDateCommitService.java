package com.dove.indicator.application.service;

import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 한 거래일의 순위 행을 저장하고 universe 커서를 그 거래일로 CAS 전진하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class RankDateCommitService {

    private final StockRankDailyService rankDailyService;
    private final RankCursorService cursorService;

    /**
     * 한 거래일의 순위 행(native 거래소로 키됨)을 저장하고 그 거래일로 universe 커서를 CAS 전진한다.
     * 예외 발생 시 저장도 함께 롤백된다.
     *
     * @param expected     계산 시작 시점의 커서값(CAS 기준)
     * @param cursorExists 계산 시점에 커서가 존재했는지
     */
    @Transactional
    public void commit(MarketUniverse universe, PriceType priceType, List<StockRankDaily> rows,
                       LocalDate expected, boolean cursorExists, LocalDate tradeDate) {
        rankDailyService.saveAll(rows);
        cursorService.advanceForwardCas(universe, priceType, expected, cursorExists, tradeDate);
    }
}
