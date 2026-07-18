package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.PortfolioMarket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 포트폴리오 종목의 시장 식별 정보 — 종목명(표시)을 (시장, 티커)에 연결해 현재가 자동 조회를 가능하게 한다.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_HOLDING",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PORTFOLIO_HOLDING_OWNER_ACCOUNT_SYMBOL",
                columnNames = {"OWNER_MEMBER_ID", "ACCOUNT_ID", "SYMBOL"}),
        indexes = @Index(name = "IDX_PORTFOLIO_HOLDING_OWNER", columnList = "OWNER_MEMBER_ID")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Column(name = "OWNER_MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long ownerMemberId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    @Comment("계좌 ID(PORTFOLIO_ACCOUNT.ID)")
    private Long accountId;

    @Column(name = "SYMBOL", nullable = false, length = 100)
    @Comment("종목명(거래의 SYMBOL과 일치)")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", nullable = false, length = 20)
    @Comment("상장 시장(KOSPI/NASDAQ 등)")
    private PortfolioMarket market;

    @Column(name = "TICKER", nullable = false, length = 20)
    @Comment("시장 내 종목 코드(현재가 조회 키)")
    private String ticker;

    @Column(name = "ANNUAL_DIVIDEND_PCT")
    @Comment("연 배당수익률(%) — 예상 배당 계산용, 사용자 입력")
    private Double annualDividendPct;

    @Column(name = "DIVIDEND_TRACKED", nullable = false)
    @Comment("배당 추적 대상 여부 — 배당 화면 표시 대상")
    private boolean dividendTracked;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("생성자 username")
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    @Comment("마지막 수정자 username")
    private String updatedBy;

    /**
     * 종목 식별 정보를 생성한다.
     */
    public static PortfolioHolding create(Long ownerMemberId, Long accountId, String symbol,
                                        PortfolioMarket market, String ticker, String createdBy) {
        PortfolioHolding h = new PortfolioHolding();
        h.ownerMemberId = ownerMemberId;
        h.accountId = accountId;
        h.symbol = symbol;
        h.market = market;
        h.ticker = ticker;
        h.createdBy = createdBy;
        h.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        h.createdAt = now;
        h.updatedAt = now;
        return h;
    }

    /**
     * 시장·티커 식별 정보를 갱신한다.
     */
    public void updateIdentity(PortfolioMarket market, String ticker, String updatedBy) {
        this.market = market;
        this.ticker = ticker;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 연 배당수익률(%)을 설정한다.
     */
    public void updateDividend(Double annualDividendPct, String updatedBy) {
        this.annualDividendPct = annualDividendPct;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 배당 추적 대상 여부를 설정한다.
     */
    public void updateTracking(boolean dividendTracked, String updatedBy) {
        this.dividendTracked = dividendTracked;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 원통화 코드 — 시장에서 파생된다.
     */
    public String currency() {
        return market.getCurrency();
    }
}
