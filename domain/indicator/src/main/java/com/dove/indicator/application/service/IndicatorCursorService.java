package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.CursorRewoundException;
import com.dove.indicator.domain.entity.IndicatorCursor;
import com.dove.indicator.domain.repository.IndicatorCursorRepository;
import com.dove.indicator.infrastructure.repository.IndicatorCursorRepositorySupport;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 그룹(종목·거래소·가격유형) 지표 커서를 조회·전진·되감기·삭제하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IndicatorCursorService {

    private final IndicatorCursorRepository cursorRepository;
    private final IndicatorCursorRepositorySupport cursorRepositorySupport;

    /**
     * 그룹의 커서를 반환한다. 없으면 비어있다.
     */
    @Transactional(readOnly = true)
    public Optional<IndicatorCursor> findCursor(String ticker, StockExchange exchange, PriceType priceType) {
        return cursorRepository.findByTickerAndExchangeAndPriceType(ticker, exchange, priceType);
    }

    /**
     * 그룹 커서가 expected와 일치할 때만 toDate로 전진한다(compare-and-set). 없으면 새로 생성한다.
     *
     * @param cursorExists 계산 시점에 그룹 커서가 존재했는지
     * @throws CursorRewoundException 커서가 expected와 달라 전진이 거부된 경우
     */
    public void advanceForwardCas(String ticker, StockExchange exchange, PriceType priceType,
                                  LocalDate expected, boolean cursorExists, LocalDate toDate) {
        if (!cursorExists) {
            // 기대=커서 없음 → 새로 생성 (동시 생성은 unique 제약이 막음)
            IndicatorCursor created = new IndicatorCursor(ticker, exchange, priceType);
            created.advance(toDate);
            cursorRepository.save(created);
            return;
        }
        long updated = cursorRepositorySupport.advanceIfMatches(ticker, exchange, priceType, expected, toDate);
        if (updated == 0) {
            throw new CursorRewoundException(ticker, exchange, priceType);
        }
    }

    /**
     * 거래소 전체에서 changedDate 직전보다 뒤에 있는 커서를 일괄로 changedDate 직전까지 되돌린다(가격유형 무관).
     * 종목별 단건 갱신 대신 한 번의 벌크 업데이트로 처리한다. 되돌릴 커서가 없으면 0행.
     */
    public void rewindExchangeBefore(StockExchange exchange, LocalDate changedDate) {
        cursorRepositorySupport.rewindExchangeBefore(exchange, changedDate.minusDays(1));
    }

    /**
     * 해당 종목·거래소의 ADJUSTED 커서를 삭제한다(다음 배치가 처음부터 재계산).
     */
    public void clearAdjusted(String ticker, StockExchange exchange) {
        cursorRepository.deleteByTickerAndExchangeAndPriceType(ticker, exchange, PriceType.ADJUSTED);
    }
}
