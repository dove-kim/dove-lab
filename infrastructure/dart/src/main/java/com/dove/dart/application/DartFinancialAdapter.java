package com.dove.dart.application;

import com.dove.dart.application.dto.DartDisclosure;
import com.dove.dart.application.dto.FinancialStatement;
import com.dove.dart.config.DartProperties;
import com.dove.dart.infrastructure.client.DartClient;
import com.dove.dart.infrastructure.client.dto.DartListItem;
import com.dove.dart.infrastructure.client.dto.DartListResponse;
import com.dove.dart.infrastructure.client.dto.FnlttItem;
import com.dove.dart.infrastructure.client.dto.FnlttResponse;
import com.dove.dart.infrastructure.client.dto.StockTotalsItem;
import com.dove.dart.infrastructure.client.dto.StockTotalsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DART OpenAPI 어댑터 — 재무제표·주식총수·정기공시를 도메인 친화 형태로 반환한다.
 */
@Component
@RequiredArgsConstructor
public class DartFinancialAdapter {

    private static final String OK = "000";
    private static final String RATE_LIMIT = "020";
    private static final String[] FS_ORDER = {"CFS", "OFS"};
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final DartClient client;
    private final DartProperties properties;
    private final DartApiQuotaService quotaService;

    /**
     * 전체 재무제표를 연결(CFS) 우선, 없으면 별도(OFS)로 조회해 표준계정코드→금액 맵으로 반환한다.
     *
     * @throws DartRateLimitException 일일 사용한도 초과 시
     */
    public Optional<FinancialStatement> fetchStatement(String corpCode, int businessYear, String reportCode) {
        for (String fsDiv : FS_ORDER) {
            quotaService.requireQuota();
            FnlttResponse res = client.getFinancialStatements(
                    properties.getApiKey(), corpCode, String.valueOf(businessYear), reportCode, fsDiv);
            requireNotRateLimited(res.status(), res.message());
            if (!OK.equals(res.status()) || res.list() == null || res.list().isEmpty()) {
                continue;
            }
            Map<String, Long> amounts = extractAmounts(res.list());
            if (amounts.isEmpty()) {
                continue;
            }
            String rceptNo = res.list().get(0).rceptNo();
            return Optional.of(new FinancialStatement(
                    corpCode, rceptNo, parseRceptDate(rceptNo), reportCode, fsDiv, amounts));
        }
        return Optional.empty();
    }

    /**
     * 보통주 상장주식수(현재까지 발행 − 감소)를 조회한다.
     *
     * @throws DartRateLimitException 일일 사용한도 초과 시
     */
    public Optional<Long> fetchCommonShares(String corpCode, int businessYear, String reportCode) {
        quotaService.requireQuota();
        StockTotalsResponse res = client.getStockTotals(
                properties.getApiKey(), corpCode, String.valueOf(businessYear), reportCode);
        requireNotRateLimited(res.status(), res.message());
        if (!OK.equals(res.status()) || res.list() == null) {
            return Optional.empty();
        }
        for (StockTotalsItem item : res.list()) {
            if ("보통주".equals(trim(item.se()))) {
                Long issued = parseAmount(item.nowToIsuStockQty());
                if (issued == null) {
                    return Optional.empty();
                }
                Long decreased = parseAmount(item.nowToDcrsStockQty());
                return Optional.of(issued - (decreased != null ? decreased : 0L));
            }
        }
        return Optional.empty();
    }

    /**
     * 시장 전체의 기간 내 정기공시를 한 번에 조회한다(종목별 호출 없이 날짜로 훑기 — 일일 폴링용).
     *
     * @throws DartRateLimitException 일일 사용한도 초과 시
     */
    public List<DartDisclosure> fetchRecentPeriodicDisclosures(LocalDate from, LocalDate to) {
        return fetchPeriodicDisclosures("", from, to);
    }

    /**
     * 기간 내 정기공시(사업·반기·분기보고서, 신규·정정)를 조회한다. corpCode가 빈 문자열이면 시장 전체.
     *
     * @throws DartRateLimitException 일일 사용한도 초과 시
     */
    public List<DartDisclosure> fetchPeriodicDisclosures(String corpCode, LocalDate from, LocalDate to) {
        List<DartDisclosure> out = new ArrayList<>();
        int page = 1;
        int totalPage = 1;
        do {
            quotaService.requireQuota();
            DartListResponse res = client.getDisclosureList(
                    properties.getApiKey(), corpCode,
                    from.format(YYYYMMDD), to.format(YYYYMMDD), "A", page, 100);
            requireNotRateLimited(res.status(), res.message());
            if (!OK.equals(res.status()) || res.list() == null) {
                break;
            }
            totalPage = res.totalPage() != null ? res.totalPage() : 1;
            for (DartListItem item : res.list()) {
                String nm = item.reportNm() != null ? item.reportNm() : "";
                if (!nm.contains("보고서")) {
                    continue;
                }
                out.add(new DartDisclosure(
                        item.corpCode(), item.stockCode(), item.rceptNo(),
                        parseRceptDate(item.rceptNo()), nm, nm.contains("정정")));
            }
            page++;
        } while (page <= totalPage);
        return out;
    }

    private Map<String, Long> extractAmounts(List<FnlttItem> items) {
        Map<String, Long> amounts = new HashMap<>();
        for (FnlttItem item : items) {
            String sj = item.sjDiv();
            if (!("BS".equals(sj) || "IS".equals(sj) || "CIS".equals(sj) || "CF".equals(sj))) {
                continue;
            }
            String accountId = item.accountId();
            Long amount = parseAmount(item.thstrmAmount());
            if (accountId == null || accountId.isBlank() || amount == null) {
                continue;
            }
            amounts.putIfAbsent(accountId, amount);
        }
        return amounts;
    }

    private void requireNotRateLimited(String status, String message) {
        if (RATE_LIMIT.equals(status)) {
            quotaService.markRemoteLimited();
            throw new DartRateLimitException("DART_RATE_LIMIT: " + message);
        }
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static Long parseAmount(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.replace(",", "").replace(" ", "").trim();
        if (t.isEmpty() || "-".equals(t)) {
            return null;
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseRceptDate(String rceptNo) {
        return LocalDate.parse(rceptNo.substring(0, 8), YYYYMMDD);
    }
}
