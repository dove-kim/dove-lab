package com.dove.stockcollection.domain.entity;

import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.enums.CollectionStatus;
import com.dove.stockcollection.domain.enums.CollectionType;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수동 수집 작업의 진행 상황·결과 기록.
 */
@Getter
@Entity
@Table(name = "COLLECTION_TASK",
        indexes = {
                @Index(name = "IDX_CT_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_CT_CREATED_AT", columnList = "CREATED_AT"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 10)
    @Comment("수집 유형 (STOCK/PRICE/EVENT/INVESTOR)")
    private CollectionType type;

    @Column(name = "SCOPE", nullable = false, length = 100)
    @Comment("수집 범위 표시 문자열 (재실행 파라미터는 아래 컬럼에서 읽음)")
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXCHANGE", length = 10)
    @Comment("대상 거래소 (PRICE만, STOCK/EVENT는 null)")
    private StockExchange exchange;

    @Column(name = "FROM_DATE", nullable = false)
    @Comment("수집 시작일 (재실행 파라미터)")
    private LocalDate fromDate;

    @Column(name = "TO_DATE", nullable = false)
    @Comment("수집 종료일 (재실행 파라미터)")
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    @Comment("작업 상태")
    private CollectionStatus status;

    @Column(name = "TOTAL", nullable = false)
    @Comment("전체 작업 수 (종목 × 윈도우 × 가격유형)")
    private int total;

    @Column(name = "DONE", nullable = false)
    @Comment("완료된 작업 수 (5초 단위 갱신)")
    private int done;

    @Column(name = "ADJUSTED_TOTAL", nullable = false)
    @Comment("수정주가 재조회 대상 종목 수 (이벤트 감지 시 발생, 0=없음)")
    private int adjustedTotal;

    @Column(name = "ADJUSTED_DONE", nullable = false)
    @Comment("수정주가 재조회 완료 종목 수")
    private int adjustedDone;

    @Column(name = "ERROR_CODE", length = 50)
    @Comment("실패 시 에러 코드")
    private String errorCode;

    @Column(name = "ERROR_DETAIL", length = 1000)
    @Comment("실패 시 상세 메시지")
    private String errorDetail;

    @Column(name = "REQUESTED_BY")
    @Comment("요청한 ROOT 회원 ID")
    private Long requestedBy;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "PROGRESS_AT")
    @Comment("진행률 마지막 갱신 시각 (ETA 계산 기준)")
    private LocalDateTime progressAt;

    @Column(name = "FINISHED_AT")
    private LocalDateTime finishedAt;

    public CollectionTask(CollectionType type, StockExchange exchange,
                          LocalDate fromDate, LocalDate toDate, Long requestedBy) {
        this.type = type;
        this.exchange = exchange;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.scope = buildScope(type, exchange, fromDate, toDate);
        this.requestedBy = requestedBy;
        this.status = CollectionStatus.PENDING;
        this.total = 0;
        this.done = 0;
        this.adjustedTotal = 0;
        this.adjustedDone = 0;
    }

    /**
     * 표시용 범위 문자열을 만든다 (재실행은 이 문자열이 아니라 구조화 컬럼으로 한다).
     */
    private static String buildScope(CollectionType type, StockExchange exchange,
                                     LocalDate fromDate, LocalDate toDate) {
        return exchange != null
                ? "%s/%s/%s~%s".formatted(type, exchange, fromDate, toDate)
                : "%s/%s~%s".formatted(type, fromDate, toDate);
    }

    /**
     * 작업을 시작 상태로 전환하고 전체 작업 수를 설정한다.
     */
    public void start(int total) {
        this.status = CollectionStatus.RUNNING;
        this.total = total;
        this.done = 0;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 진행률(완료 작업 수)과 갱신 시각을 기록한다.
     */
    public void updateProgress(int done) {
        this.done = done;
        this.progressAt = LocalDateTime.now();
    }

    /**
     * 수정주가 재조회 대상 종목 수를 설정한다.
     */
    public void setAdjustedTotal(int total) {
        this.adjustedTotal = total;
    }

    /**
     * 수정주가 재조회 완료 수를 갱신한다.
     */
    public void updateAdjustedProgress(int done) {
        this.adjustedDone = done;
    }

    /**
     * 작업을 정상 완료 상태로 전환한다.
     */
    public void complete() {
        this.status = CollectionStatus.DONE;
        this.done = this.total;
        this.adjustedDone = this.adjustedTotal;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * 작업을 실패 상태로 전환하고 에러 코드·상세를 기록한다.
     */
    public void fail(String errorCode, String errorDetail) {
        this.status = CollectionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorDetail = truncate(errorDetail);
        this.finishedAt = LocalDateTime.now();
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
