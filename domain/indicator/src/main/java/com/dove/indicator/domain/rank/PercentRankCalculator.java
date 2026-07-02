package com.dove.indicator.domain.rank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * universe 내 값들을 횡단면 PERCENT_RANK(0~1)로 환산하는 계산기.
 */
public class PercentRankCalculator {

    /**
     * 키별 값을 오름차순 percentile로 환산한다. percentile = (rank - 1) / (n - 1)이며,
     * 동순위는 그 그룹의 최소 순위를 공유하고, NULL 값은 universe에서 제외해 결과에 담지 않는다.
     * 값이 하나뿐이거나 모두 같으면 0을 부여한다(분모/분자 0 회피).
     */
    public Map<String, Double> percentRank(Map<String, Double> values) {
        List<Map.Entry<String, Double>> present = new ArrayList<>();
        for (Map.Entry<String, Double> e : values.entrySet()) {
            if (e.getValue() != null) present.add(e);
        }
        int n = present.size();
        Map<String, Double> result = new LinkedHashMap<>();
        if (n == 0) return result;
        if (n == 1) {
            result.put(present.get(0).getKey(), 0.0);
            return result;
        }

        present.sort(Map.Entry.comparingByValue());
        double denom = n - 1;
        int i = 0;
        while (i < n) {
            // 같은 값 구간을 묶어 최소 순위(=i+1)를 공유시킨다.
            int j = i;
            while (j + 1 < n && present.get(j + 1).getValue().equals(present.get(i).getValue())) j++;
            double percentile = i / denom; // rank=i+1 → (rank-1)/(n-1) = i/(n-1)
            for (int k = i; k <= j; k++) {
                result.put(present.get(k).getKey(), percentile);
            }
            i = j + 1;
        }
        return result;
    }
}
