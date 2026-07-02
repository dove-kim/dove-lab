package com.dove.modelserving.domain.zone;

import com.dove.modelserving.domain.feature.FeatureResolver;
import com.dove.modelserving.domain.feature.FeatureSource;
import com.dove.modelserving.domain.meta.ModelEntryZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * meta.json의 진입존 조건 문자열을 평가 가능한 EntryZone으로 해석하는 파서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntryZoneParser {

    private static final String PREV_PREFIX = "prev_";

    private final FeatureResolver featureResolver;

    /**
     * 진입존 정의를 평가 가능한 EntryZone으로 해석한다. 조건이 없거나, 하나라도 파싱 불가·미지 피처면
     * 빈 EntryZone(어떤 행도 통과 안 함)을 반환한다(fail-closed).
     */
    public EntryZone parse(ModelEntryZone zone) {
        if (zone == null || zone.conditions() == null || zone.conditions().isEmpty()) {
            log.warn("진입존 조건 없음 — 채점 대상 0건(fail-closed)");
            return new EntryZone(List.of());
        }

        List<ZoneCondition> parsed = new ArrayList<>(zone.conditions().size());
        for (String raw : zone.conditions()) {
            ZoneCondition condition = parseCondition(raw);
            if (condition == null) {
                log.warn("진입존 조건 파싱 실패 — fail-closed: '{}'", raw);
                return new EntryZone(List.of());
            }
            parsed.add(condition);
        }
        return new EntryZone(parsed);
    }

    /**
     * "feat OP value" 또는 "prev_feat OP value" 형식을 ZoneCondition으로 파싱한다. 실패 시 null.
     */
    private ZoneCondition parseCondition(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();

        for (ZoneOperator op : orderedByTokenLength()) {
            int idx = trimmed.indexOf(op.token());
            if (idx <= 0) continue;

            String lhs = trimmed.substring(0, idx).trim();
            String rhs = trimmed.substring(idx + op.token().length()).trim();
            return buildCondition(lhs, op, rhs);
        }
        return null;
    }

    /**
     * 좌변 피처명·연산자·우변 값으로 조건을 만든다. 피처가 미해석이거나 값이 숫자가 아니면 null.
     */
    private ZoneCondition buildCondition(String lhs, ZoneOperator op, String rhs) {
        boolean prev = lhs.toLowerCase().startsWith(PREV_PREFIX);
        String featureName = prev ? lhs.substring(PREV_PREFIX.length()) : lhs;

        Optional<FeatureSource> source = featureResolver.resolve(featureName);
        if (source.isEmpty()) return null;

        try {
            double value = Double.parseDouble(rhs);
            return new ZoneCondition(source.get().column(), prev, op, value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 연산자를 토큰 길이 내림차순으로 정렬해 반환한다. ">="를 ">"보다 먼저 매칭하기 위함.
     */
    private static List<ZoneOperator> orderedByTokenLength() {
        return List.of(ZoneOperator.GE, ZoneOperator.LE, ZoneOperator.EQ, ZoneOperator.GT, ZoneOperator.LT);
    }
}
