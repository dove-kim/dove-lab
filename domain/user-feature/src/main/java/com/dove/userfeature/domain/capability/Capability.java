package com.dove.userfeature.domain.capability;

/**
 * 권한 단위(capability) 레지스트리 — API가 강제하는 권한 키.
 *
 * <p>capability는 코드가 정의한다(어떤 엔드포인트가 강제하기에 존재). 런타임 생성이 없으므로 enum이다.
 * 메뉴 위치·라벨·잠금/숨김 같은 표시 정책은 프론트엔드 책임이며 여기 두지 않는다.
 * 역할(ROLE) 기반 관리·ROOT 고유 기능은 capability가 아니다.
 */
public enum Capability {

    /** 종목 조회(전체 종목 브라우즈). */
    STOCK_VIEW,
    /** 조건 검색 + 검색필터·종목필터 관리(필터가 있어야 검색하므로 한 권한). */
    STOCK_SEARCH,
    /** ML 예측 지표 데이터(민감 — 권한 없으면 응답에서 제거). */
    INDICATOR_ML,
    /** ML 모델 채점 점수 조회·차트(민감 — 권한 없으면 403). */
    MODEL_SCORE,
    /** 커스텀 지표 사용(전역 게이트 — 지표별 접근은 MEMBER_CUSTOM_INDICATOR_GRANT). */
    CUSTOM_INDICATOR
}
