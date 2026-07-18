"use client";

import { useEffect, useMemo, useState } from "react";
import { cx } from "@/utils/cx";
import CommaInput from "@/components/CommaInput";
import { type PortfolioPosition, type PortfolioRebalancePlan, CUR_SYMBOL } from "@/types/portfolio";
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

type Mode = "sell" | "buy";

/**
 * 리밸런싱 탭 — 프리셋 관리(목표 배분 저장)와 리밸런싱 실행(프리셋 불러와 계산)을 분리한다.
 */
export default function RebalanceTab() {
  const { data, err } = usePortfolioData<PortfolioPosition[]>("/api/portfolio/positions");
  const [items, setItems] = useState<RbItem[]>([]);
  const [tab, setTab] = useState<"run" | "presets">("run");
  const [mode, setMode] = useState<Mode>("sell");
  const [baseCur, setBaseCur] = useState("KRW");
  const [cash, setCash] = useState("0");
  const [search, setSearch] = useState("");
  const [newCur, setNewCur] = useState("KRW");
  const [plans, setPlans] = useState<PortfolioRebalancePlan[]>([]);
  const [planName, setPlanName] = useState("");
  const [loadedId, setLoadedId] = useState<number | "">("");

  function loadPlans() {
    fetch("/api/portfolio/rebalance-plans")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioRebalancePlan[]) => setPlans(Array.isArray(d) ? d : []))
      .catch(() => {});
  }
  useEffect(() => {
    loadPlans();
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
    const base = baseItems();
    const entries = plan.entries.map((e) => ({ ...e, custom: e.account == null || e.account === "신규" }));
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
    const entries = items
      .filter((it) => it.inc)
      .map((it) => ({ symbol: it.symbol, account: it.sub, currency: it.currency, targetPct: it.target }));
    if (entries.length === 0) {
      alert("선택된 종목이 없습니다.");
      return;
    }
    const res = await fetch("/api/portfolio/rebalance-plans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, entries }),
    });
    if (res.ok) loadPlans();
  }

  async function deletePlan(id: number) {
    if (!confirm("이 프리셋을 삭제할까요?")) return;
    await fetch(`/api/portfolio/rebalance-plans/${id}`, { method: "DELETE" });
    loadPlans();
    setLoadedId("");
    setPlanName("");
  }

  // 프리셋 관리 탭 진입 시 아직 아무것도 없으면 보유 종목을 후보로 채운다.
  useEffect(() => {
    if (data && items.length === 0) setItems(baseItems().map((it) => ({ ...it, inc: true, target: it.krwVal ? 0 : 0 })));
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

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

  const calc = useMemo(() => {
    const pool = items.filter((it) => it.inc && (baseCur === "KRW" || it.currency === baseCur));
    const V = pool.reduce((s, it) => s + val(it), 0);
    const sumT = pool.reduce((s, it) => s + it.target, 0) || 1;
    const C = parseFloat(cash.replace(/[^0-9.]/g, "")) || 0;
    const acts: Record<number, number> = {};
    let cmin = 0;
    if (mode === "sell") {
      const Vp = V + C;
      pool.forEach((it) => (acts[it.id] = (it.target / sumT) * Vp - val(it)));
    } else {
      // 매수만 — 매도 없음. 전체(현재+추가금) 목표 대비 부족분(underweight)만 추가금으로 매수.
      // 초과·0% 종목은 못 팔므로 그대로 둔다(시간이 지나며 희석).
      const Vp = V + C;
      const gaps = pool.map((it) => Math.max(0, (it.target / sumT) * Vp - val(it)));
      const gsum = gaps.reduce((s, g) => s + g, 0);
      cmin = gsum; // 부족분을 모두 채우는 데 필요한 추가금
      pool.forEach((it, i) => {
        acts[it.id] = gaps[i] <= 0 || gsum <= 0 ? 0 : C >= gsum ? gaps[i] : (C * gaps[i]) / gsum;
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
    return { pool, V, sumT, C, acts, cmin, totBuy, totSell, subs };
  }, [items, mode, baseCur, cash]);

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

  const selTargetSum = selected.reduce((s, it) => s + it.target, 0);
  const need = Math.max(0, calc.cmin);
  const loadedPlan = plans.find((p) => p.id === loadedId);

  return (
    <div className="flex flex-col gap-5">
      <div className="flex gap-1 border-b border-white/10">
        {([["run", "리밸런싱"], ["presets", "프리셋 관리"]] as const).map(([k, label]) => (
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

      {/* ── 리밸런싱 실행 ── */}
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
                  {p.name} ({p.entries.length})
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
              프리셋을 선택하면 그 목표 배분으로 리밸런싱을 계산합니다.
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
                <CommaInput decimal value={cash} onChange={setCash} className={cx.inputNumber + " w-32"} />
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
                  </tbody>
                </table>
              </div>

              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <StatTile size="base" label="목표 합계" value={Math.round(calc.sumT) + "%"} tone={Math.round(calc.sumT) === 100 ? "text-white" : "text-sky-300"} />
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

      {/* ── 프리셋 관리 ── */}
      {tab === "presets" && (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-indigo-500/30 bg-indigo-600/10 p-2.5">
            <select
              value=""
              onChange={(e) => {
                const p = plans.find((x) => x.id === Number(e.target.value));
                if (p) applyPlan(p);
              }}
              className={cx.select}
            >
              <option value="">불러와 수정…</option>
              {plans.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.entries.length})
                </option>
              ))}
            </select>
            <input value={planName} onChange={(e) => setPlanName(e.target.value)} placeholder="프리셋 이름" className={cx.input + " sm:w-44"} />
            <button onClick={savePlan} className={cx.btnPrimary}>
              {plans.find((p) => p.name === planName.trim()) ? "덮어쓰기" : "저장"}
            </button>
            {planName && plans.find((p) => p.name === planName.trim()) && (
              <button
                onClick={() => deletePlan(plans.find((p) => p.name === planName.trim())!.id)}
                className="text-xs text-slate-500 hover:text-rose-300 transition"
              >
                삭제
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="추가할 종목 검색 / 직접 입력" className={cx.input + " sm:w-56"} />
            <select value={newCur} onChange={(e) => setNewCur(e.target.value)} className={cx.select}>
              {["KRW", "USD", "JPY", "CNY", "EUR", "HKD"].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
            <button onClick={addItem} className={cx.btnSecondary}>＋ 직접 추가</button>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
          {/* 선택된 종목 = 프리셋 구성 */}
          <div>
            <div className="flex items-center justify-between mb-2 gap-2 h-7">
              <h3 className="text-sm font-medium text-slate-300">
                선택된 종목 <span className="text-slate-500">({selected.length})</span> · 목표합{" "}
                <span className={Math.round(selTargetSum) === 100 ? "text-white" : "text-sky-300"}>{Math.round(selTargetSum)}%</span>
              </h3>
              {selected.length > 0 && (
                <button onClick={clearAll} className="text-xs text-slate-400 hover:text-rose-300 transition">모두 제외</button>
              )}
            </div>
            <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-auto max-h-[60vh]">
              <table className={cx.table.root}>
                <thead className={cx.table.head + " sticky top-0 z-10"}>
                  <tr>
                    <th className={cx.table.th + " w-full"}>종목</th>
                    <th className={cx.table.th + " whitespace-nowrap"}>구분</th>
                    <th className={cx.table.th + " text-right whitespace-nowrap"}>평가액(원)</th>
                    <th className={cx.table.th + " text-right whitespace-nowrap"}>목표%</th>
                    <th className={cx.table.th + " text-right"}></th>
                  </tr>
                </thead>
                <tbody className={cx.table.body}>
                  {selected.map((it) => (
                    <tr key={it.id} className={cx.table.tr}>
                      <td className={cx.table.td + " text-white font-medium"}>{it.symbol}</td>
                      <td className={cx.table.td + " whitespace-nowrap"}>
                        {it.isNew ? <span className="rounded-full bg-white/5 px-2 py-0.5 text-xs text-slate-300">신규</span> : it.sub}
                        <span className="ml-1 text-xs text-slate-500">{it.currency}</span>
                      </td>
                      <td className={cx.table.td + " text-right tabular-nums"}>{it.krwVal ? won(it.krwVal) : "—"}</td>
                      <td className={cx.table.td + " text-right"}>
                        <input
                          type="number"
                          value={it.target}
                          onChange={(e) => setTarget(it.id, parseFloat(e.target.value) || 0)}
                          className="w-16 bg-white/5 border border-white/15 rounded px-2 py-1 text-right text-sm text-white [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none"
                        />
                      </td>
                      <td className={cx.table.td + " text-right"}>
                        <button onClick={() => toggleInc(it.id, false)} className="text-xs text-slate-500 hover:text-rose-300 transition whitespace-nowrap">제외</button>
                      </td>
                    </tr>
                  ))}
                  {selected.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-4 py-6 text-center text-sm text-slate-500">
                        선택된 종목이 없습니다. 아래에서 추가하세요.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* 추가할 종목 */}
          <div>
            <div className="flex items-center justify-between mb-2 h-7">
              <h3 className="text-sm font-medium text-slate-300">
                추가할 종목 <span className="text-slate-500">({candidates.length})</span>
              </h3>
              <span className="text-xs text-slate-500">행을 눌러 추가</span>
            </div>
            <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-auto max-h-[60vh]">
              <table className={cx.table.root}>
                <thead className={cx.table.head + " sticky top-0 z-10"}>
                  <tr>
                    <th className={cx.table.th + " w-full"}>종목</th>
                    <th className={cx.table.th + " whitespace-nowrap"}>구분</th>
                    <th className={cx.table.th + " text-right whitespace-nowrap"}>평가액(원)</th>
                    <th className={cx.table.th + " text-right"}></th>
                  </tr>
                </thead>
                <tbody className={cx.table.body}>
                  {candidates.map((it) => (
                    <tr key={it.id} onClick={() => toggleInc(it.id, true)} className={cx.table.tr + " cursor-pointer hover:bg-white/5"}>
                      <td className={cx.table.td + " text-white font-medium"}>{it.symbol}</td>
                      <td className={cx.table.td + " whitespace-nowrap"}>
                        {it.isNew ? <span className="rounded-full bg-white/5 px-2 py-0.5 text-xs text-slate-300">신규</span> : it.sub}
                        <span className="ml-1 text-xs text-slate-500">{it.currency}</span>
                      </td>
                      <td className={cx.table.td + " text-right tabular-nums"}>{it.krwVal ? won(it.krwVal) : "—"}</td>
                      <td className={cx.table.td + " text-right text-indigo-300 whitespace-nowrap"}>＋</td>
                    </tr>
                  ))}
                  {candidates.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-center text-sm text-slate-500">
                        추가할 종목이 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
          </div>

          <p className="text-xs text-slate-500">목표%를 정하고 이름을 붙여 저장하세요. 리밸런싱은 &lsquo;리밸런싱&rsquo; 탭에서 프리셋을 불러와 계산합니다.</p>
        </div>
      )}
    </div>
  );
}

