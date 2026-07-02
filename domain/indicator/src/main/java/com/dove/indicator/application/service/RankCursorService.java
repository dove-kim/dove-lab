package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.RankCursorRewoundException;
import com.dove.indicator.domain.rank.entity.RankCursor;
import com.dove.indicator.domain.rank.repository.RankCursorRepository;
import com.dove.indicator.infrastructure.repository.RankCursorRepositorySupport;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * (universe·가격유형) 순위 커서를 조회·전진하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RankCursorService {

    private final RankCursorRepository cursorRepository;
    private final RankCursorRepositorySupport cursorRepositorySupport;

    /**
     * universe의 순위 커서를 반환한다. 없으면 비어있다.
     */
    @Transactional(readOnly = true)
    public Optional<RankCursor> findCursor(MarketUniverse universe, PriceType priceType) {
        return cursorRepository.findByUniverseAndPriceType(universe, priceType);
    }

    /**
     * universe 커서가 expected와 일치할 때만 toDate로 전진한다(compare-and-set). 없으면 새로 생성한다.
     *
     * @param cursorExists 계산 시점에 커서가 존재했는지
     * @throws RankCursorRewoundException 커서가 expected와 달라 전진이 거부된 경우
     */
    public void advanceForwardCas(MarketUniverse universe, PriceType priceType,
                                  LocalDate expected, boolean cursorExists, LocalDate toDate) {
        if (!cursorExists) {
            RankCursor created = new RankCursor(universe, priceType);
            created.advance(toDate);
            cursorRepository.save(created);
            return;
        }
        long updated = cursorRepositorySupport.advanceIfMatches(universe, priceType, expected, toDate);
        if (updated == 0) {
            throw new RankCursorRewoundException(universe, priceType);
        }
    }
}
