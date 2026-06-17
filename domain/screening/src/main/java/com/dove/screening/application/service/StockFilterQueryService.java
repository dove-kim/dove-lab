package com.dove.screening.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.entity.StockFilter;
import com.dove.screening.domain.repository.StockFilterRepository;
import com.dove.screening.domain.value.NamePatternCondition;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.TagCondition;
import com.dove.screening.infrastructure.repository.StockTagFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 종목 필터 조회 서비스 (시스템·개인 통합).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockFilterQueryService {

    private final StockFilterRepository repository;
    private final StockTagFilterRepository stockTagFilterRepository;

    /**
     * ID로 종목 필터를 조회한다. 시스템·개인 무관.
     */
    public Optional<StockFilter> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 종목 필터를 쿼리로 평가해 통과 ticker 집합을 반환한다. 필터가 없으면 빈 집합.
     *
     * @param markets 시장 제한 (null/빈 배열이면 전체)
     */
    public Set<String> resolveTickers(Long stockFilterId, List<MarketType> markets) {
        return repository.findById(stockFilterId)
                .map(sf -> stockTagFilterRepository.findTickers(
                        sf.getTagConditions(), sf.getNumericConditions(), sf.getStockConditions(),
                        sf.getNamePatternConditions(), markets))
                .orElse(Set.of());
    }

    /**
     * 시스템 필터 전체를 조회한다 (관리 화면용, enabled 무관).
     */
    public List<StockFilter> findSystemFilters() {
        return repository.findByMemberIdIsNullOrderByDisplayOrderAsc();
    }

    /**
     * 활성 상태인 시스템 필터 목록을 조회한다.
     */
    public List<StockFilter> findSystemFiltersEnabled() {
        return repository.findByMemberIdIsNullAndEnabledTrueOrderByDisplayOrderAsc();
    }

    /**
     * 회원이 소유한 개인 필터 목록을 조회한다.
     */
    public List<StockFilter> findPersonalFilters(Long memberId) {
        return repository.findByMemberIdOrderByDisplayOrderAsc(memberId);
    }

    /**
     * 활성 시스템 필터와 회원의 개인 필터를 통합해 반환한다.
     */
    public List<StockFilter> findAvailableForMember(Long memberId) {
        List<StockFilter> result = new ArrayList<>(findSystemFiltersEnabled());
        result.addAll(findPersonalFilters(memberId));
        return result;
    }

    /**
     * 태그·수치 조건에 통과하는 ticker 집합을 반환한다.
     *
     * @param tagConds 태그 조건 목록 (null/빈 배열이면 태그 제한 없음)
     * @param numConds 수치 범위 조건 목록 (null/빈 배열이면 수치 제한 없음)
     * @param markets  대상 시장 (null/빈 배열이면 KRX 전체)
     */
    public Set<String> previewByTagConditions(List<TagCondition> tagConds,
                                              List<NumericCondition> numConds,
                                              List<NamePatternCondition> nameConds,
                                              List<MarketType> markets) {
        List<MarketType> targets = (markets != null && !markets.isEmpty())
                ? markets
                : MarketType.KRX_MARKETS;
        return stockTagFilterRepository.findTickers(
                tagConds, numConds != null ? numConds : List.of(), List.of(),
                nameConds != null ? nameConds : List.of(), targets);
    }
}
