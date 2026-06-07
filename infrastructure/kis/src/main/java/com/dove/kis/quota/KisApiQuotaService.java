package com.dove.kis.quota;

import com.dove.apiquota.QuotaStatusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KIS 주식 API 게이트 제공자.
 */
@Service
@RequiredArgsConstructor
public class KisApiQuotaService {

    private final KisGate stockGate;

    /** KIS 주식 호출 게이트. */
    public KisGate stockGate() {
        return stockGate;
    }

    /**
     * 쿼터 상태 제공자 목록을 반환한다.
     */
    public List<QuotaStatusProvider> getProviders() {
        return List.of();
    }
}
