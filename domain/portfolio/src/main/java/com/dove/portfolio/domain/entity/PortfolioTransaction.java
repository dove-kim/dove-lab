package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.TxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 포트폴리오 거래 한 건 — 매수·매도·배당·입출금 등. 포지션·평단은 이 거래들을 접어 파생한다.
 *
 * <p>모든 금액(단가·수량·거래금액·수수료)은 거래 통화(원통화) 기준. 원화 환산은 표시 시점 환율로 파생한다.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_TRANSACTION",
        indexes = {
                @Index(name = "IDX_PORTFOLIO_TX_OWNER_DATE", columnList = "OWNER_MEMBER_ID,TRADE_DATE"),
                @Index(name = "IDX_PORTFOLIO_TX_ACCOUNT", columnList = "ACCOUNT_ID,TRADE_DATE"),
                @Index(name = "IDX_PORTFOLIO_TX_SYMBOL", columnList = "OWNER_MEMBER_ID,SYMBOL")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioTransaction {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 20)
    @Comment("거래 유형(BUY/SELL/DEPOSIT/WITHDRAW/DIVIDEND/INTEREST)")
    private TxType type;

    @Column(name = "TRADE_DATE", nullable = false)
    @Comment("체결/발생 일자")
    private LocalDate tradeDate;

    @Column(name = "SYMBOL", length = 100)
    @Comment("종목명(입출금이면 NULL)")
    private String symbol;

    @Column(name = "CURRENCY", nullable = false, length = 10)
    @Comment("원통화 코드(KRW/USD 등)")
    private String currency;

    @Column(name = "QUANTITY", precision = 24, scale = 8)
    @Comment("수량(원통화 종목 단위, 소수 허용)")
    private BigDecimal quantity;

    @Column(name = "PRICE", precision = 24, scale = 8)
    @Comment("단가(원통화)")
    private BigDecimal price;

    @Column(name = "AMOUNT", nullable = false, precision = 24, scale = 8)
    @Comment("거래 금액(거래 통화 기준, 크기)")
    private BigDecimal amount;

    @Column(name = "FEE", nullable = false)
    @Comment("수수료(거래 통화)")
    private Long fee;

    @Column(name = "TAG", length = 50)
    @Comment("자유 태그")
    private String tag;

    @Column(name = "MEMO", length = 500)
    @Comment("메모")
    private String memo;

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
     * 거래를 생성한다.
     */
    public static PortfolioTransaction create(Long ownerMemberId, Long accountId, TxType type, LocalDate tradeDate,
                                            String symbol, String currency, BigDecimal quantity, BigDecimal price,
                                            BigDecimal amount, Long fee,
                                            String tag, String memo, String createdBy) {
        PortfolioTransaction t = new PortfolioTransaction();
        t.ownerMemberId = ownerMemberId;
        t.accountId = accountId;
        t.type = type;
        t.tradeDate = tradeDate;
        t.symbol = symbol;
        t.currency = currency;
        t.quantity = quantity;
        t.price = price;
        t.amount = amount;
        t.fee = fee != null ? fee : 0L;
        t.tag = tag;
        t.memo = memo;
        t.createdBy = createdBy;
        t.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        t.createdAt = now;
        t.updatedAt = now;
        return t;
    }

    /**
     * 거래 내용을 갱신한다.
     */
    public void update(TxType type, LocalDate tradeDate, String symbol, String currency, BigDecimal quantity,
                       BigDecimal price, BigDecimal amount, Long fee,
                       String tag, String memo, String updatedBy) {
        this.type = type;
        this.tradeDate = tradeDate;
        this.symbol = symbol;
        this.currency = currency;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.fee = fee != null ? fee : 0L;
        this.tag = tag;
        this.memo = memo;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
