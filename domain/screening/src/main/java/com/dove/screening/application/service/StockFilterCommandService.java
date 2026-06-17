package com.dove.screening.application.service;

import com.dove.screening.application.exception.DuplicateStockFilterNameException;
import com.dove.screening.domain.entity.StockFilter;
import com.dove.screening.domain.repository.StockFilterRepository;
import com.dove.screening.domain.value.NamePatternCondition;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 종목 필터 커맨드 서비스 (시스템·개인 통합).
 * 시스템 필터 변경은 API 계층에서 ADMIN/ROOT 권한 검증.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockFilterCommandService {

    private final StockFilterRepository repository;

    // ── 시스템 필터 ─────────────────────────────────────────────────────────

    /**
     * 시스템 종목 필터를 생성한다 (이름 패턴 조건 없이).
     */
    public StockFilter createSystem(String name, String description,
                                    List<TagCondition> tagConditions,
                                    List<StockCondition> stockConditions,
                                    List<NumericCondition> numericConditions,
                                    String createdBy) {
        return createSystem(name, description, tagConditions, stockConditions, numericConditions,
                List.of(), createdBy);
    }

    /**
     * 시스템 종목 필터를 생성한다.
     *
     * @throws DuplicateStockFilterNameException 같은 이름의 시스템 필터가 이미 있을 때
     */
    public StockFilter createSystem(String name, String description,
                                    List<TagCondition> tagConditions,
                                    List<StockCondition> stockConditions,
                                    List<NumericCondition> numericConditions,
                                    List<NamePatternCondition> namePatternConditions,
                                    String createdBy) {
        if (repository.countByMemberIdIsNullAndName(name) > 0) {
            throw new DuplicateStockFilterNameException("DUPLICATE_STOCK_FILTER_NAME");
        }
        return repository.save(
                StockFilter.createSystem(name, description, tagConditions, stockConditions, numericConditions,
                        namePatternConditions, createdBy));
    }

    /**
     * 시스템 종목 필터를 수정한다 (이름 패턴 조건 없이).
     */
    public StockFilter updateSystem(Long id, String name, String description,
                                    List<TagCondition> tagConditions,
                                    List<StockCondition> stockConditions,
                                    List<NumericCondition> numericConditions,
                                    String updatedBy) {
        return updateSystem(id, name, description, tagConditions, stockConditions, numericConditions,
                List.of(), updatedBy);
    }

    /**
     * 시스템 종목 필터를 수정한다.
     *
     * @throws NoSuchElementException 시스템 필터가 없을 때
     * @throws DuplicateStockFilterNameException 변경하려는 이름이 다른 시스템 필터와 중복될 때
     */
    public StockFilter updateSystem(Long id, String name, String description,
                                    List<TagCondition> tagConditions,
                                    List<StockCondition> stockConditions,
                                    List<NumericCondition> numericConditions,
                                    List<NamePatternCondition> namePatternConditions,
                                    String updatedBy) {
        StockFilter filter = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("STOCK_FILTER_NOT_FOUND"));
        if (!filter.isSystem()) {
            throw new NoSuchElementException("STOCK_FILTER_NOT_FOUND");
        }
        if (!filter.getName().equals(name)
                && repository.countByMemberIdIsNullAndNameAndIdNot(name, id) > 0) {
            throw new DuplicateStockFilterNameException("DUPLICATE_STOCK_FILTER_NAME");
        }
        filter.update(name, description, tagConditions, stockConditions, numericConditions,
                namePatternConditions, updatedBy);
        return filter;
    }

    /**
     * 시스템 종목 필터의 활성 여부를 변경한다.
     *
     * @throws NoSuchElementException 시스템 필터가 없을 때
     */
    public StockFilter setEnabled(Long id, boolean enabled, String updatedBy) {
        StockFilter filter = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("STOCK_FILTER_NOT_FOUND"));
        if (!filter.isSystem()) {
            throw new NoSuchElementException("STOCK_FILTER_NOT_FOUND");
        }
        filter.updateEnabled(enabled, updatedBy);
        return filter;
    }

    /**
     * 시스템 종목 필터를 삭제한다.
     *
     * @throws NoSuchElementException 시스템 필터가 없을 때
     */
    public void deleteSystem(Long id) {
        StockFilter filter = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("STOCK_FILTER_NOT_FOUND"));
        if (!filter.isSystem()) {
            throw new NoSuchElementException("STOCK_FILTER_NOT_FOUND");
        }
        repository.delete(filter);
    }

    // ── 개인 필터 ───────────────────────────────────────────────────────────

    /**
     * 개인 종목 필터를 생성한다 (이름 패턴 조건 없이).
     */
    public StockFilter createPersonal(Long memberId, String name, String description,
                                      List<TagCondition> tagConditions,
                                      List<StockCondition> stockConditions,
                                      List<NumericCondition> numericConditions,
                                      String createdBy) {
        return createPersonal(memberId, name, description, tagConditions, stockConditions, numericConditions,
                List.of(), createdBy);
    }

    /**
     * 개인 종목 필터를 생성한다.
     *
     * @throws DuplicateStockFilterNameException 같은 이름의 개인 필터가 이미 있을 때
     */
    public StockFilter createPersonal(Long memberId, String name, String description,
                                      List<TagCondition> tagConditions,
                                      List<StockCondition> stockConditions,
                                      List<NumericCondition> numericConditions,
                                      List<NamePatternCondition> namePatternConditions,
                                      String createdBy) {
        try {
            return repository.saveAndFlush(
                    StockFilter.createPersonal(memberId, name, description,
                            tagConditions, stockConditions, numericConditions, namePatternConditions, createdBy));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateStockFilterNameException("DUPLICATE_STOCK_FILTER_NAME");
        }
    }

    /**
     * 회원이 소유한 개인 종목 필터를 수정한다 (이름 패턴 조건 없이).
     */
    public StockFilter updatePersonal(Long memberId, Long id, String name, String description,
                                      List<TagCondition> tagConditions,
                                      List<StockCondition> stockConditions,
                                      List<NumericCondition> numericConditions,
                                      String updatedBy) {
        return updatePersonal(memberId, id, name, description, tagConditions, stockConditions, numericConditions,
                List.of(), updatedBy);
    }

    /**
     * 회원이 소유한 개인 종목 필터를 수정한다.
     *
     * @throws NoSuchElementException 해당 개인 필터가 없을 때
     * @throws DuplicateStockFilterNameException 변경하려는 이름이 다른 개인 필터와 중복될 때
     */
    public StockFilter updatePersonal(Long memberId, Long id, String name, String description,
                                      List<TagCondition> tagConditions,
                                      List<StockCondition> stockConditions,
                                      List<NumericCondition> numericConditions,
                                      List<NamePatternCondition> namePatternConditions,
                                      String updatedBy) {
        StockFilter filter = repository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("STOCK_FILTER_NOT_FOUND"));
        filter.update(name, description, tagConditions, stockConditions, numericConditions,
                namePatternConditions, updatedBy);
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateStockFilterNameException("DUPLICATE_STOCK_FILTER_NAME");
        }
        return filter;
    }

    /**
     * 회원이 소유한 개인 종목 필터를 삭제한다.
     *
     * @throws NoSuchElementException 해당 개인 필터가 없을 때
     */
    public void deletePersonal(Long memberId, Long id) {
        StockFilter filter = repository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("STOCK_FILTER_NOT_FOUND"));
        repository.delete(filter);
    }
}
