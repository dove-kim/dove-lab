package com.dove.technicalindicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.IndicatorCursorId;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.IndicatorCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IndicatorCursorCommandService {

    private final IndicatorCursorRepository indicatorCursorRepository;

    @Transactional
    public IndicatorCursor getOrCreate(MarketType market, String code,
                                       IndicatorType indicatorType, LocalDate initialDate) {
        IndicatorCursorId id = new IndicatorCursorId(market, code, indicatorType);
        return indicatorCursorRepository.findById(id)
                .orElseGet(() -> indicatorCursorRepository.save(
                        new IndicatorCursor(id, initialDate.minusDays(1))));
    }

    @Transactional
    public void rewindIfBefore(MarketType market, String code,
                                IndicatorType indicatorType, LocalDate insertedDate) {
        IndicatorCursorId id = new IndicatorCursorId(market, code, indicatorType);
        indicatorCursorRepository.findById(id).ifPresent(cursor -> {
            if (!insertedDate.isAfter(cursor.getLastCalculatedDate())) {
                cursor.rewindTo(insertedDate.minusDays(1));
            }
        });
    }
}
