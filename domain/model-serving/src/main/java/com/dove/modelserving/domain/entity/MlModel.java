package com.dove.modelserving.domain.entity;

import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.stock.domain.converter.PriceTypeCodeConverter;
import com.dove.stock.domain.converter.StockExchangeSetCodeConverter;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 채점에 사용하는 등록된 ML 모델(아티팩트·메타·채점 진행 커서를 한 행에 담는다).
 */
@Getter
@Entity
@Table(name = "ML_MODEL",
        indexes = {
                @Index(name = "IDX_MM_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_MM_NAME_VERSION", columnList = "NAME, VERSION")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MlModel {

    /** 채점 커서가 없을 때(null) 채점을 시작하는 최초 기준일. */
    public static final LocalDate EARLIEST_DATE = LocalDate.of(1985, 1, 1);

    /** 채점 대상 거래소를 지정하지 않았을 때의 기본값(=기존 KRX 의미: 코스피∪코스닥). */
    public static final Set<StockExchange> DEFAULT_SCORE_EXCHANGES =
            Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("모델 이름(예: swing_entry)")
    private String name;

    @Column(name = "VERSION", nullable = false, length = 50)
    @Comment("버전(예: 1.0.0)")
    private String version;

    @Column(name = "ARTIFACT", nullable = false, columnDefinition = "LONGBLOB")
    @Comment("모델 파일(joblib pickle: model·calibrator·meta)")
    private byte[] artifact;

    @Column(name = "META_JSON", nullable = false, columnDefinition = "LONGTEXT")
    @Comment("meta.json 원본(features 목록·entry_zone·feature_hash 포함)")
    private String metaJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "OUTPUT_TYPE", nullable = false, length = 20)
    @Comment("출력 의미(PROBABILITY/REGRESSION)")
    private ModelOutputType outputType;

    @Convert(converter = StockExchangeSetCodeConverter.class)
    @Column(name = "SCORE_EXCHANGES", nullable = false, length = 40)
    @Comment("채점 대상 거래소 코드 CSV(예: 0,1)")
    private Set<StockExchange> scoreExchanges;

    @Convert(converter = PriceTypeCodeConverter.class)
    @Column(name = "SCORE_PRICE_TYPE", nullable = false)
    @Comment("채점 대상 주가유형(기본 ADJUSTED)")
    private PriceType scorePriceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Comment("채점 활성 상태(ACTIVE/INACTIVE)")
    private ModelStatus status;

    @Column(name = "SCORE_CURSOR")
    @Comment("마지막으로 채점 완료된 거래일. NULL이면 미시작")
    private LocalDate scoreCursor;

    @Column(name = "LAST_SCORED_AT")
    @Comment("마지막 채점 성공 일시")
    private LocalDateTime lastScoredAt;

    @Column(name = "LAST_ERROR", length = 500)
    @Comment("마지막 채점 실패 사유(코드:메시지). 성공 시 NULL")
    private String lastError;

    @Column(name = "CREATED_BY", nullable = false, length = 100)
    @Comment("등록자")
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("최종 갱신 일시")
    private LocalDateTime updatedAt;

    public MlModel(String name, String version, byte[] artifact, String metaJson,
                   ModelOutputType outputType, Set<StockExchange> scoreExchanges, PriceType scorePriceType,
                   String createdBy) {
        this.name = name;
        this.version = version;
        this.artifact = artifact;
        this.metaJson = metaJson;
        this.outputType = outputType;
        this.scoreExchanges = (scoreExchanges == null || scoreExchanges.isEmpty())
                ? new LinkedHashSet<>(DEFAULT_SCORE_EXCHANGES)
                : new LinkedHashSet<>(scoreExchanges);
        this.scorePriceType = scorePriceType == null ? PriceType.ADJUSTED : scorePriceType;
        this.status = ModelStatus.ACTIVE;
        this.scoreCursor = null;
        this.createdBy = createdBy;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 검증을 통과한 모델을 INACTIVE 상태로 새로 등록한다. 활성화는 별도 단계에서 한다.
     */
    public static MlModel register(String name, String version, byte[] artifact, String metaJson,
                                   ModelOutputType outputType, Set<StockExchange> scoreExchanges,
                                   PriceType scorePriceType, String createdBy) {
        MlModel model = new MlModel(name, version, artifact, metaJson, outputType,
                scoreExchanges, scorePriceType, createdBy);
        model.status = ModelStatus.INACTIVE;
        return model;
    }

    /**
     * 채점 커서를 지정 거래일로 전진시키고 갱신 일시를 기록한다.
     */
    public void advanceScoreCursor(LocalDate toDate) {
        this.scoreCursor = toDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채점을 활성화하고 갱신 일시를 기록한다.
     */
    public void activate() {
        this.status = ModelStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채점을 비활성화하고 갱신 일시를 기록한다.
     */
    public void deactivate() {
        this.status = ModelStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 채점 성공을 기록한다(성공 일시 갱신·실패 사유 초기화).
     */
    public void recordScoreSuccess() {
        LocalDateTime now = LocalDateTime.now();
        this.lastScoredAt = now;
        this.lastError = null;
        this.updatedAt = now;
    }

    /**
     * 마지막 채점 실패 사유를 기록한다(500자 초과 시 잘라서 저장, 성공 일시는 유지).
     */
    public void recordScoreFailure(String error) {
        this.lastError = error == null || error.length() <= 500 ? error : error.substring(0, 500);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채점 커서를 지정 거래일로 되돌리고 갱신 일시를 기록한다. null이면 미시작 상태로 되돌린다.
     */
    public void resetScoreCursor(LocalDate toDate) {
        this.scoreCursor = toDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채점 커서가 ACTIVE인지 반환한다.
     */
    public boolean isActive() {
        return status == ModelStatus.ACTIVE;
    }

    /**
     * 채점 커서값으로부터 다음 채점 시작일을 반환한다. null이면 최초 기준일, 있으면 그 다음 날.
     */
    public static LocalDate firstScoreDate(LocalDate scoreCursor) {
        return scoreCursor == null ? EARLIEST_DATE : scoreCursor.plusDays(1);
    }
}
