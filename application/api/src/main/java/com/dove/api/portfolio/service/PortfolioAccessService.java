package com.dove.api.portfolio.service;

import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioShareService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.enums.PortfolioSharePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * 계좌 접근제어 — 호출자가 소유자거나 공유받은 계좌인지 확인해 유효 권한을 해석한다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioAccessService {

    private final PortfolioAccountService accountService;
    private final PortfolioShareService shareService;

    /**
     * 계좌 열람 권한을 해석한다.
     *
     * @throws ResponseStatusException 계좌가 없으면 404, 접근 권한이 없으면 403
     */
    public PortfolioAccess resolveRead(long callerMemberId, long accountId) {
        PortfolioAccount account = getAccount(accountId);
        if (account.getOwnerMemberId() == callerMemberId) {
            return new PortfolioAccess(callerMemberId, true, true, false);
        }
        PortfolioSharePermission permission = shareService.permissionFor(callerMemberId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "PORTFOLIO_ACCESS_DENIED"));
        return new PortfolioAccess(account.getOwnerMemberId(), false,
                permission.allowsWrite(), permission.hidesAmounts());
    }

    /**
     * 계좌 쓰기 권한을 해석한다(소유자 또는 WRITE 공유).
     *
     * @throws ResponseStatusException 계좌가 없으면 404, 쓰기 권한이 없으면 403
     */
    public PortfolioAccess resolveWrite(long callerMemberId, long accountId) {
        PortfolioAccess access = resolveRead(callerMemberId, accountId);
        if (!access.canWrite()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PORTFOLIO_ACCESS_DENIED");
        }
        return access;
    }

    private PortfolioAccount getAccount(long accountId) {
        try {
            return accountService.getById(accountId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
    }
}
