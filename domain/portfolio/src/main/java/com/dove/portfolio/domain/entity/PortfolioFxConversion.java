package com.dove.portfolio.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 통화 환전 한 건(예: KRW→USD). 통화별 현금을 이어주며 외부 입출금이 아니므로 순납입엔 잡히지 않는다.
 *
 * <p>모든 금액은 각자의 원통화 기준. 원화 환산은 표시 시점 환율로 파생한다.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_FX_CONVERSION",
        indexes = {
                @Index(name = "IDX_PORTFOLIO_FX_CONV_OWNER_DATE", columnList = "OWNER_MEMBER_ID,CONV_DATE"),
                @Index(name = "IDX_PORTFOLIO_FX_CONV_ACCOUNT", columnList = "ACCOUNT_ID")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioFxConversion {

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

    @Column(name = "CONV_DATE", nullable = false)
    @Comment("환전 일자")
    private LocalDate convDate;

    @Column(name = "FROM_CURRENCY", nullable = false, length = 10)
    @Comment("보낸 통화 코드")
    private String fromCurrency;

    @Column(name = "FROM_AMOUNT", nullable = false, precision = 24, scale = 8)
    @Comment("보낸 금액(보낸 통화 기준)")
    private BigDecimal fromAmount;

    @Column(name = "TO_CURRENCY", nullable = false, length = 10)
    @Comment("받은 통화 코드")
    private String toCurrency;

    @Column(name = "TO_AMOUNT", nullable = false, precision = 24, scale = 8)
    @Comment("받은 금액(받은 통화 기준)")
    private BigDecimal toAmount;

    @Column(name = "FEE", nullable = false)
    @Comment("수수료(보낸 통화 기준)")
    private Long fee;

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
     * 환전을 생성한다.
     */
    public static PortfolioFxConversion create(Long ownerMemberId, Long accountId, LocalDate convDate,
                                               String fromCurrency, BigDecimal fromAmount,
                                               String toCurrency, BigDecimal toAmount, Long fee,
                                               String memo, String createdBy) {
        PortfolioFxConversion c = new PortfolioFxConversion();
        c.ownerMemberId = ownerMemberId;
        c.accountId = accountId;
        c.convDate = convDate;
        c.fromCurrency = fromCurrency;
        c.fromAmount = fromAmount;
        c.toCurrency = toCurrency;
        c.toAmount = toAmount;
        c.fee = fee != null ? fee : 0L;
        c.memo = memo;
        c.createdBy = createdBy;
        c.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        c.createdAt = now;
        c.updatedAt = now;
        return c;
    }

    /**
     * 환전 내용을 갱신한다.
     */
    public void update(LocalDate convDate, String fromCurrency, BigDecimal fromAmount,
                       String toCurrency, BigDecimal toAmount, Long fee, String memo, String updatedBy) {
        this.convDate = convDate;
        this.fromCurrency = fromCurrency;
        this.fromAmount = fromAmount;
        this.toCurrency = toCurrency;
        this.toAmount = toAmount;
        this.fee = fee != null ? fee : 0L;
        this.memo = memo;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
