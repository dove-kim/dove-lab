package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.BreadthCursorRewoundException;
import com.dove.indicator.domain.breadth.entity.BreadthCursor;
import com.dove.indicator.domain.breadth.repository.BreadthCursorRepository;
import com.dove.indicator.infrastructure.repository.BreadthCursorRepositorySupport;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * (universe·가격유형) 상승비율 커서를 조회·전진하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BreadthCursorService {

    private final BreadthCursorRepository cursorRepository;
    private final BreadthCursorRepositorySupport cursorRepositorySupport;

    /**
     * universe의 상승비율 커서를 반환한다. 없으면 비어있다.
     */
    @Transactional(readOnly = true)
    public Optional<BreadthCursor> findCursor(MarketUniverse universe, PriceType priceType) {
        return cursorRepository.findByUniverseAndPriceType(universe, priceType);
    }

    /**
     * universe 커서가 expected와 일치할 때만 toDate로 전진한다(compare-and-set). 없으면 새로 생성한다.
     *
     * @param cursorExists 계산 시점에 커서가 존재했는지
     * @throws BreadthCursorRewoundException 커서가 expected와 달라 전진이 거부된 경우
     */
    public void advanceForwardCas(MarketUniverse universe, PriceType priceType,
                                  LocalDate expected, boolean cursorExists, LocalDate toDate) {
        if (!cursorExists) {
            BreadthCursor created = new BreadthCursor(universe, priceType);
            created.advance(toDate);
            cursorRepository.save(created);
            return;
        }
        long updated = cursorRepositorySupport.advanceIfMatches(universe, priceType, expected, toDate);
        if (updated == 0) {
            throw new BreadthCursorRewoundException(universe, priceType);
        }
    }
}
