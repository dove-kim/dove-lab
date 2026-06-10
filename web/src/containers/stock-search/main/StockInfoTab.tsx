"use client";

import { useEffect, useState } from "react";

interface StockDetail {
  ticker: string;
  name: string;
  market: string;
  isin: string | null;
  listingDate: string | null;
  secugrpNm: string | null;
  kindStkCertTpNm: string | null;
  listedShares: number | null;
  capitalAmount: number | null;
  faceValue: number | null;
  idxBztpLclsNm: string | null;
  idxBztpMclsNm: string | null;
  idxBztpSclsNm: string | null;
  stdIdstClsfNm: string | null;
  prdtClsfNm: string | null;
  kospi200ItemYn: string | null;
  trStopYn: string | null;
  admnItemYn: string | null;
  frnrPsnlLmtRt: string | null;
  prdtRiskGradCd: string | null;
  prdtName: string | null;
  prdtEngName: string | null;
  lstgAbolDt: string | null;
  sctsMketLstgDt: string | null;
}

function text(v: string | null | undefined): string {
  return v != null && v !== "" ? v : "-";
}

/** 숫자 + 단위. 값 없으면 "-". */
function numUnit(v: number | null | undefined, unit: string): string {
  return v != null ? `${v.toLocaleString()} ${unit}` : "-";
}

/** yyyyMMdd 또는 ISO 날짜 → yyyy-MM-dd. */
function ymd(v: string | null | undefined): string {
  if (!v) return "-";
  if (/^\d{8}$/.test(v)) return `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`;
  return v;
}

/** 비율 문자열 → "N%" (불필요한 0 제거). */
function rate(v: string | null | undefined): string {
  if (v == null || v === "") return "-";
  const n = Number(v);
  if (Number.isNaN(n)) return v;
  return `${n.toLocaleString(undefined, { maximumFractionDigits: 4 })}%`;
}

function yn(v: string | null | undefined): string {
  if (v === "Y") return "예";
  if (v === "N") return "아니오";
  return "-";
}

/** 라벨-값 한 줄. */
function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex py-2 border-b border-white/5">
      <span className="w-32 flex-shrink-0 text-xs text-slate-500">{label}</span>
      <span className="text-sm text-slate-200 break-all">{value}</span>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-0.5">
      <h3 className="text-sm font-semibold text-slate-300 mb-1">{title}</h3>
      <div>{children}</div>
    </section>
  );
}

/**
 * 종목 상세 탭 — STOCK_DETAIL 전체 정보를 한글 라벨로 보여준다.
 */
export default function StockInfoTab({ code }: { code: string }) {
  const [detail, setDetail] = useState<StockDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`/api/stocks/${code}/detail`)
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data: StockDetail) => setDetail(data))
      .catch(() => setError("상세 정보를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [code]);

  if (loading) {
    return <div className="p-6 text-sm text-slate-500">불러오는 중...</div>;
  }
  if (error || !detail) {
    return <div className="p-6 text-sm text-rose-400">{error ?? "정보 없음"}</div>;
  }

  return (
    <div className="h-full overflow-y-auto px-5 py-4 space-y-6">
      <Section title="기본 정보">
        <Row label="종목명" value={text(detail.name)} />
        <Row label="코드" value={text(detail.ticker)} />
        <Row label="시장" value={text(detail.market)} />
        <Row label="ISIN" value={text(detail.isin)} />
        <Row label="상장일" value={ymd(detail.listingDate)} />
        <Row label="정식명" value={text(detail.prdtName)} />
        <Row label="영문명" value={text(detail.prdtEngName)} />
      </Section>

      <Section title="분류">
        <Row label="증권그룹" value={text(detail.secugrpNm)} />
        <Row label="주권종류" value={text(detail.kindStkCertTpNm)} />
        <Row label="업종 대분류" value={text(detail.idxBztpLclsNm)} />
        <Row label="업종 중분류" value={text(detail.idxBztpMclsNm)} />
        <Row label="업종 소분류" value={text(detail.idxBztpSclsNm)} />
        <Row label="표준산업분류" value={text(detail.stdIdstClsfNm)} />
        <Row label="상품분류" value={text(detail.prdtClsfNm)} />
      </Section>

      <Section title="규모">
        <Row label="상장주식수" value={numUnit(detail.listedShares, "주")} />
        <Row label="상장자본금" value={numUnit(detail.capitalAmount, "원")} />
        <Row label="액면가" value={numUnit(detail.faceValue, "원")} />
        <Row label="외국인한도비율" value={rate(detail.frnrPsnlLmtRt)} />
        <Row label="위험등급" value={text(detail.prdtRiskGradCd)} />
      </Section>

      <Section title="상태">
        <Row label="KOSPI200" value={yn(detail.kospi200ItemYn)} />
        <Row label="거래정지" value={yn(detail.trStopYn)} />
        <Row label="관리종목" value={yn(detail.admnItemYn)} />
        <Row label="상장폐지일" value={ymd(detail.lstgAbolDt)} />
        <Row label="유가증권시장 상장일" value={ymd(detail.sctsMketLstgDt)} />
      </Section>
    </div>
  );
}
