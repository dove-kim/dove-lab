"use client";

import { useEffect, useMemo, useState } from "react";
import { cx } from "@/utils/cx";
import CommaInput from "@/components/CommaInput";
import { type PortfolioPosition, type PortfolioRebalancePlan, CUR_SYMBOL } from "@/types/portfolio";
import { type StockMatchResult } from "@/types/filter";
import { usePortfolioData, won, fxRateOf } from "./usePortfolioData";
import { StatTile } from "./StatTile";

interface RbItem {
  id: number;
  symbol: string;
  sub: string; // 계좌명 또는 "신규"
  currency: string;
  krwVal: number;
  natVal: number;
  isNew: boolean;
  inc: boolean;
  target: number;
}

// 고정폭 숫자 입력 클래스 — cx.inputNumber는 w-full이라 폭을 못 줄여서 별도.
const NUM = "bg-white/5 border border-white/15 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400/50 transition [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none";
// 표 셀 안의 소형 숫자 입력.
const NUMCELL = "w-16 bg-white/5 border border-white/15 rounded px-2 py-1 text-right text-sm text-white [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none";
/** 프리셋 현금 라인 — 계좌의 특정 통화 잔액이 전략 현금(원화·달러 등 통화별로 각각). weightPct=리밸런싱 목표 비중(슬롯은 무시). */
interface CashEntry {
  account: string;
  currency: string;
  weightPct: number;
}

type Mode = "sell" | "buy";

/**
 * 매매 계획 탭 — 리밸런싱(프리셋 목표% 매수·매도), 슬롯 계산(신규 매수액), 프리셋 관리를 제공한다.
 */
export default function RebalanceTab() {
  const { data, err } = usePortfolioData<PortfolioPosition[]>("/api/portfolio/positions");
  const [items, setItems] = useState<RbItem[]>([]);
  const [tab, setTab] = useState<"run" | "slot" | "sell" | "presets">("run");
  const [presetView, setPresetView] = useState<"items" | "weights" | "slots">("items"); // 프리셋 관리 서브탭
  const [mode, setMode] = useState<Mode>("sell");
  const [baseCur, setBaseCur] = useState("KRW");
  // 슬롯 계산 탭 — 신규 매수액(=(보유평가+현금)/슬롯). 보유평가는 장부에서 자동.
  const [slotPlanId, setSlotPlanId] = useState<number | "">("");
  const [slotN, setSlotN] = useState(8);
  const [slotCash, setSlotCash] = useState("0");
  const [slotPrice, setSlotPrice] = useState("");
  const [slotNeed, setSlotNeed] = useState("0");
  const [cash, setCash] = useState("0");
  const [search, setSearch] = useState("");
  const [newCur, setNewCur] = useState("KRW");
  const [plans, setPlans] = useState<PortfolioRebalancePlan[]>([]);
  const [planName, setPlanName] = useState("");
  const [loadedId, setLoadedId] = useState<number | "">("");
  const [savedMsg, setSavedMsg] = useState("");
  const [planSlots, setPlanSlots] = useState(8); // 프리셋 관리에서 편집·저장하는 슬롯 수
  const [accounts, setAccounts] = useState<string[]>([]);
  const [cashByAcct, setCashByAcct] = useState<Record<string, Record<string, number>>>({});
  const [cashEntries, setCashEntries] = useState<CashEntry[]>([]); // 편집 중 프리셋의 현금 소속(포지션과 분리)
  const [partRate, setPartRate] = useState(10); // 유동성 참여율(%) — 프리셋 설정값(권한 있을 때만)
  // 슬롯 계산 탭 — 그날 검색 후보를 불러와 종목별 사이징(검색 권한 있을 때만 노출).
  const [canSearch, setCanSearch] = useState(false);
  const [filters, setFilters] = useState<{ id: number; name: string }[]>([]);
  const [slotFilterId, setSlotFilterId] = useState<number | "">("");
  const [slotDate, setSlotDate] = useState("");
  const [candLoading, setCandLoading] = useState(false);
  const [candErr, setCandErr] = useState("");
  const [candStocks, setCandStocks] = useState<StockMatchResult[]>([]);

  function loadPlans() {
    fetch("/api/portfolio/rebalance-plans")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioRebalancePlan[]) => setPlans(Array.isArray(d) ? d : []))
      .catch(() => {});
  }
  useEffect(() => {
    loadPlans();
    fetch("/api/portfolio/accounts")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: { name: string }[]) => setAccounts(Array.isArray(d) ? d.map((a) => a.name) : []))
      .catch(() => {});
    fetch("/api/portfolio/cash-by-account")
      .then((r) => (r.ok ? r.json() : {}))
      .then((d) => setCashByAcct(d && typeof d === "object" ? d : {}))
      .catch(() => {});
    // 검색 권한이 있으면(=목록 200) 그날 후보 사이징 기능 노출. 403이면 조용히 숨김.
    fetch("/api/filters")
      .then((r) => (r.ok ? r.json() : null))
      .then((d: { id: number; name: string }[] | null) => {
        if (Array.isArray(d)) { setCanSearch(true); setFilters(d.map((f) => ({ id: f.id, name: f.name }))); }
      })
      .catch(() => {});
  }, []);

  function baseItems(): RbItem[] {
    return (data ?? []).map((p, i) => ({
      id: i + 1,
      symbol: p.symbol,
      sub: p.account,
      currency: p.currency,
      krwVal: p.evalKrw,
      natVal: p.currency === "KRW" ? p.evalKrw : Math.round(p.curPriceNat * p.quantity),
      isNew: false,
      inc: false,
      target: 0,
    }));
  }

  // 저장된 프리셋을 현재 보유와 매칭해 선택·목표를 복원.
  // 커스텀(신규) 항목은 종목명만으로 실제 보유에 자동 연동 → 나중에 진짜 매수하면 목표%가 그 보유에 붙는다.
  function applyPlan(plan: PortfolioRebalancePlan) {
    setCashEntries(plan.config.cash.map((c) => ({ account: c.account, currency: c.currency, weightPct: c.weightPct })));
    setPlanSlots(Math.max(1, Math.round(plan.config.slots || 8)));
    setPartRate(Math.max(0, plan.config.partRate ?? 10));
    const base = baseItems();
    const entries = plan.config.positions.map((e) => ({ ...e, custom: e.account == null || e.account === "신규" }));
    const used = new Set<number>();
    const next: RbItem[] = base.map((it) => {
      // 실제 계좌·종목 정확 매칭 우선, 없으면 커스텀 항목을 종목명으로 연동.
      let idx = entries.findIndex((e, i) => !used.has(i) && !e.custom && e.symbol === it.symbol && e.account === it.sub);
      if (idx < 0) idx = entries.findIndex((e, i) => !used.has(i) && e.custom && e.symbol === it.symbol);
      if (idx >= 0) {
        used.add(idx);
        return { ...it, inc: true, target: entries[idx].targetPct };
      }
      return it;
    });
    let nid = next.length ? Math.max(...next.map((x) => x.id)) : 0;
    entries.forEach((e, i) => {
      if (used.has(i)) return; // 아직 실제 보유가 없는 커스텀 → 신규 자리표시로 유지
      next.push({ id: ++nid, symbol: e.symbol, sub: e.account ?? "신규", currency: e.currency, krwVal: 0, natVal: 0, isNew: true, inc: true, target: e.targetPct });
    });
    setItems(next);
    setPlanName(plan.name);
    setLoadedId(plan.id);
  }

  async function savePlan() {
    const name = planName.trim();
    if (!name) {
      alert("프리셋 이름을 입력하세요.");
      return;
    }
    const positions = items
      .filter((it) => it.inc)
      .map((it) => ({ symbol: it.symbol, account: it.sub, currency: it.currency, targetPct: it.target }));
    const cash = cashEntries
      .filter((c) => c.account)
      .map((c) => ({ account: c.account, currency: c.currency, weightPct: c.weightPct }));
    if (positions.length + cash.length === 0) {
      alert("선택된 종목·현금이 없습니다.");
      return;
    }
    const config = { slots: planSlots, partRate, positions, cash };
    const res = await fetch("/api/portfolio/rebalance-plans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, config }),
    });
    if (res.ok) {
      loadPlans();
      setSavedMsg(`✓ "${name}" 저장됨 (종목 ${positions.length} · 현금 ${cash.length})`);
      setTimeout(() => setSavedMsg(""), 3000);
    } else {
      setSavedMsg("저장 실패 — 다시 시도하세요");
      setTimeout(() => setSavedMsg(""), 3000);
    }
  }

  async function deletePlan(id: number) {
    if (!confirm("이 프리셋을 삭제할까요?")) return;
    await fetch(`/api/portfolio/rebalance-plans/${id}`, { method: "DELETE" });
    loadPlans();
    setLoadedId("");
    setPlanName("");
  }

  // 선택한 검색필터를 그날 기준으로 실행해 후보 종목을 불러온다.
  async function loadCandidates() {
    if (!slotFilterId) return;
    setCandLoading(true);
    setCandErr("");
    setCandStocks([]);
    try {
      const res = await fetch(`/api/filters/${slotFilterId}/execute`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(slotDate ? { referenceDate: slotDate } : {}),
      });
      const d = await res.json();
      if (!res.ok) {
        setCandErr(d?.error === "NO_DATA_FOR_DATE" ? "해당 날짜의 데이터가 없습니다." : "검색 실행 중 오류가 발생했습니다.");
        return;
      }
      setCandStocks(Array.isArray(d?.results) ? d.results : []);
      if (d?.evaluationDate) setSlotDate(d.evaluationDate);
    } catch {
      setCandErr("네트워크 오류가 발생했습니다.");
    } finally {
      setCandLoading(false);
    }
  }

  // 프리셋 관리 탭 진입 시 보유 종목을 '추가 후보'로만 채운다(기본 미선택 — 구성은 비워둠).
  useEffect(() => {
    if (data && items.length === 0) setItems(baseItems());
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  // 비율 조정은 종목이 있어야 하므로, 종목이 비면 종목 관리로 되돌린다.
  useEffect(() => {
    if (presetView === "weights" && items.every((it) => !it.inc)) setPresetView("items");
  }, [items, presetView]);

  const currencies = useMemo(() => {
    const set = new Set<string>(["KRW"]);
    items.forEach((it) => set.add(it.currency));
    return Array.from(set);
  }, [items]);

  // 보유 포지션에서 서버가 쓴 환율을 역산(원화평가 ÷ 원통화평가).
  const fx = useMemo(() => {
    const m: Record<string, number> = {};
    for (const p of data ?? []) {
      const rate = fxRateOf(p);
      if (p.currency !== "KRW" && rate != null) {
        m[p.currency] = rate;
      }
    }
    return m;
  }, [data]);

  const val = (it: RbItem) => (baseCur === "KRW" ? it.krwVal : it.natVal);
  const amt = (x: number) => (baseCur === "KRW" ? won(Math.round(x)) : (CUR_SYMBOL[baseCur] ?? "") + won(Math.round(x)));
  // 계좌의 특정 통화 잔액(네이티브) / 그 KRW 환산.
  const cashNat = (account: string, currency: string) => cashByAcct[account]?.[currency] ?? 0;
  const cashKrw1 = (account: string, currency: string) => cashNat(account, currency) * (currency === "KRW" ? 1 : (fx[currency] ?? 0));

  const calc = useMemo(() => {
    const pool = items.filter((it) => it.inc && (baseCur === "KRW" || it.currency === baseCur));
    const V = pool.reduce((s, it) => s + val(it), 0);
    // 현금은 KRW 기준일 때만 배분 대상(계좌 현금은 KRW로 합산). cashWeight=현금 목표 비중.
    const useCash = baseCur === "KRW";
    const cashAcctKrw = useCash ? cashEntries.reduce((s, c) => s + cashKrw1(c.account, c.currency), 0) : 0;
    const cashWeight = useCash ? cashEntries.reduce((s, c) => s + c.weightPct, 0) : 0;
    const sumT = (pool.reduce((s, it) => s + it.target, 0) + cashWeight) || 1;
    const C = parseFloat(cash.replace(/[^0-9.]/g, "")) || 0;
    const Vp = V + cashAcctKrw + C; // 전체 = 종목평가 + 전략현금 + 추가금
    const acts: Record<number, number> = {};
    let cmin = 0;
    if (mode === "sell") {
      pool.forEach((it) => (acts[it.id] = (it.target / sumT) * Vp - val(it)));
    } else {
      // 매수만 — 목표 대비 부족분(underweight)만 (전략현금+추가금)으로 매수.
      const gaps = pool.map((it) => Math.max(0, (it.target / sumT) * Vp - val(it)));
      const gsum = gaps.reduce((s, g) => s + g, 0);
      const deploy = cashAcctKrw + C; // 배치 가능 현금
      cmin = Math.max(0, gsum - cashAcctKrw); // 추가금으로 더 필요한 최소
      pool.forEach((it, i) => {
        acts[it.id] = gaps[i] <= 0 || gsum <= 0 ? 0 : deploy >= gsum ? gaps[i] : (deploy * gaps[i]) / gsum;
      });
    }
    let totBuy = 0;
    let totSell = 0;
    const subs: Record<string, number> = {};
    pool.forEach((it) => {
      const a = acts[it.id] || 0;
      if (a > 0.5) {
        totBuy += a;
        if (baseCur === "KRW") subs[it.currency] = (subs[it.currency] ?? 0) + a;
      } else if (a < -0.5) totSell += -a;
    });
    const cashTarget = (cashWeight / sumT) * Vp;
    const cashCur = cashAcctKrw + C;
    return { pool, V, sumT, C, acts, cmin, totBuy, totSell, subs, cashWeight, cashTarget, cashCur, cashAcctKrw };
  }, [items, mode, baseCur, cash, cashEntries, cashByAcct, fx]);

  function setTarget(id: number, v: number) {
    setItems((prev) => prev.map((it) => (it.id === id ? { ...it, target: v } : it)));
  }
  function toggleInc(id: number, on: boolean) {
    setItems((prev) => prev.map((it) => (it.id === id ? { ...it, inc: on } : it)));
  }
  function addItem() {
    const name = search.trim();
    if (!name) return;
    setItems((prev) => [
      ...prev,
      { id: (prev.length ? prev[prev.length - 1].id : 0) + 1, symbol: name, sub: "신규", currency: newCur, krwVal: 0, natVal: 0, isNew: true, inc: true, target: 0 },
    ]);
    setSearch("");
  }

  const q = search.trim().toLowerCase();
  const selected = items.filter((it) => it.inc);
  const candidates = items.filter((it) => !it.inc && (!q || it.symbol.toLowerCase().includes(q)));
  function clearAll() {
    setItems((prev) => prev.map((it) => (it.inc ? { ...it, inc: false } : it)));
  }

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">보유 정보를 불러오지 못했습니다.</p>;
  if (!data) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  const selTargetSum = selected.reduce((s, it) => s + it.target, 0) + cashEntries.reduce((s, c) => s + c.weightPct, 0);
  const need = Math.max(0, calc.cmin);
  const loadedPlan = plans.find((p) => p.id === loadedId);

  // 슬롯 계산 — 선택한 프리셋에 담긴 보유만 평가 합산 + 현금으로 슬롯당 신규 매수액을 산출.
  const slotPlan = plans.find((p) => p.id === slotPlanId);
  const slotPositions = slotPlan
    ? (data ?? []).filter((p) => slotPlan.config.positions.some((e) => e.symbol === p.symbol && (e.account == null || e.account === "신규" || e.account === p.account)))
    : [];
  const slotHoldings = slotPositions.reduce((s, p) => s + p.evalKrw, 0);
  // 프리셋 현금 라인 계좌들의 잔액 전액(현재환율 KRW 환산) — 슬롯은 비중 무시하고 전액 반영
  const slotPresetCashKrw = (slotPlan?.config.cash ?? [])
    .reduce((s, c) => s + cashKrw1(c.account, c.currency), 0);
  const slotCashN = parseFloat(slotCash.replace(/[^0-9.]/g, "")) || 0; // 추가금(신규)
  const slotCashAvail = slotPresetCashKrw + slotCashN; // 배치 가능 현금 = 전략현금 + 추가금
  const slotTotal = slotHoldings + slotCashAvail;
  const slotAmt = slotTotal / Math.max(1, slotN);
  const slotFundable = slotAmt > 0 ? Math.floor(slotCashAvail / slotAmt) : 0;
  const slotLeftover = slotCashAvail - slotFundable * slotAmt;
  const slotPriceN = parseFloat(slotPrice.replace(/[^0-9.]/g, "")) || 0;
  const slotShares = slotPriceN > 0 ? Math.floor(slotAmt / slotPriceN) : 0;

  // 그날 후보별 사이징 — 매수액 = min(슬롯당, 참여율×거래대금), 주식수 = floor(매수액÷종가).
  const candRate = Math.max(0, partRate) / 100;
  const candRows = candStocks.map((c) => {
    const price = c.closePrice ?? 0;
    const adv = price > 0 && c.volume ? price * c.volume : 0;
    const liqCap = candRate > 0 && adv > 0 ? candRate * adv : Infinity;
    const buyAmt = Math.max(0, Math.min(slotAmt, liqCap));
    const shares = price > 0 ? Math.floor(buyAmt / price) : 0;
    const limited = liqCap < slotAmt;
    return { c, price, adv, buyAmt, shares, limited };
  });
  // 매도(급전) — 전략현금 먼저 쓰고, 부족분을 약세부터 청산(하이브리드: 마지막이 승자면 부분).
  const slotNeedN = parseFloat(slotNeed.replace(/[^0-9.]/g, "")) || 0;
  const sellShortfall = Math.max(0, slotNeedN - slotPresetCashKrw);
  const sellPlan = (() => {
    let remaining = sellShortfall;
    return [...slotPositions].sort((a, b) => a.pnlPct - b.pnlPct).map((p) => {
      if (remaining <= 0.5) return { p, sell: 0, partial: false };
      if (p.evalKrw <= remaining) { remaining -= p.evalKrw; return { p, sell: p.evalKrw, partial: false }; }
      const partial = p.pnlPct > 0; // 승자면 필요분만(부분), 패자면 통째(초과)
      const sell = partial ? remaining : p.evalKrw;
      remaining = 0;
      return { p, sell, partial };
    });
  })();
  const sellTotal = sellPlan.reduce((s, x) => s + x.sell, 0);
  const sellExcess = Math.max(0, sellTotal - sellShortfall); // 통째 매도로 생긴 남는 현금

  return (
    <div className="flex flex-col gap-5">
      <div className="flex gap-1 border-b border-white/10">
        {([["run", "리밸런싱"], ["slot", "슬롯 계산"], ["sell", "매도(급전)"], ["presets", "프리셋 관리"]] as const).map(([k, label]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={
              "px-4 py-3 text-sm font-medium border-b-2 -mb-px transition cursor-pointer " +
              (tab === k ? "border-indigo-400 text-indigo-300" : "border-transparent text-slate-400 hover:text-white")
            }
          >
            {label}
          </button>
        ))}
      </div>

      {/* ── 리밸런싱 ── */}
      {tab === "run" && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-white/10 bg-white/5 p-2.5">
            <span className="text-xs text-slate-400">프리셋</span>
            <select
              value={loadedId}
              onChange={(e) => {
                const p = plans.find((x) => x.id === Number(e.target.value));
                if (p) applyPlan(p);
              }}
              className={cx.select}
            >
              <option value="">선택…</option>
              {plans.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.config.positions.length})
                </option>
              ))}
            </select>
            {loadedPlan && <span className="text-xs text-slate-500">불러옴 · 목표는 프리셋 관리에서 수정</span>}
          </div>

          {plans.length === 0 ? (
            <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">
              저장된 프리셋이 없습니다. <span className="text-slate-300">프리셋 관리</span>에서 목표 배분을 먼저 만드세요.
            </p>
          ) : !loadedPlan ? (
            <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">
              프리셋을 선택하면 그 목표 배분으로 매수·매도를 계산합니다. (신규 매수액은 &lsquo;슬롯 계산&rsquo; 탭)
            </p>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {(
                  [
                    { m: "sell" as Mode, t: "매도+매수", d: "초과 종목을 팔고 부족 종목을 사서 목표에 맞춤. 추가금 없어도 가능." },
                    { m: "buy" as Mode, t: "매수만", d: "매도 없이 추가금으로 부족분만 매수. 목표 0% 종목은 그대로 둠." },
                  ]
                ).map((c) => (
                  <button
                    key={c.m}
                    onClick={() => setMode(c.m)}
                    className={
                      "text-left bg-white/5 rounded-xl p-4 border transition cursor-pointer " +
                      (mode === c.m ? "border-indigo-500" : "border-white/10 hover:border-white/25")
                    }
                  >
                    <div className="text-white font-medium mb-1">{c.t}</div>
                    <div className="text-xs text-slate-400">{c.d}</div>
                  </button>
                ))}
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <label className="text-sm text-slate-400">투자금 통화</label>
                <select value={baseCur} onChange={(e) => setBaseCur(e.target.value)} className={cx.select}>
                  {currencies.map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </select>
                <label className="text-sm text-slate-400 ml-2">투자금(추가)</label>
                <CommaInput decimal value={cash} onChange={setCash} className={NUM + " w-32"} />
                {mode === "buy" && (
                  <button onClick={() => setCash(String(Math.round(need)))} className={cx.btnSecondary}>완전 채우기</button>
                )}
              </div>

              <p className="text-xs text-slate-500">
                평가·비중은 현재환율 KRW 기준, 해외 종목은 실행용 원통화 환산액(≈)을 함께 표시. 목표%는 프리셋 값(읽기 전용).
              </p>

              <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
                <table className={cx.table.root}>
                  <thead className={cx.table.head}>
                    <tr>
                      <th className={cx.table.th}>종목</th>
                      <th className={cx.table.th}>구분</th>
                      <th className={cx.table.th + " text-right"}>평가액</th>
                      <th className={cx.table.th + " text-right"}>현재</th>
                      <th className={cx.table.th + " text-right"}>목표%</th>
                      <th className={cx.table.th + " text-right"}>조치</th>
                    </tr>
                  </thead>
                  <tbody className={cx.table.body}>
                    {calc.pool.map((it) => {
                      const a = calc.acts[it.id] || 0;
                      const curPct = calc.V ? ((val(it) / calc.V) * 100).toFixed(1) : "0.0";
                      const nativeEq =
                        baseCur === "KRW" && it.currency !== "KRW" && fx[it.currency]
                          ? ` (≈${CUR_SYMBOL[it.currency]}${won(Math.round(Math.abs(a) / fx[it.currency]))})`
                          : "";
                      return (
                        <tr key={it.id} className={cx.table.tr}>
                          <td className={cx.table.td + " text-white font-medium"}>{it.symbol}</td>
                          <td className={cx.table.td}>
                            {it.sub} <span className="text-xs text-slate-500">{it.currency}</span>
                          </td>
                          <td className={cx.table.td + " text-right tabular-nums"}>{val(it) ? amt(val(it)) : "—"}</td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{curPct}%</td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-300"}>{it.target}%</td>
                          <td className={cx.table.td + " text-right tabular-nums"}>
                            {a > 0.5 ? (
                              <span className="text-rose-300">매수 {amt(a)}{nativeEq}</span>
                            ) : a < -0.5 ? (
                              <span className="text-sky-300">매도 {amt(-a)}{nativeEq}</span>
                            ) : (
                              <span className="text-slate-600">-</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                    {calc.cashWeight > 0 && (
                      <tr className={cx.table.tr}>
                        <td className={cx.table.td + " text-white font-medium"}>현금</td>
                        <td className={cx.table.td + " text-xs text-slate-500"}>전략 계좌</td>
                        <td className={cx.table.td + " text-right tabular-nums"}>{amt(calc.cashCur)}</td>
                        <td className={cx.table.td + " text-right tabular-nums text-slate-600"}>—</td>
                        <td className={cx.table.td + " text-right tabular-nums text-slate-300"}>{Math.round(calc.cashWeight)}%</td>
                        <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>목표 {amt(calc.cashTarget)}</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <StatTile size="base" label="목표 합계(현금 포함)" value={Math.round(calc.sumT) + "%"} tone={Math.round(calc.sumT) === 100 ? "text-white" : "text-sky-300"} />
                {mode === "sell" ? (
                  <>
                    <StatTile size="base" label="총 매도" value={amt(calc.totSell)} tone="text-sky-300" />
                    <StatTile size="base" label="총 매수" value={amt(calc.totBuy)} tone="text-rose-300" />
                    <StatTile size="base" label="추가금" value={amt(calc.C)} />
                  </>
                ) : (
                  <>
                    <StatTile size="base" label="매수 배분" value={amt(calc.totBuy)} tone="text-rose-300" />
                    <StatTile size="base"
                      label="상태"
                      value={calc.C >= calc.cmin ? "완전" : `부분(−${amt(calc.cmin - calc.C)})`}
                      tone={calc.C >= calc.cmin ? "text-rose-300" : "text-sky-300"}
                    />
                    <StatTile size="base" label="추가금" value={amt(calc.C)} />
                  </>
                )}
              </div>

              {baseCur === "KRW" && Object.keys(calc.subs).length > 0 && (
                <div className="text-sm text-slate-300">
                  <div className="font-medium mb-1">통화별 매수 소계 — 외화는 환전 후 통화별 계산으로</div>
                  <div className="flex flex-wrap gap-2">
                    {Object.entries(calc.subs).map(([c, v]) => (
                      <span key={c} className="rounded-full bg-white/5 px-2.5 py-1 text-xs">
                        {c} 매수 {won(Math.round(v))}
                        {c !== "KRW" && fx[c] ? ` → ${CUR_SYMBOL[c]}${won(Math.round(v / fx[c]))}` : ""}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* ── 슬롯 계산 ── */}
      {tab === "slot" && (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-slate-400">
            신규 타점에 <b className="text-slate-200">얼마</b> 넣을지 계산합니다. 슬롯당 = (프리셋 내 보유 평가 + 전략현금 + 추가금) ÷ 슬롯수 — 전략현금은 프리셋의 계좌·비율로 자동 반영.
          </p>
          <div className="flex flex-wrap items-end gap-4 rounded-xl border border-white/10 bg-white/5 p-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">전략 프리셋</label>
              <select
                value={slotPlanId}
                onChange={(e) => {
                  const id = e.target.value ? Number(e.target.value) : "";
                  setSlotPlanId(id);
                  const cfg = id ? plans.find((x) => x.id === id)?.config : null;
                  if (id && cfg) { setSlotN(Math.max(1, Math.round(cfg.slots || 8))); setPartRate(Math.max(0, cfg.partRate ?? 10)); }
                }}
                className={cx.select}
              >
                <option value="">프리셋 선택…</option>
                {plans.map((p) => (
                  <option key={p.id} value={p.id}>{p.name} ({p.config.positions.length})</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1">슬롯 수 <span className="text-slate-600">(프리셋값, 바꿔 계산 가능)</span></label>
              <input type="number" min={1} value={slotN}
                onChange={(e) => setSlotN(Math.max(1, Math.floor(Number(e.target.value) || 1)))}
                className={NUM + " w-20"} />
            </div>
          </div>

          {!slotPlan ? (
            <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">
              프리셋을 선택하세요 — 그 프리셋에 담긴 종목의 현재 평가액만으로 슬롯을 나눕니다.
              <br />없으면 <span className="text-slate-300">프리셋 관리</span>에서 전략 종목을 담아 저장하세요.
            </p>
          ) : (
            <>
              <div className="text-sm text-slate-400">
                보유 <b className="text-white tabular-nums">{won(Math.round(slotHoldings))}</b>
                <span className="text-slate-500"> · {slotPositions.length}종목</span>
                {slotPresetCashKrw > 0 && <> · 전략현금 <b className="text-white tabular-nums">{won(Math.round(slotPresetCashKrw))}</b></>}
              </div>
              <div className="flex flex-wrap items-end gap-4">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">이번 투자금(현금·추가금)</label>
                  <CommaInput decimal value={slotCash} onChange={setSlotCash} className={NUM + " w-36"} />
                </div>
              </div>
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <StatTile size="base" label="슬롯당 매수액" value={won(Math.round(slotAmt))} tone="text-rose-300" />
                <StatTile size="base" label="현금으로 채울 슬롯" value={`${slotFundable}개`} tone="text-white" />
                <StatTile size="base" label="소요 / 남는 현금" value={`${won(Math.round(slotFundable * slotAmt))} / ${won(Math.round(slotLeftover))}`} />
                <StatTile size="base" label="전략 총자산(보유+현금)" value={won(Math.round(slotTotal))} />
              </div>
              {slotCashAvail > 0 && slotFundable === 0 && (
                <p className="text-sm text-amber-300">
                  배치 가능 현금(전략현금+추가금 {won(Math.round(slotCashAvail))})이 슬롯당({won(Math.round(slotAmt))})보다 적어 한 슬롯도 다 못 채웁니다 — 전액 부분 매수하거나 다음 적립을 기다리세요.
                </p>
              )}
              <div className="flex flex-wrap items-end gap-4 rounded-xl border border-white/10 bg-slate-900/40 p-4">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">신규 후보 현재가(선택)</label>
                  <CommaInput value={slotPrice} onChange={setSlotPrice} className={NUM + " w-32"} />
                </div>
                <div className="text-sm text-slate-300 pb-2">
                  {slotPriceN > 0 ? (
                    <>→ <b className="text-white">{slotShares.toLocaleString()}주</b>{" "}
                      <span className="text-slate-500">(약 {won(Math.round(slotShares * slotPriceN))} · 슬롯당 {won(Math.round(slotAmt))} 기준)</span></>
                  ) : (
                    <span className="text-slate-500">현재가를 넣으면 슬롯당 금액으로 살 정수 주식수를 보여줍니다.</span>
                  )}
                </div>
              </div>
              <p className="text-xs text-slate-500">
                부족한 슬롯은 이번에 비워두고 다음 월급 때 채우면 됩니다(적립식). 슬롯 수는 프리셋에 저장돼 자동 로드, 여기서 바꿔 계산만 해볼 수도 있습니다.
              </p>

              {canSearch && (
                <div className="flex flex-col gap-3 rounded-xl border border-indigo-500/25 bg-indigo-600/5 p-4">
                  <h3 className="text-sm font-medium text-slate-200">그날 후보 사이징 <span className="text-xs font-normal text-slate-500">검색필터 결과에 슬롯 계산을 얹기</span></h3>
                  <div className="flex flex-wrap items-end gap-4">
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">검색필터</label>
                      <select value={slotFilterId} onChange={(e) => setSlotFilterId(e.target.value ? Number(e.target.value) : "")} className={cx.select}>
                        <option value="">필터 선택…</option>
                        {filters.map((f) => (<option key={f.id} value={f.id}>{f.name}</option>))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">기준일 <span className="text-slate-600">(비우면 최신)</span></label>
                      <input type="date" value={slotDate} onChange={(e) => setSlotDate(e.target.value)} className={NUM + " w-40"} />
                    </div>
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">참여율 %</label>
                      <input type="number" min={0} max={100} value={partRate} onChange={(e) => setPartRate(Math.max(0, Math.min(100, Number(e.target.value) || 0)))} className={NUM + " w-20"} />
                    </div>
                    <button onClick={loadCandidates} disabled={!slotFilterId || candLoading} className={cx.btnPrimary + " disabled:opacity-40"}>
                      {candLoading ? "불러오는 중…" : "불러오기"}
                    </button>
                  </div>
                  {candErr && <p className="text-sm text-rose-300">{candErr}</p>}
                  {!candErr && candStocks.length > 0 && (
                    <>
                      <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
                        <table className={cx.table.root}>
                          <thead className={cx.table.head}>
                            <tr>
                              <th className={cx.table.th}>종목</th>
                              <th className={cx.table.th + " text-right whitespace-nowrap"}>모델</th>
                              <th className={cx.table.th + " text-right whitespace-nowrap"}>현재가</th>
                              <th className={cx.table.th + " text-right whitespace-nowrap"}>거래대금</th>
                              <th className={cx.table.th + " text-right whitespace-nowrap"}>매수액</th>
                              <th className={cx.table.th + " text-right whitespace-nowrap"}>주식수</th>
                            </tr>
                          </thead>
                          <tbody className={cx.table.body}>
                            {candRows.map(({ c, price, adv, buyAmt, shares, limited }) => (
                              <tr key={c.code} className={cx.table.tr}>
                                <td className={cx.table.td + " text-white font-medium whitespace-nowrap"}>{c.name}<span className="ml-1.5 text-xs font-normal text-slate-500">{c.code}</span></td>
                                <td className={cx.table.td + " text-right tabular-nums " + (c.modelScore != null ? "text-indigo-300" : "text-slate-600")}>{c.modelScore != null ? c.modelScore.toFixed(2) : "—"}</td>
                                <td className={cx.table.td + " text-right tabular-nums text-slate-300"}>{price > 0 ? price.toLocaleString() : "—"}</td>
                                <td className={cx.table.td + " text-right tabular-nums text-slate-500"}>{adv > 0 ? won(Math.round(adv)) : "—"}</td>
                                <td className={cx.table.td + " text-right tabular-nums text-rose-300"}>
                                  {won(Math.round(buyAmt))}{limited && <span className="ml-1 text-xs text-amber-300">유동성</span>}
                                </td>
                                <td className={cx.table.td + " text-right tabular-nums text-white"}>{shares > 0 ? shares.toLocaleString() : "—"}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <p className="text-xs text-slate-500">
                        매수액 = min(슬롯당 {won(Math.round(slotAmt))}, 참여율 {partRate}%×거래대금). <span className="text-amber-300">유동성</span> 표시는 거래대금이 얇아 슬롯보다 작게 잡힌 종목 — 그만큼만 사고 나머지는 다음 후보/현금으로.
                      </p>
                    </>
                  )}
                  {!candErr && !candLoading && candStocks.length === 0 && (
                    <p className="text-xs text-slate-500">검색필터를 골라 불러오면 그날 걸린 종목마다 매수액·주식수를 계산합니다.</p>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* ── 매도 (급전) — 슬롯과 무관, 필요 현금을 약세부터 청산 ── */}
      {tab === "sell" && (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-slate-400">
            급전이 필요할 때 <b className="text-slate-200">전략현금 먼저 쓰고, 부족분만 약세(수익률 낮은)부터</b> 청산합니다. 슬롯과 무관 — 승자는 최대한 보존.
          </p>
          <div className="flex flex-wrap items-end gap-4 rounded-xl border border-white/10 bg-white/5 p-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">전략 프리셋</label>
              <select value={slotPlanId} onChange={(e) => setSlotPlanId(e.target.value ? Number(e.target.value) : "")} className={cx.select}>
                <option value="">프리셋 선택…</option>
                {plans.map((p) => (<option key={p.id} value={p.id}>{p.name} ({p.config.positions.length})</option>))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1">필요 현금</label>
              <CommaInput decimal value={slotNeed} onChange={setSlotNeed} className={NUM + " w-36"} />
            </div>
          </div>
          {!slotPlan ? (
            <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">프리셋을 선택하세요 — 그 프리셋 보유·현금으로 급전을 마련합니다.</p>
          ) : (
            <>
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <StatTile size="base" label="전략현금 먼저 사용" value={won(Math.round(Math.min(slotPresetCashKrw, slotNeedN)))} tone="text-white" />
                <StatTile size="base" label="매도 필요분" value={won(Math.round(sellShortfall))} tone="text-sky-300" />
                <StatTile size="base" label="매도 합계 / 남는 현금" value={`${won(Math.round(sellTotal))} / ${won(Math.round(sellExcess))}`} />
                <StatTile size="base" label="청산 가능(보유 전액)" value={won(Math.round(slotHoldings))} />
              </div>
              {slotNeedN > 0 && sellShortfall <= 0.5 && (
                <p className="text-sm text-emerald-300">전략현금({won(Math.round(slotPresetCashKrw))})으로 필요 현금이 충당됩니다 — 매도 불필요.</p>
              )}
              {sellShortfall > slotHoldings + 0.5 && (
                <p className="text-sm text-amber-300">전략현금 + 보유 전액을 팔아도 필요 현금({won(Math.round(slotNeedN))})에 못 미칩니다.</p>
              )}
              {sellShortfall > 0.5 && slotPositions.length > 0 && (
                <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
                  <table className={cx.table.root}>
                    <thead className={cx.table.head}>
                      <tr>
                        <th className={cx.table.th}>종목</th>
                        <th className={cx.table.th}>계좌</th>
                        <th className={cx.table.th + " text-right"}>수익률</th>
                        <th className={cx.table.th + " text-right"}>평가액</th>
                        <th className={cx.table.th + " text-right"}>매도액</th>
                      </tr>
                    </thead>
                    <tbody className={cx.table.body}>
                      {sellPlan.map(({ p, sell, partial }, i) => (
                        <tr key={i} className={cx.table.tr + (sell > 0.5 ? " bg-sky-600/10" : "")}>
                          <td className={cx.table.td + " text-white font-medium"}>{p.symbol}</td>
                          <td className={cx.table.td}>{p.account} <span className="text-xs text-slate-500">{p.currency}</span></td>
                          <td className={cx.table.td + " text-right tabular-nums " + (p.pnlPct >= 0 ? "text-red-400" : "text-blue-400")}>{p.pnlPct >= 0 ? "+" : ""}{p.pnlPct.toFixed(1)}%</td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{won(Math.round(p.evalKrw))}</td>
                          <td className={cx.table.td + " text-right tabular-nums " + (sell > 0.5 ? "text-sky-300" : "text-slate-600")}>
                            {sell > 0.5 ? won(Math.round(sell)) : "-"}{partial ? <span className="text-xs text-amber-300"> 부분</span> : ""}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="text-xs text-slate-500">
                <b className="text-slate-300">전략현금 먼저 → 부족분만 약세부터</b>. 마지막이 수익 중이면 필요분만 부분 매도(승자 보존), 손실이면 통째(초과분=남는 현금). 급전 매도는 전략 밖이니 최소한만.
              </p>
            </>
          )}
        </div>
      )}

      {/* ── 프리셋 관리 ── */}
      {tab === "presets" && (
        <div className="flex flex-col gap-4">
          {/* 공통: 프리셋 불러오기·이름·저장 */}
          <div className="flex flex-wrap items-end gap-x-4 gap-y-3 rounded-xl border border-white/10 bg-white/5 p-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">불러오기</label>
              <select value="" onChange={(e) => { const p = plans.find((x) => x.id === Number(e.target.value)); if (p) applyPlan(p); }} className={cx.select}>
                <option value="">프리셋 선택…</option>
                {plans.map((p) => (<option key={p.id} value={p.id}>{p.name}</option>))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1">프리셋 이름</label>
              <input value={planName} onChange={(e) => setPlanName(e.target.value)} placeholder="예: 돌파 전략" className={cx.input + " w-44"} />
            </div>
            <button onClick={savePlan} className={cx.btnPrimary}>{plans.find((p) => p.name === planName.trim()) ? "덮어쓰기" : "저장"}</button>
            {planName.trim() && plans.find((p) => p.name === planName.trim()) && (
              <button onClick={() => deletePlan(plans.find((p) => p.name === planName.trim())!.id)} className={cx.btnSecondary}>삭제</button>
            )}
            {savedMsg && <span className={"self-center text-sm font-medium " + (savedMsg.startsWith("✓") ? "text-emerald-300" : "text-rose-300")}>{savedMsg}</span>}
          </div>

          {/* 서브탭 — 비율 조정은 종목이 선택돼야 활성 */}
          <div className="flex gap-1 rounded-lg border border-white/10 bg-white/5 p-1 w-fit">
            {([["items", "종목 관리", false], ["weights", "비율 조정", selected.length === 0], ["slots", "슬롯 설정", false]] as const).map(([v, label, disabled]) => (
              <button key={v} disabled={disabled} onClick={() => { if (!disabled) setPresetView(v); }}
                title={disabled ? "먼저 종목 관리에서 종목을 담으세요" : undefined}
                className={"px-4 py-1.5 rounded-md text-sm font-medium transition " + (disabled ? "text-slate-600 cursor-not-allowed" : presetView === v ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white")}>{label}</button>
            ))}
          </div>

          {/* ── 종목 관리 — 구성 + 추가 ── */}
          {presetView === "items" && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between h-6">
                  <h3 className="text-sm font-medium text-slate-300">현재 구성 <span className="text-slate-500">({selected.length}종목 + {cashEntries.length}현금)</span></h3>
                  {(selected.length > 0 || cashEntries.length > 0) && (
                    <button onClick={() => { clearAll(); setCashEntries([]); }} className="text-xs text-slate-400 hover:text-rose-300 transition">모두 비우기</button>
                  )}
                </div>
                <div className="max-h-[28rem] rounded-xl border border-white/10 bg-slate-900/40 overflow-auto">
                  <table className={cx.table.root}>
                    <thead className={cx.table.head + " sticky top-0 z-10"}>
                      <tr>
                        <th className={cx.table.th + " w-full"}>종목 / 현금</th>
                        <th className={cx.table.th + " text-right whitespace-nowrap"}>평가액</th>
                        <th className={cx.table.th}></th>
                      </tr>
                    </thead>
                    <tbody className={cx.table.body}>
                      {selected.map((it) => (
                        <tr key={it.id} className={cx.table.tr}>
                          <td className={cx.table.td + " text-white font-medium"}>{it.symbol}<span className="ml-1.5 text-xs font-normal text-slate-500">{it.isNew ? "신규" : it.sub}·{it.currency}</span></td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{it.krwVal ? won(it.krwVal) : "—"}</td>
                          <td className={cx.table.td + " text-right"}><button onClick={() => toggleInc(it.id, false)} className="text-xs text-slate-500 hover:text-rose-300 whitespace-nowrap">제외</button></td>
                        </tr>
                      ))}
                      {cashEntries.map((c, i) => (
                        <tr key={"cash" + i} className={cx.table.tr + " bg-sky-500/5"}>
                          <td className={cx.table.td + " font-medium text-sky-200"}>현금<span className="ml-1.5 text-xs font-normal text-slate-500">{c.account}·{c.currency}</span></td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{won(Math.round(cashKrw1(c.account, c.currency)))}</td>
                          <td className={cx.table.td + " text-right"}><button onClick={() => setCashEntries(cashEntries.filter((_, j) => j !== i))} className="text-xs text-slate-500 hover:text-rose-300 whitespace-nowrap">제외</button></td>
                        </tr>
                      ))}
                      {selected.length === 0 && cashEntries.length === 0 && (
                        <tr><td colSpan={3} className="px-4 py-10 text-center text-sm text-slate-500">오른쪽에서 종목·현금을 담으세요.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="flex flex-col gap-3">
                <div className="rounded-xl border border-white/10 bg-slate-900/40 p-3 flex flex-col gap-2">
                  <h3 className="text-sm font-medium text-slate-300">종목 추가</h3>
                  <div className="flex flex-wrap gap-2">
                    <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="종목 검색 / 직접 입력" className={cx.input + " flex-1 min-w-[8rem]"} />
                    <select value={newCur} onChange={(e) => setNewCur(e.target.value)} className={cx.select}>{["KRW", "USD", "JPY", "CNY", "EUR", "HKD"].map((c) => (<option key={c}>{c}</option>))}</select>
                    <button onClick={addItem} className={cx.btnSecondary}>＋ 직접</button>
                  </div>
                  {candidates.length > 0 && (
                    <div className="max-h-[28rem] rounded-lg border border-white/10 overflow-auto">
                      <table className={cx.table.root}>
                        <tbody className={cx.table.body}>
                          {candidates.map((it) => (
                            <tr key={it.id} onClick={() => toggleInc(it.id, true)} className={cx.table.tr + " cursor-pointer hover:bg-white/5"}>
                              <td className={cx.table.td + " text-white"}>{it.symbol}<span className="ml-1.5 text-xs text-slate-500">{it.isNew ? "신규" : it.sub}·{it.currency}</span></td>
                              <td className={cx.table.td + " text-right tabular-nums text-slate-500"}>{it.krwVal ? won(it.krwVal) : "—"}</td>
                              <td className={cx.table.td + " text-right text-indigo-300"}>＋</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                <div className="rounded-xl border border-white/10 bg-slate-900/40 p-3 flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-medium text-slate-300">현금 계좌 <span className="text-xs font-normal text-slate-500">통화별</span></h3>
                    <button onClick={() => setCashEntries([...cashEntries, { account: accounts[0] ?? "", currency: "KRW", weightPct: 0 }])} className={cx.btnSecondary}>＋ 계좌·통화</button>
                  </div>
                  {cashEntries.length === 0 ? (
                    <p className="text-xs text-slate-500">계좌의 통화(원화·달러 등)를 넣으면 그 잔액이 전략 현금이 됩니다. 통화별로 각각. 비중%는 &lsquo;비율 조정&rsquo;에서.</p>
                  ) : (
                    <div className="flex flex-col gap-2">
                      {cashEntries.map((c, i) => (
                        <div key={i} className="flex flex-wrap items-center gap-2 text-sm">
                          <select value={c.account} onChange={(e) => setCashEntries(cashEntries.map((x, j) => (j === i ? { ...x, account: e.target.value } : x)))} className={cx.select}>
                            {accounts.length === 0 && <option value="">계좌 없음</option>}
                            {accounts.map((a) => (<option key={a} value={a}>{a}</option>))}
                          </select>
                          <select value={c.currency} onChange={(e) => setCashEntries(cashEntries.map((x, j) => (j === i ? { ...x, currency: e.target.value } : x)))} className={cx.select}>
                            {["KRW", "USD", "JPY", "CNY", "EUR", "HKD"].map((cur) => (<option key={cur}>{cur}</option>))}
                          </select>
                          <span className="ml-auto text-xs text-slate-500 tabular-nums">{Math.round(cashNat(c.account, c.currency)).toLocaleString()} {c.currency}</span>
                          <button onClick={() => setCashEntries(cashEntries.filter((_, j) => j !== i))} className="text-xs text-slate-500 hover:text-rose-300 transition">제거</button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* ── 비율 조정 — 목표% ── */}
          {presetView === "weights" && (
            <div className="flex flex-col gap-3">
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-white/10 bg-slate-900/40 px-4 py-2.5 text-sm">
                <span className="text-slate-400">종목 <b className="text-white">{selected.length}</b> + 현금 <b className="text-white">{cashEntries.length}</b></span>
                <span className="text-slate-400">목표합 <b className={Math.round(selTargetSum) === 100 ? "text-emerald-300" : "text-amber-300"}>{Math.round(selTargetSum)}%</b> <span className="text-xs text-slate-500">합 100 권장</span></span>
              </div>
              {selected.length === 0 ? (
                <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">먼저 &lsquo;종목 관리&rsquo;에서 종목을 담으세요.</p>
              ) : (
                <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-auto max-h-[64vh]">
                  <table className={cx.table.root}>
                    <thead className={cx.table.head + " sticky top-0 z-10"}>
                      <tr>
                        <th className={cx.table.th + " w-full"}>종목 / 현금</th>
                        <th className={cx.table.th + " text-right whitespace-nowrap"}>평가액</th>
                        <th className={cx.table.th + " text-right whitespace-nowrap"}>목표%</th>
                      </tr>
                    </thead>
                    <tbody className={cx.table.body}>
                      {selected.map((it) => (
                        <tr key={it.id} className={cx.table.tr}>
                          <td className={cx.table.td + " text-white font-medium"}>{it.symbol}<span className="ml-1.5 text-xs font-normal text-slate-500">{it.isNew ? "신규" : it.sub}·{it.currency}</span></td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{it.krwVal ? won(it.krwVal) : "—"}</td>
                          <td className={cx.table.td + " text-right"}><input type="number" value={it.target} onChange={(e) => setTarget(it.id, parseFloat(e.target.value) || 0)} className={NUMCELL} /></td>
                        </tr>
                      ))}
                      {cashEntries.map((c, i) => (
                        <tr key={"cash" + i} className={cx.table.tr + " bg-sky-500/5"}>
                          <td className={cx.table.td + " font-medium text-sky-200"}>현금<span className="ml-1.5 text-xs font-normal text-slate-500">{c.account}·{c.currency}</span></td>
                          <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{won(Math.round(cashKrw1(c.account, c.currency)))}</td>
                          <td className={cx.table.td + " text-right"}><input type="number" value={c.weightPct} onChange={(e) => setCashEntries(cashEntries.map((x, j) => (j === i ? { ...x, weightPct: Math.max(0, Math.min(100, parseFloat(e.target.value) || 0)) } : x)))} className={NUMCELL} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="text-xs text-slate-500">&lsquo;리밸런싱&rsquo; 탭이 이 목표%로 매수/매도량을 계산합니다. 슬롯 계산은 비율을 쓰지 않습니다.</p>
            </div>
          )}

          {/* ── 슬롯 설정 — 슬롯 수 + 참여율 ── */}
          {presetView === "slots" && (
            <div className="flex flex-col gap-4 rounded-xl border border-white/10 bg-slate-900/40 p-5 max-w-2xl">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <div className="text-sm font-medium text-slate-200">슬롯 수</div>
                  <p className="text-xs text-slate-500 mt-0.5">전략 총자산을 몇 등분해 한 종목에 넣을지. 슬롯당 = (보유+현금+추가금) ÷ 슬롯 수.</p>
                </div>
                <input type="number" min={1} value={planSlots} onChange={(e) => setPlanSlots(Math.max(1, Math.floor(Number(e.target.value) || 1)))} className={NUM + " w-24"} />
              </div>
              {canSearch && (
                <div className="flex items-center justify-between gap-4 border-t border-white/10 pt-4">
                  <div>
                    <div className="text-sm font-medium text-slate-200">유동성 참여율 %</div>
                    <p className="text-xs text-slate-500 mt-0.5">종목별 상한 = 참여율 × 그날 거래대금. -10% 스탑을 시장 안 밀고 하루 안에 청산 가능한 선(보수 5·보통 10).</p>
                  </div>
                  <input type="number" min={0} max={100} value={partRate} onChange={(e) => setPartRate(Math.max(0, Math.min(100, Number(e.target.value) || 0)))} className={NUM + " w-24"} />
                </div>
              )}
              <p className="text-xs text-slate-500 border-t border-white/10 pt-4">두 값은 프리셋에 함께 저장돼 &lsquo;슬롯 계산&rsquo; 탭에서 자동 로드됩니다.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

