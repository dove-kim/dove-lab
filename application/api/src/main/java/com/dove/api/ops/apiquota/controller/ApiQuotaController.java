package com.dove.api.ops.apiquota.controller;

import com.dove.api.ops.apiquota.dto.ApiQuotaResponse;
import com.dove.apiquota.QuotaStatusProvider;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 외부 API 사용량(쿼터) 조회 API.
 */
@RestController
@RequestMapping("/admin/ops/api-quota")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class ApiQuotaController {

    private final List<QuotaStatusProvider> providers;

    /**
     * 등록된 모든 쿼터 제공자의 현재 사용량을 반환한다.
     */
    @GetMapping
    public ApiQuotaResponse get() {
        return new ApiQuotaResponse(
                providers.stream()
                        .map(p -> ApiQuotaResponse.QuotaEntry.from(p.getStatus()))
                        .toList()
        );
    }
}
