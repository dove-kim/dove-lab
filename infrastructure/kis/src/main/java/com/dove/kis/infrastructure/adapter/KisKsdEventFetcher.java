package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisKsdResponse;
import com.dove.kis.quota.KisGate;
import com.dove.stock.domain.enums.StockEventType;
import com.dove.stockcollection.application.port.KsdEventFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * KIS 예탁원정보(KSD) 권리 이벤트 어댑터.
 */
@Component
@RequiredArgsConstructor
public class KisKsdEventFetcher implements KsdEventFetcher {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PAGE_LIMIT = 100;     // KSD 호출당 최대 행

    private final KisStockClient client;
    private final KisGate gate;

    @Override
    public List<Map<String, Object>> fetch(StockEventType type, LocalDate from, LocalDate to, String sht) {
        List<Map<String, Object>> acc = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        collectRange(type, from, to, sht, acc, seen);
        return acc;
    }

    /**
     * 구간을 조회하되 100행 상한이면 반으로 분할해 재귀한다.
     */
    private void collectRange(StockEventType type, LocalDate from, LocalDate to, String sht,
                              List<Map<String, Object>> acc, Set<String> seen) {
        List<Map<String, Object>> page = rowsOf(callRange(type, from, to, sht));
        addNew(page, acc, seen);
        if (page.size() < PAGE_LIMIT) return;
        if (from.equals(to)) return;   // 단일일 100 상한 — 더 분할 불가
        LocalDate mid = from.plusDays((to.toEpochDay() - from.toEpochDay()) / 2);
        collectRange(type, from, mid, sht, acc, seen);
        collectRange(type, mid.plusDays(1), to, sht, acc, seen);
    }

    /** 미수집 행만 누적. 키=(record_date,sht_cd). 추가한 건수 반환. */
    private int addNew(List<Map<String, Object>> page, List<Map<String, Object>> acc, Set<String> seen) {
        int added = 0;
        for (Map<String, Object> row : page) {
            String key = str(row, "record_date") + "|" + str(row, "sht_cd");
            if (seen.add(key)) { acc.add(row); added++; }
        }
        return added;
    }

    private KisKsdResponse callRange(StockEventType type, LocalDate from, LocalDate to, String sht) {
        String f = from.format(YMD);
        String t = to.format(YMD);
        return call(switch (type) {
            case DIVIDEND     -> () -> client.getKsdDividend("HHKDB669102C0", "", "0", f, t, sht, "");
            case RIGHTS_ISSUE -> () -> client.getKsdPaidinCapin("HHKDB669100C0", "", "2", f, t, sht);
            case BONUS_ISSUE  -> () -> client.getKsdBonusIssue("HHKDB669101C0", "", f, t, sht);
            case MERGER_SPLIT -> () -> client.getKsdMergerSplit("HHKDB669104C0", "", f, t, sht);
            case PAR_CHANGE   -> () -> client.getKsdRevSplit("HHKDB669105C0", sht, "", f, t, "0");
            case CAP_REDUCTION -> () -> client.getKsdCapDcrs("HHKDB669106C0", "", f, t, sht);
        });
    }

    private KisKsdResponse call(Supplier<KisKsdResponse> req) {
        return gate.call(req::get);
    }

    private List<Map<String, Object>> rowsOf(KisKsdResponse resp) {
        if (resp == null || !resp.isSuccess() || resp.rows() == null) return List.of();
        return resp.rows();
    }

    private static String str(Map<String, Object> r, String key) {
        Object v = r.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
