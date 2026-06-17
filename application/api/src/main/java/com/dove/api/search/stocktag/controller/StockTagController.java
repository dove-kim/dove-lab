package com.dove.api.search.stocktag.controller;

import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.userfeature.domain.capability.Capability;
import com.dove.api.search.stocktag.dto.StockTagFieldGroup;
import com.dove.api.search.stocktag.dto.StockTagNumericField;
import com.dove.api.search.stocktag.dto.StockTagValueItem;
import com.dove.api.search.stocktag.dto.StockTagsResponse;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.entity.StockTagValue;
import com.dove.stock.domain.enums.NumericField;
import com.dove.stock.domain.enums.TagField;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 분류 메타·값 목록 조회 API.
 */
@RestController
@RequestMapping("/stock-tags")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_VIEW)
public class StockTagController {

    private final StockTagValueService queryService;

    /**
     * 전체 분류 차원 메타와 등록된 값 목록을 반환한다.
     */
    @GetMapping
    public StockTagsResponse getTags() {
        // field별 등록 값 묶기
        Map<String, List<StockTagValueItem>> valuesByField = queryService.findAll().stream()
                .collect(Collectors.groupingBy(
                        StockTagValue::getField,
                        Collectors.mapping(StockTagValueItem::from, Collectors.toList())));

        List<StockTagFieldGroup> tagFields = Arrays.stream(TagField.values())
                .map(f -> new StockTagFieldGroup(
                        f.name(), f.label(), f.source().name(), f.type().name(),
                        valuesByField.getOrDefault(f.name(), List.of())))
                .toList();

        List<StockTagNumericField> numericFields = Arrays.stream(NumericField.values())
                .map(f -> new StockTagNumericField(f.name(), f.label(), f.source().name()))
                .toList();

        return new StockTagsResponse(tagFields, numericFields);
    }
}
