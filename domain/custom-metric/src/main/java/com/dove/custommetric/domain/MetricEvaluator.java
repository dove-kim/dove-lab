package com.dove.custommetric.domain;

import com.dove.custommetric.domain.spec.AggNode;
import com.dove.custommetric.domain.spec.BinaryNode;
import com.dove.custommetric.domain.spec.BinaryOp;
import com.dove.custommetric.domain.spec.ConstNode;
import com.dove.custommetric.domain.spec.CumProd1pNode;
import com.dove.custommetric.domain.spec.EmaNode;
import com.dove.custommetric.domain.spec.LagNode;
import com.dove.custommetric.domain.spec.MetricNode;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.RefNode;
import com.dove.custommetric.domain.spec.RollMeanNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 커스텀 지표 계산식(MetricSpec)을 거래일 정렬 시계열로 평가하는 순수 계산기.
 * 횡단 집계(AggNode)는 {@link AggResolver}가 데이터로 채우고, 나머지 연산은 인메모리로 계산한다.
 * 표본 부족·미확정 구간은 NaN으로 두며, 저장 측이 NaN을 건너뛴다.
 */
public final class MetricEvaluator {

    private MetricEvaluator() {
    }

    /**
     * 스펙을 길이 n(거래일 수) 시계열로 평가한다. 비교 연산은 1.0/0.0, 미확정은 NaN.
     *
     * @throws IllegalArgumentException 정의되지 않은 ref를 참조할 때
     */
    public static double[] evaluate(MetricSpec spec, int n, AggResolver aggResolver) {
        Map<String, double[]> letCache = new HashMap<>();
        return eval(spec.root(), n, spec, aggResolver, letCache);
    }

    private static double[] eval(MetricNode node, int n, MetricSpec spec, AggResolver agg, Map<String, double[]> cache) {
        return switch (node) {
            case AggNode a -> require(agg.resolve(a), n);
            case ConstNode c -> {
                double[] out = new double[n];
                Arrays.fill(out, c.value());
                yield out;
            }
            case RefNode r -> {
                double[] cached = cache.get(r.name());
                if (cached != null) yield cached;
                MetricNode target = spec.lets() == null ? null : spec.lets().get(r.name());
                if (target == null) throw new IllegalArgumentException("정의되지 않은 ref: " + r.name());
                double[] v = eval(target, n, spec, agg, cache);
                cache.put(r.name(), v);
                yield v;
            }
            case RollMeanNode rm -> rollMean(eval(rm.input(), n, spec, agg, cache), rm.window(), rm.minPeriods());
            case EmaNode e -> ema(eval(e.input(), n, spec, agg, cache), e.window());
            case CumProd1pNode cp -> cumProd1p(eval(cp.input(), n, spec, agg, cache));
            case LagNode l -> lag(eval(l.input(), n, spec, agg, cache), l.periods());
            case BinaryNode b -> binary(b.op(), eval(b.left(), n, spec, agg, cache), eval(b.right(), n, spec, agg, cache));
        };
    }

    private static double[] require(double[] s, int n) {
        if (s == null || s.length != n) {
            throw new IllegalArgumentException("AggResolver가 길이 " + n + " 시계열을 반환해야 함");
        }
        return s;
    }

    /** window 위치 구간의 non-NaN이 minPeriods 이상이면 그 평균, 아니면 NaN. */
    private static double[] rollMean(double[] x, int window, int minPeriods) {
        int n = x.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            int from = Math.max(0, i - window + 1);
            double sum = 0;
            int cnt = 0;
            for (int k = from; k <= i; k++) {
                if (!Double.isNaN(x[k])) {
                    sum += x[k];
                    cnt++;
                }
            }
            out[i] = cnt >= minPeriods ? sum / cnt : Double.NaN;
        }
        return out;
    }

    /** 평활계수 2/(w+1). 첫 유효 window개로 SMA 시드 후 전진. NaN 입력은 직전값 유지. */
    private static double[] ema(double[] x, int window) {
        int n = x.length;
        double[] out = new double[n];
        Arrays.fill(out, Double.NaN);
        double alpha = 2.0 / (window + 1);
        int seedEnd = -1;
        double seedSum = 0;
        int cnt = 0;
        for (int i = 0; i < n && seedEnd < 0; i++) {
            if (!Double.isNaN(x[i])) {
                seedSum += x[i];
                cnt++;
                if (cnt == window) {
                    seedEnd = i;
                    out[i] = seedSum / window;
                }
            }
        }
        if (seedEnd < 0) return out;
        double prev = out[seedEnd];
        for (int i = seedEnd + 1; i < n; i++) {
            if (Double.isNaN(x[i])) {
                out[i] = prev;
            } else {
                prev = alpha * x[i] + (1 - alpha) * prev;
                out[i] = prev;
            }
        }
        return out;
    }

    /** ∏(1+x). NaN은 0으로 간주. */
    private static double[] cumProd1p(double[] x) {
        int n = x.length;
        double[] out = new double[n];
        double acc = 1.0;
        for (int i = 0; i < n; i++) {
            double r = Double.isNaN(x[i]) ? 0.0 : x[i];
            acc *= (1.0 + r);
            out[i] = acc;
        }
        return out;
    }

    /** periods만큼 과거값 참조. 앞쪽 periods개는 NaN. */
    private static double[] lag(double[] x, int periods) {
        int n = x.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = i >= periods ? x[i - periods] : Double.NaN;
        }
        return out;
    }

    private static double[] binary(BinaryOp op, double[] a, double[] b) {
        int n = a.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double l = a[i];
            double r = b[i];
            if (Double.isNaN(l) || Double.isNaN(r)) {
                out[i] = Double.NaN;
                continue;
            }
            out[i] = switch (op) {
                case ADD -> l + r;
                case SUB -> l - r;
                case MUL -> l * r;
                case DIV -> r == 0 ? Double.NaN : l / r;
                case GT -> l > r ? 1.0 : 0.0;
                case LT -> l < r ? 1.0 : 0.0;
                case GTE -> l >= r ? 1.0 : 0.0;
                case LTE -> l <= r ? 1.0 : 0.0;
                case AND -> (l != 0 && r != 0) ? 1.0 : 0.0;
                case OR -> (l != 0 || r != 0) ? 1.0 : 0.0;
            };
        }
        return out;
    }
}
