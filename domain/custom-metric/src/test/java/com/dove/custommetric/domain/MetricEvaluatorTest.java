package com.dove.custommetric.domain;

import com.dove.custommetric.domain.spec.AggNode;
import com.dove.custommetric.domain.spec.BinaryNode;
import com.dove.custommetric.domain.spec.BinaryOp;
import com.dove.custommetric.domain.spec.ConstNode;
import com.dove.custommetric.domain.spec.CumProd1pNode;
import com.dove.custommetric.domain.spec.LagNode;
import com.dove.custommetric.domain.spec.MetricAgg;
import com.dove.custommetric.domain.spec.MetricNode;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.RefNode;
import com.dove.custommetric.domain.spec.RollMeanNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 커스텀 지표 계산식 순수 평가기를 검증한다.
 */
class MetricEvaluatorTest {

    /** AggNode를 지정 시계열로 해석하는 테스트 리졸버(모든 AggNode에 동일 시계열). */
    private AggResolver resolver(double[] series) {
        return node -> series;
    }

    private double[] eval(MetricNode root, double[] aggSeries) {
        return MetricEvaluator.evaluate(new MetricSpec(Map.of(), root), aggSeries.length, resolver(aggSeries));
    }

    private AggNode ewret() {
        return new AggNode(MetricAgg.MEAN, "RET_1D", null, 1L);
    }

    private double last(double[] a) {
        return a[a.length - 1];
    }

    @Nested
    @DisplayName("개별 연산")
    class Ops {

        @Test
        @DisplayName("cumprod1p — ∏(1+x)")
        void cumprod() {
            double[] out = eval(new CumProd1pNode(ewret()), new double[]{0.1, 0.1, -0.5});
            assertThat(out[0]).isCloseTo(1.1, within(1e-9));
            assertThat(out[1]).isCloseTo(1.21, within(1e-9));
            assertThat(out[2]).isCloseTo(0.605, within(1e-9));
        }

        @Test
        @DisplayName("roll_mean — 표본 minPeriods 미만 구간은 NaN")
        void rollMeanMinPeriods() {
            double[] out = eval(new RollMeanNode(ewret(), 3, 3), new double[]{2, 4, 6, 8});
            assertThat(out[0]).isNaN();
            assertThat(out[1]).isNaN();
            assertThat(out[2]).isCloseTo(4.0, within(1e-9)); // (2+4+6)/3
            assertThat(out[3]).isCloseTo(6.0, within(1e-9)); // (4+6+8)/3
        }

        @Test
        @DisplayName("lag — periods만큼 과거값, 앞쪽은 NaN")
        void lag() {
            double[] out = eval(new LagNode(ewret(), 1), new double[]{10, 20, 30});
            assertThat(out[0]).isNaN();
            assertThat(out[1]).isCloseTo(10.0, within(1e-9));
            assertThat(out[2]).isCloseTo(20.0, within(1e-9));
        }

        @Test
        @DisplayName("binary GT — 비교는 1.0/0.0, NaN 전파")
        void binaryGt() {
            MetricNode gt = new BinaryNode(BinaryOp.GT, ewret(), new ConstNode(5));
            double[] out = eval(gt, new double[]{3, 7, Double.NaN});
            assertThat(out[0]).isZero();
            assertThat(out[1]).isEqualTo(1.0);
            assertThat(out[2]).isNaN();
        }

        @Test
        @DisplayName("let/ref — 같은 중간값을 두 번 참조해도 동일 결과")
        void letRef() {
            // ref(idx) - ref(idx) == 0
            MetricSpec spec = new MetricSpec(
                    Map.of("idx", new CumProd1pNode(ewret())),
                    new BinaryNode(BinaryOp.SUB, new RefNode("idx"), new RefNode("idx")));
            double[] out = MetricEvaluator.evaluate(spec, 3, resolver(new double[]{0.1, 0.2, 0.3}));
            assertThat(out).containsExactly(0.0, 0.0, 0.0);
        }
    }

    @Nested
    @DisplayName("레짐 스펙 (지수 > 200일선)")
    class Regime {

        /** 레짐 = cumprod1p(EW) > roll_mean(cumprod1p(EW), 200, 150). */
        private MetricSpec regimeSpec() {
            return new MetricSpec(
                    Map.of("idx", new CumProd1pNode(ewret())),
                    new BinaryNode(BinaryOp.GT, new RefNode("idx"),
                            new RollMeanNode(new RefNode("idx"), 200, 150)));
        }

        private double[] runRegime(double[] returns) {
            return MetricEvaluator.evaluate(regimeSpec(), returns.length, resolver(returns));
        }

        private double[] filled(int n, double v) {
            double[] a = new double[n];
            java.util.Arrays.fill(a, v);
            return a;
        }

        @Test
        @DisplayName("표본 부족(150일 미만) 구간은 NaN")
        void nanWhenInsufficient() {
            double[] out = runRegime(filled(100, 0.005));
            assertThat(out[out.length - 1]).isNaN();
        }

        @Test
        @DisplayName("상승 추세 — 마지막 거래일 레짐 ON(1.0)")
        void onInUptrend() {
            assertThat(last(runRegime(filled(300, 0.005)))).isEqualTo(1.0);
        }

        @Test
        @DisplayName("하락 추세 — 마지막 거래일 레짐 OFF(0.0)")
        void offInDowntrend() {
            assertThat(last(runRegime(filled(300, -0.005)))).isZero();
        }

        @Test
        @DisplayName("과거 기저 구간을 잘라도 최근 레짐 판정 불변 — 누적지수 기저 무관")
        void invariantToBase() {
            double[] full = new double[400];
            for (int i = 0; i < full.length; i++) full[i] = (i % 2 == 0) ? 0.01 : -0.005;
            double[] windowed = java.util.Arrays.copyOfRange(full, 100, 400); // 300개

            double lastFull = last(runRegime(full));
            double lastWindow = last(runRegime(windowed));
            assertThat(lastWindow).isEqualTo(lastFull);
        }
    }
}
