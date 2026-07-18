package com.dove.api.portfolio.service;

/**
 * 한 계좌에 대한 호출자의 유효 접근 권한 — 소유 또는 공유 grant로 해석된 결과.
 *
 * @param ownerMemberId 계좌 소유 회원 ID(데이터 조회 대상)
 * @param owner         호출자가 소유자인지 여부
 * @param canWrite      쓰기 허용 여부(소유자 또는 WRITE 공유)
 * @param hideAmounts   금액 숨김 여부(READ_RELATIVE 공유)
 */
public record PortfolioAccess(
        long ownerMemberId,
        boolean owner,
        boolean canWrite,
        boolean hideAmounts
) {}
