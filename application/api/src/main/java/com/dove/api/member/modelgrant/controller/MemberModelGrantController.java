package com.dove.api.member.modelgrant.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.member.modelgrant.dto.UpdateUserModelRequest;
import com.dove.api.search.stock.dto.ModelSummaryResponse;
import com.dove.modelserving.application.service.ModelQueryService;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.userfeature.application.service.MemberModelGrantCommandService;
import com.dove.userfeature.application.service.MemberModelGrantQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 사용자별 모델 점수 접근 부여 관리 API (ADMIN 이상). 모델 아티팩트·계산식은 노출하지 않는다.
 */
@RestController
@RequestMapping("/admin/model-grants")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class MemberModelGrantController {

    private final ModelQueryService modelQueryService;
    private final MemberModelGrantCommandService commandService;
    private final MemberModelGrantQueryService queryService;

    /**
     * 부여 대상이 되는 활성 모델 요약 목록을 반환한다.
     */
    @GetMapping("/models")
    public List<ModelSummaryResponse> grantableModels() {
        return modelQueryService.findByStatus(ModelStatus.ACTIVE).stream()
                .map(ModelSummaryResponse::from)
                .toList();
    }

    /**
     * 회원에게 부여된 모델 ID 집합을 조회한다.
     */
    @GetMapping("/users/{userId}")
    public Set<Long> getGrants(@PathVariable("userId") Long memberId) {
        return queryService.findGrantedModelIds(memberId);
    }

    /**
     * 회원에게 모델 접근을 부여하거나 회수한다.
     */
    @PatchMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGrant(
            @PathVariable("userId") Long memberId,
            @RequestBody @Valid UpdateUserModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> commandService.grant(memberId, request.modelId(), admin.memberId());
            case REVOKE -> commandService.revoke(memberId, request.modelId());
        }
    }
}
