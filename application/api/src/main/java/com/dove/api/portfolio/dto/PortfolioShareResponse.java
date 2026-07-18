package com.dove.api.portfolio.dto;

/**
 * 계좌 공유 응답. 방향(OUT=내가 공유, IN=공유받음)에 따라 상대방 표시가 달라진다.
 *
 * @param id          공유 grant ID
 * @param accountId   대상 계좌 ID(공유받은 계좌 열람 진입용)
 * @param accountName 대상 계좌명
 * @param grantee     상대방 표시(OUT=공유받은 사람, IN=공유한 사람)
 * @param permission  권한(READ/READ_RELATIVE/WRITE)
 * @param direction   방향(OUT/IN)
 */
public record PortfolioShareResponse(
        long id,
        long accountId,
        String accountName,
        String grantee,
        String permission,
        String direction
) {}
