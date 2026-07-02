package com.dove.scheduler.fundamental;

import com.dove.dart.application.CorpCodeDownloader;
import com.dove.dart.application.DartFinancialAdapter;
import com.dove.dart.application.DartRateLimitException;
import com.dove.dart.application.dto.CorpMapping;
import com.dove.dart.application.dto.DartDisclosure;
import com.dove.dart.application.dto.FinancialStatement;
import com.dove.fundamental.application.FundamentalCommandService;
import com.dove.fundamental.application.FundamentalFactory;
import com.dove.fundamental.application.FundamentalQueryService;
import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import com.dove.fundamental.domain.enums.ReportCode;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stockcollection.application.service.CollectionProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DART 재무제표 수집 — 고유번호 동기화, 과거 백필(재개 가능), 신규·정정 공시 폴링.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalCollectionService {

    private final CorpCodeDownloader corpCodeDownloader;
    private final DartFinancialAdapter dartAdapter;
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final FundamentalQueryService fundamentalQueryService;
    private final FundamentalCommandService fundamentalCommandService;

    /**
     * DART 고유번호를 내려받아 보유 종목(STOCK)에 매핑·저장한다.
     *
     * @return 매핑된 종목 수
     */
    public int syncCorpCodes() {
        List<CorpMapping> mappings = corpCodeDownloader.download();
        Map<String, String> byTicker = mappings.stream()
                .collect(Collectors.toMap(CorpMapping::stockCode, CorpMapping::corpCode, (a, b) -> a));
        int matched = stockCommandService.assignCorpCodes(byTicker);
        log.info("DART 고유번호 매핑 완료: {}/{}", matched, mappings.size());
        return matched;
    }

    /**
     * corp_code 보유 종목의 [fromYear..toYear] × 보고서들을 백필한다.
     * 이미 수집된 (종목·연도·보고서)는 건너뛰고, 일일 한도 초과 시 저장된 지점까지 두고 중단한다(다음 실행에서 재개).
     *
     * @return 신규 저장 건수
     */
    public int backfill(int fromYear, int toYear, List<ReportCode> reports, CollectionProgress progress) {
        List<Stock> targets = stockQueryService.findWithCorpCode();
        if (targets.isEmpty()) {
            syncCorpCodes();
            targets = stockQueryService.findWithCorpCode();
        }
        int totalUnits = targets.size() * (toYear - fromYear + 1) * reports.size();
        progress.onTotal(totalUnits);
        log.info("재무 백필 시작: 종목 {} × 연도 {}~{} × 보고서 {}", targets.size(), fromYear, toYear, reports.size());

        int saved = 0;
        int done = 0;
        try {
            for (Stock stock : targets) {
                for (int year = fromYear; year <= toYear; year++) {
                    for (ReportCode report : reports) {
                        done++;
                        if (fundamentalQueryService.existsStatement(
                                stock.getTicker(), (short) year, report.code())) {
                            progress.onProgress(done);
                            continue;
                        }
                        if (collectOne(stock, year, report, false)) {
                            saved++;
                        }
                        progress.onProgress(done);
                    }
                }
            }
        } catch (DartRateLimitException e) {
            log.info("DART 일일한도 도달 — 백필 중단(다음날 재개). 신규 {}", saved);
        }
        log.info("재무 백필 종료: 신규 {}건", saved);
        return saved;
    }

    /**
     * 기간 내 신규·정정 정기공시를 폴링해 아직 없는 것만 수집한다(일일 잡).
     *
     * @return 신규 저장 건수
     */
    public int pollRecent(LocalDate from, LocalDate to) {
        List<Stock> stocks = stockQueryService.findWithCorpCode();
        if (stocks.isEmpty()) {
            // 최초 배포 직후 등 매핑 전이면 자가 부트스트랩 — 주간 corp-sync를 기다리지 않음
            syncCorpCodes();
            stocks = stockQueryService.findWithCorpCode();
        }
        Map<String, Stock> byTicker = stocks.stream()
                .collect(Collectors.toMap(Stock::getTicker, Function.identity(), (a, b) -> a));
        int saved = 0;
        try {
            // 시장 전체 공시를 한 번에 훑고(종목별 호출 없음) 우리 종목만 필터
            for (DartDisclosure d : dartAdapter.fetchRecentPeriodicDisclosures(from, to)) {
                Stock stock = byTicker.get(d.stockCode());
                if (stock == null) {
                    continue;
                }
                if (fundamentalQueryService.existsStatement(d.rceptNo(), FinancialStatementDiv.CFS)
                        || fundamentalQueryService.existsStatement(d.rceptNo(), FinancialStatementDiv.OFS)) {
                    continue;
                }
                int year = d.rceptDt().getYear();
                for (ReportCode report : ReportCode.values()) {
                    if (collectOneByRcept(stock, year, report, d.rceptNo(), d.amendment())) {
                        saved++;
                        break;
                    }
                }
            }
        } catch (DartRateLimitException e) {
            log.info("DART 일일한도 도달 — 폴링 중단. 신규 {}", saved);
        }
        return saved;
    }

    private boolean collectOne(Stock stock, int year, ReportCode report, boolean amendment) {
        Optional<FinancialStatement> statement = dartAdapter.fetchStatement(stock.getCorpCode(), year, report.code());
        if (statement.isEmpty()) {
            return false;
        }
        Long shares = dartAdapter.fetchCommonShares(stock.getCorpCode(), year, report.code()).orElse(null);
        save(stock, year, report, statement.get(), amendment, shares);
        return true;
    }

    private boolean collectOneByRcept(Stock stock, int year, ReportCode report, String rceptNo, boolean amendment) {
        Optional<FinancialStatement> statement = dartAdapter.fetchStatement(stock.getCorpCode(), year, report.code());
        if (statement.isEmpty() || !rceptNo.equals(statement.get().rceptNo())) {
            return false;
        }
        Long shares = dartAdapter.fetchCommonShares(stock.getCorpCode(), year, report.code()).orElse(null);
        save(stock, year, report, statement.get(), amendment, shares);
        return true;
    }

    private void save(Stock stock, int year, ReportCode report, FinancialStatement statement,
                      boolean amendment, Long shares) {
        StockFundamental entity = FundamentalFactory.fromAccounts(
                stock.getTicker(), stock.getCorpCode(), (short) year, report.code(),
                statement.rceptNo(), statement.rceptDt(),
                FinancialStatementDiv.valueOf(statement.fsDiv()), amendment,
                statement.amounts(), shares);
        fundamentalCommandService.saveStatement(entity);
    }
}
