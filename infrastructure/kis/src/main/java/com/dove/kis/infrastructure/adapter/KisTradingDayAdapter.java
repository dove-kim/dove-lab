package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisHolidayResponse;
import com.dove.kis.quota.KisGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 국내휴장일조회(CTCA0903R)로 영업일 여부를 확인한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisTradingDayAdapter {

    private static final String TR_ID = "CTCA0903R";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisStockClient kisStockClient;
    private final KisGate kisGate;

    /**
     * 지정 날짜가 국내 주식 개장일(영업일)인지 확인한다.
     * 조회 실패 시 안전하게 휴장(false)으로 처리한다.
     */
    public boolean isTradingDay(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        try {
            KisHolidayResponse resp = kisGate.call(
                    () -> kisStockClient.getHoliday(TR_ID, dateStr, "", ""));
            if (resp == null || !resp.isSuccess() || resp.getOutput() == null) {
                log.warn("휴장일 조회 응답 없음 ({}), 휴장으로 처리", date);
                return false;
            }
            List<KisHolidayResponse.KisHolidayItem> items = resp.getOutput();
            boolean open = items.stream()
                    .filter(item -> dateStr.equals(item.getBassDate()))
                    .findFirst()
                    .map(KisHolidayResponse.KisHolidayItem::isOpen)
                    .orElse(false);
            log.info("영업일 확인 ({}): {}", date, open ? "개장일" : "휴장일");
            return open;
        } catch (Exception e) {
            log.warn("휴장일 조회 실패 ({}), 휴장으로 처리: {}", date, e.getMessage());
            return false;
        }
    }
}
