package com.dove.stock.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * KIS API에서 수집한 종목 상세 정보. Stock과 1:1 관계.
 * 항상 최신 단일 레코드만 유지 (upsert).
 */
@Getter
@Entity
@Table(name = "STOCK_DETAIL",
        indexes = {
                @Index(name = "IDX_SD_PRDT_CLSF", columnList = "PRDT_CLSF_CD"),
                @Index(name = "IDX_SD_BZTP_LCLS", columnList = "IDX_BZTP_LCLS_NM"),
                @Index(name = "IDX_SD_BZTP_MCLS", columnList = "IDX_BZTP_MCLS_NM"),
                @Index(name = "IDX_SD_BZTP_SCLS", columnList = "IDX_BZTP_SCLS_NM"),
                @Index(name = "IDX_SD_STD_IDST", columnList = "STD_IDST_CLSF_NM"),
                @Index(name = "IDX_SD_PRDT_CLSF_NM", columnList = "PRDT_CLSF_NM"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDetail {

    @Id
    @Column(name = "TICKER", length = 20, nullable = false)
    private String ticker;

    // ── 주식기본조회 (CTPF1002R) ────────────────────────────────────────────

    @Column(name = "LISTED_SHARES")
    @Comment("상장주식수 (lstg_stqt)")
    private Long listedShares;

    @Column(name = "CAPITAL_AMOUNT")
    @Comment("상장자본금액 (lstg_cptl_amt)")
    private Long capitalAmount;

    @Column(name = "FACE_VALUE")
    @Comment("액면가 (papr)")
    private Long faceValue;

    @Column(name = "STOCK_KIND_CD", length = 3)
    @Comment("주식종류코드 (stck_kind_cd): 101=보통주, 201=우선주 등")
    private String stockKindCd;

    @Column(name = "ETF_DVSN_CD", length = 2)
    @Comment("ETF구분코드 (etf_dvsn_cd)")
    private String etfDvsnCd;

    @Column(name = "REITS_KIND_CD", length = 1)
    @Comment("리츠종류코드 (reits_kind_cd)")
    private String reitsKindCd;

    @Column(name = "KOSPI200_ITEM_YN", length = 1)
    @Comment("코스피200편입여부 (kospi200_item_yn)")
    private String kospi200ItemYn;

    @Column(name = "IDX_BZTP_LCLS_CD", length = 3)
    @Comment("지수업종대분류코드")
    private String idxBztpLclsCd;

    @Column(name = "IDX_BZTP_MCLS_CD", length = 3)
    @Comment("지수업종중분류코드")
    private String idxBztpMclsCd;

    @Column(name = "IDX_BZTP_SCLS_CD", length = 3)
    @Comment("지수업종소분류코드")
    private String idxBztpSclsCd;

    @Column(name = "IDX_BZTP_LCLS_NM", length = 60)
    @Comment("지수업종대분류명")
    private String idxBztpLclsNm;

    @Column(name = "IDX_BZTP_MCLS_NM", length = 60)
    @Comment("지수업종중분류명")
    private String idxBztpMclsNm;

    @Column(name = "IDX_BZTP_SCLS_NM", length = 60)
    @Comment("지수업종소분류명")
    private String idxBztpSclsNm;

    @Column(name = "STD_IDST_CLSF_CD", length = 6)
    @Comment("표준산업분류코드 (std_idst_clsf_cd)")
    private String stdIdstClsfCd;

    @Column(name = "STD_IDST_CLSF_NM", length = 130)
    @Comment("표준산업분류명 (std_idst_clsf_cd_name)")
    private String stdIdstClsfNm;

    @Column(name = "FRNR_PSNL_LMT_RT", length = 24)
    @Comment("외국인개인한도율 (frnr_psnl_lmt_rt)")
    private String frnrPsnlLmtRt;

    @Column(name = "TR_STOP_YN", length = 1)
    @Comment("거래정지여부 (tr_stop_yn)")
    private String trStopYn;

    @Column(name = "ADMN_ITEM_YN", length = 1)
    @Comment("관리종목여부 (admn_item_yn)")
    private String admnItemYn;

    @Column(name = "LSTG_ABOL_DT", length = 8)
    @Comment("상장폐지일 yyyyMMdd (lstg_abol_dt)")
    private String lstgAbolDt;

    @Column(name = "SCTS_MKET_LSTG_DT", length = 8)
    @Comment("유가증권시장상장일 yyyyMMdd (scts_mket_lstg_dt)")
    private String sctsMketLstgDt;

    // ── 상품기본조회 (CTPF1604R) ────────────────────────────────────────────

    @Column(name = "PRDT_NAME", length = 60)
    @Comment("상품명 (prdt_name)")
    private String prdtName;

    @Column(name = "PRDT_ABRV_NAME", length = 60)
    @Comment("상품약명 (prdt_abrv_name)")
    private String prdtAbrvName;

    @Column(name = "PRDT_ENG_NAME", length = 60)
    @Comment("상품영문명 (prdt_eng_name)")
    private String prdtEngName;

    @Column(name = "SHTN_PDNO", length = 12)
    @Comment("단축상품번호 (shtn_pdno)")
    private String shtnPdno;

    @Column(name = "PRDT_RISK_GRAD_CD", length = 2)
    @Comment("상품위험등급코드 (prdt_risk_grad_cd)")
    private String prdtRiskGradCd;

    @Column(name = "PRDT_CLSF_CD", length = 6)
    @Comment("상품분류코드 (prdt_clsf_cd) — 검색 필터 기준")
    private String prdtClsfCd;

    @Column(name = "PRDT_CLSF_NM", length = 60)
    @Comment("상품분류명 (prdt_clsf_name)")
    private String prdtClsfNm;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("최종 갱신 일시")
    private LocalDateTime updatedAt;

    public StockDetail(String ticker) {
        this.ticker = ticker;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyStockInfo(Long listedShares, Long capitalAmount, Long faceValue,
                               String stockKindCd, String etfDvsnCd, String reitsKindCd,
                               String kospi200ItemYn,
                               String idxBztpLclsCd, String idxBztpMclsCd, String idxBztpSclsCd,
                               String idxBztpLclsNm, String idxBztpMclsNm, String idxBztpSclsNm,
                               String stdIdstClsfCd, String stdIdstClsfNm,
                               String frnrPsnlLmtRt, String trStopYn, String admnItemYn,
                               String lstgAbolDt, String sctsMketLstgDt) {
        this.listedShares = listedShares;
        this.capitalAmount = capitalAmount;
        this.faceValue = faceValue;
        this.stockKindCd = stockKindCd;
        this.etfDvsnCd = etfDvsnCd;
        this.reitsKindCd = reitsKindCd;
        this.kospi200ItemYn = kospi200ItemYn;
        this.idxBztpLclsCd = idxBztpLclsCd;
        this.idxBztpMclsCd = idxBztpMclsCd;
        this.idxBztpSclsCd = idxBztpSclsCd;
        this.idxBztpLclsNm = idxBztpLclsNm;
        this.idxBztpMclsNm = idxBztpMclsNm;
        this.idxBztpSclsNm = idxBztpSclsNm;
        this.stdIdstClsfCd = stdIdstClsfCd;
        this.stdIdstClsfNm = stdIdstClsfNm;
        this.frnrPsnlLmtRt = frnrPsnlLmtRt;
        this.trStopYn = trStopYn;
        this.admnItemYn = admnItemYn;
        this.lstgAbolDt = lstgAbolDt;
        this.sctsMketLstgDt = sctsMketLstgDt;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyProductInfo(String prdtName, String prdtAbrvName, String prdtEngName,
                                 String shtnPdno, String prdtRiskGradCd,
                                 String prdtClsfCd, String prdtClsfNm) {
        this.prdtName = prdtName;
        this.prdtAbrvName = prdtAbrvName;
        this.prdtEngName = prdtEngName;
        this.shtnPdno = shtnPdno;
        this.prdtRiskGradCd = prdtRiskGradCd;
        this.prdtClsfCd = prdtClsfCd;
        this.prdtClsfNm = prdtClsfNm;
        this.updatedAt = LocalDateTime.now();
    }
}
