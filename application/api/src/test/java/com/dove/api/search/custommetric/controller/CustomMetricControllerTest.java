package com.dove.api.search.custommetric.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.ops.custommetric.dto.MetricPoint;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.custommetric.domain.entity.CustomMetricDaily;
import com.dove.custommetric.domain.entity.CustomMetricDailyId;
import com.dove.custommetric.domain.entity.CustomMetricDef;
import com.dove.custommetric.domain.entity.MetricShape;
import com.dove.stock.domain.enums.PriceType;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 커스텀 지표 시계열 조회 API의 접근 게이트·매핑 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class CustomMetricControllerTest {

    @Mock CustomMetricDefService defService;
    @Mock CustomMetricDailyService dailyService;
    @Mock MemberCustomIndicatorGrantQueryService grantQueryService;

    @InjectMocks CustomMetricController controller;

    private static AuthenticatedUser user(String role) {
        return new AuthenticatedUser(7L, "tester", role, false, Set.of("CUSTOM_INDICATOR"));
    }

    private static CustomMetricDef def(Long id, boolean active) {
        CustomMetricDef d = CustomMetricDef.create("레짐", null, MetricShape.SERIES, "{}", PriceType.RAW, "root");
        if (!active) d.updateActive(false);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    private static CustomMetricDaily daily(Long metricId, LocalDate date, double value) {
        return new CustomMetricDaily(new CustomMetricDailyId(metricId, date), value, LocalDateTime.now());
    }

    @Nested
    @DisplayName("series")
    class Series {

        @Test
        @DisplayName("ROOT는 grant 확인 없이 시계열을 거래일 오름차순으로 반환한다")
        void shouldReturnSeriesForRootWithoutGrantCheck() {
            given(defService.findById(3L)).willReturn(Optional.of(def(3L, true)));
            given(dailyService.findByMetricAndDateRange(3L, null, null)).willReturn(List.of(
                    daily(3L, LocalDate.of(2026, 1, 5), 0.4),
                    daily(3L, LocalDate.of(2026, 1, 6), 0.5)));

            List<MetricPoint> result = controller.series(3L, null, null, user("ROOT"));

            assertThat(result).containsExactly(
                    new MetricPoint("2026-01-05", 0.4),
                    new MetricPoint("2026-01-06", 0.5));
        }

        @Test
        @DisplayName("부여받은 사용자는 범위를 전달해 시계열을 반환한다")
        void shouldReturnSeriesWhenGranted() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 31);
            given(grantQueryService.hasGrant(7L, 3L)).willReturn(true);
            given(defService.findById(3L)).willReturn(Optional.of(def(3L, true)));
            given(dailyService.findByMetricAndDateRange(3L, from, to))
                    .willReturn(List.of(daily(3L, LocalDate.of(2026, 1, 10), 0.7)));

            List<MetricPoint> result = controller.series(3L, from, to, user("USER"));

            assertThat(result).containsExactly(new MetricPoint("2026-01-10", 0.7));
        }

        @Test
        @DisplayName("부여받지 않은 사용자는 403 CUSTOM_METRIC_NOT_GRANTED로 거부한다")
        void shouldRejectWhenNotGranted() {
            given(grantQueryService.hasGrant(7L, 3L)).willReturn(false);

            assertThatThrownBy(() -> controller.series(3L, null, null, user("USER")))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException ex = (ResponseStatusException) e;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ex.getReason()).isEqualTo("CUSTOM_METRIC_NOT_GRANTED");
                    });
        }

        @Test
        @DisplayName("비활성 지표는 빈 목록을 반환한다")
        void shouldReturnEmptyWhenInactive() {
            given(defService.findById(3L)).willReturn(Optional.of(def(3L, false)));

            assertThat(controller.series(3L, null, null, user("ROOT"))).isEmpty();
        }

        @Test
        @DisplayName("지표가 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenMissing() {
            given(defService.findById(3L)).willReturn(Optional.empty());

            assertThat(controller.series(3L, null, null, user("ROOT"))).isEmpty();
        }
    }
}
