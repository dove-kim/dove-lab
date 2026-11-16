package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisOverseasClient;
import com.dove.kis.infrastructure.client.dto.KisOverseasDailyOutput;
import com.dove.kis.infrastructure.client.dto.KisOverseasDailyResponse;
import com.dove.kis.infrastructure.client.dto.KisOverseasPriceResponse;
import com.dove.kis.quota.KisGate;
import com.dove.portfolio.application.port.OverseasClose;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KIS 해외주식 현재체결가 기반 해외 종가 포트 어댑터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisOverseasPriceAdapter implements OverseasPricePort {

    private static final String TR_PRICE = "HHDFS00000300";
    private static final String TR_DAILY = "HHDFS76240000";
    private static final String AUTH = "";
    private static final String GUBN_DAY = "0";
    private static final String MODP_ADJUSTED = "1";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 한 종목당 연속조회 상한 — 100건씩이므로 넉넉히 50페이지(약 20년). */
    private static final int MAX_PAGES = 50;
    /** 빈 응답을 만났을 때 기준일을 물릴 일수(휴장 연휴 흡수). */
    private static final int BLANK_STEP_DAYS = 7;
    /** 연속 빈 응답 허용 횟수 — 넘으면 더 과거 데이터가 없다고 본다. */
    private static final int MAX_BLANK_PAGES = 3;

    private final KisOverseasClient client;
    private final KisGate kisGate;

    @Override
    public Optional<BigDecimal> fetchClose(PortfolioMarket market, String ticker) {
        String excd = excd(market);
        if (excd == null) {
            return Optional.empty();
        }
        try {
            KisOverseasPriceResponse res = kisGate.call(() -> client.getPrice(TR_PRICE, AUTH, excd, ticker));
            if (res == null || !res.isSuccess() || res.getOutput() == null) {
                return Optional.empty();
            }
            BigDecimal last = res.getOutput().getLastDecimal();
            if (last == null || last.signum() <= 0) {
                return Optional.empty();
            }
            return Optional.of(last);
        } catch (RuntimeException e) {
            log.warn("KIS overseas price fetch failed {} {}: {}", excd, ticker, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<OverseasClose> fetchDailyCloses(PortfolioMarket market, String ticker, LocalDate from) {
        String excd = excd(market);
        if (excd == null) {
            return List.of();
        }
        Map<LocalDate, BigDecimal> collected = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now();
        int blanks = 0;
        // 기준일을 과거로 밀며 연속 조회 — 휴장일·장 시작 전에는 빈 페이지가 오므로 며칠 물러나 다시 본다.
        for (int page = 0; page < MAX_PAGES && !cursor.isBefore(from); page++) {
            List<KisOverseasDailyOutput> rows = fetchPage(excd, ticker, cursor);
            if (rows.isEmpty()) {
                if (++blanks > MAX_BLANK_PAGES) {
                    break;
                }
                cursor = cursor.minusDays(BLANK_STEP_DAYS);
                continue;
            }
            blanks = 0;
            LocalDate oldest = null;
            for (KisOverseasDailyOutput row : rows) {
                LocalDate date = row.getDateValue();
                BigDecimal close = row.getCloseValue();
                if (date == null || close == null) {
                    continue;
                }
                if (oldest == null || date.isBefore(oldest)) {
                    oldest = date;
                }
                if (!date.isBefore(from)) {
                    collected.putIfAbsent(date, close);
                }
            }
            if (oldest == null) {
                break;
            }
            cursor = oldest.minusDays(1);
        }
        return collected.entrySet().stream()
                .map(e -> new OverseasClose(e.getKey(), e.getValue()))
                .toList();
    }

    /** 기준일 한 페이지(최대 100건)를 받아온다. 실패하면 빈 목록. */
    private List<KisOverseasDailyOutput> fetchPage(String excd, String ticker, LocalDate bymd) {
        try {
            KisOverseasDailyResponse res = kisGate.call(
                    () -> client.getDailyPrice(TR_DAILY, AUTH, excd, ticker, GUBN_DAY, bymd.format(YMD), MODP_ADJUSTED));
            if (res == null) {
                log.warn("KIS overseas daily returned null {} {} {}", excd, ticker, bymd);
                return List.of();
            }
            if (!res.isSuccess()) {
                log.warn("KIS overseas daily rejected {} {} {}: rt_cd={} msg_cd={} msg={}",
                        excd, ticker, bymd, res.getResultCode(), res.getMessageCode(), res.getMessage());
                return List.of();
            }
            if (res.getOutputs() == null || res.getOutputs().isEmpty()) {
                log.info("KIS overseas daily empty page {} {} {}", excd, ticker, bymd);
                return List.of();
            }
            return res.getOutputs();
        } catch (RuntimeException e) {
            log.warn("KIS overseas daily fetch failed {} {} {}: {}", excd, ticker, bymd, e.getMessage());
            return List.of();
        }
    }

    private String excd(PortfolioMarket market) {
        return switch (market) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            case AMEX -> "AMS";
            case HKEX -> "HKS";
            case TSE -> "TSE";
            case SSE -> "SHS";
            case SZSE -> "SZS";
            default -> null;
        };
    }
}
