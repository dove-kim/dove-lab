package com.dove.api.ops.custommetric.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.ops.custommetric.dto.CustomMetricRequest;
import com.dove.api.ops.custommetric.dto.CustomMetricResponse;
import com.dove.api.ops.custommetric.dto.MetricPoint;
import com.dove.api.ops.custommetric.dto.MetricPreviewRequest;
import com.dove.api.ops.custommetric.service.CustomMetricPreviewService;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * ROOT 전용 — 커스텀 지표 정의 등록·생명주기·미리보기 API.
 */
@RestController
@RequestMapping("/admin/ops/custom-metrics")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class CustomMetricAdminController {

    private final CustomMetricDefService defService;
    private final CustomMetricDailyService dailyService;
    private final CustomMetricPreviewService previewService;
    private final MemberCustomIndicatorGrantCommandService grantCommandService;

    /**
     * 커스텀 지표를 등록한다. 스펙이 잘못되면 422.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomMetricResponse create(@Valid @RequestBody CustomMetricRequest req,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return CustomMetricResponse.from(defService.create(
                    req.name(), req.description(), req.shape(), req.spec(), req.priceType(), user.username()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    /**
     * 전체 지표 정의를 반환한다.
     */
    @GetMapping
    public List<CustomMetricResponse> list() {
        return defService.findAll().stream().map(CustomMetricResponse::from).toList();
    }

    /**
     * 단일 지표 정의를 반환한다.
     */
    @GetMapping("/{id}")
    public CustomMetricResponse get(@PathVariable Long id) {
        return CustomMetricResponse.from(defService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CUSTOM_METRIC_NOT_FOUND")));
    }

    /**
     * 지표 정의를 갱신한다. 스펙 변경 시 진행 상태가 초기화된다.
     */
    @PutMapping("/{id}")
    public CustomMetricResponse update(@PathVariable Long id, @Valid @RequestBody CustomMetricRequest req) {
        try {
            defService.update(id, req.name(), req.description(), req.spec(), req.priceType());
            return get(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    /**
     * 지표를 활성화한다.
     */
    @PostMapping("/{id}/activate")
    public CustomMetricResponse activate(@PathVariable Long id) {
        defService.updateActive(id, true);
        return get(id);
    }

    /**
     * 지표를 비활성화한다(야간 계산 스킵).
     */
    @PostMapping("/{id}/deactivate")
    public CustomMetricResponse deactivate(@PathVariable Long id) {
        defService.updateActive(id, false);
        return get(id);
    }

    /**
     * 저장값을 지우고 진행 상태를 초기화해 다음 배치가 처음부터 재계산하게 한다.
     */
    @PostMapping("/{id}/recompute")
    public CustomMetricResponse recompute(@PathVariable Long id) {
        dailyService.deleteByMetric(id);
        defService.resetProgress(id);
        return get(id);
    }

    /**
     * 지표 정의와 저장값을 함께 삭제한다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        grantCommandService.revokeAllForMetric(id);
        dailyService.deleteByMetric(id);
        defService.delete(id);
    }

    /**
     * 초안 스펙을 최근 구간에 대해 시험 계산해 미리보기 시계열을 반환한다. 스펙이 잘못되면 422.
     */
    @PostMapping("/preview")
    public List<MetricPoint> preview(@Valid @RequestBody MetricPreviewRequest req) {
        try {
            return previewService.preview(req.spec(), req.priceType());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }
}
