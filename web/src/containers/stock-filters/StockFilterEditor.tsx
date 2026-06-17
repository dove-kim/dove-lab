"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { cx } from "@/utils/cx";
import {
  FilterMode,
  NameMatchType,
  NamePatternCondition,
  StockCondition,
  StockFilterResponse,
  StockSummary,
  TagCondition,
} from "@/types/stock-filter";
import {
  StockTagFieldGroup,
  StockTagsResponse,
} from "@/types/stock-tag";

const PREVIEW_PAGE_SIZE = 100;

const BOOLEAN_OPTIONS: { value: string; label: string }[] = [
  { value: "Y", label: "예" },
  { value: "N", label: "아니오" },
];

interface Props {
  mode: "create" | "edit" | "view";
  scope: "system" | "personal";
  initial?: StockFilterResponse;
  onSaved: (saved: StockFilterResponse) => void;
  onClose: () => void;
  canDelete?: boolean;
  onDeleted?: (id: number) => void;
}

interface StockListItem {
  marketType: string;
  code: string;
  name: string;
}

/** 한 종목 식별자(시장:코드)를 묶는 키 */
function stockKey(marketType: string, code: string): string {
  return `${marketType}:${code}`;
}

/** 이름 패턴 칩에 표시할 문구 (예: "리츠"로 끝). */
function namePhrase(pattern: string, matchType: NameMatchType): string {
  if (matchType === "STARTS_WITH") return `"${pattern}"로 시작`;
  if (matchType === "ENDS_WITH") return `"${pattern}"로 끝`;
  return `"${pattern}" 포함`;
}

/** field 그룹의 첫 선택 값(원문). BOOLEAN은 "Y", 값 없으면 빈 문자열. */
function firstValueOf(group: StockTagFieldGroup): string {
  if (group.type === "BOOLEAN") return "Y";
  return group.values.length > 0 ? group.values[0].value : "";
}

export default function StockFilterEditor({
  mode,
  scope,
  initial,
  onSaved,
  onClose,
  canDelete,
  onDeleted,
}: Props) {
  const readonly = mode === "view";

  // 기본 정보
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [enabled, setEnabled] = useState(initial?.enabled ?? true);

  // 시장 — 시스템 지원 시장 (API 로 동적 조회) + 사용자 선택
  const [availableMarkets, setAvailableMarkets] = useState<string[]>([]);
  const [selectedMarkets, setSelectedMarkets] = useState<string[]>([]);

  // 분류 메타 (동적 로드)
  const [tags, setTags] = useState<StockTagsResponse | null>(null);

  // 태그 조건
  const [tagConds, setTagConds] = useState<TagCondition[]>(initial?.tagConditions ?? []);
  const [tagDraftField, setTagDraftField] = useState<string>("");
  const [tagDraftValue, setTagDraftValue] = useState<string>("");
  const [tagDraftMode, setTagDraftMode] = useState<FilterMode>("INCLUDE");

  // 이름 패턴 조건 (예: "스팩" 제외)
  const [nameConds, setNameConds] = useState<NamePatternCondition[]>(initial?.namePatternConditions ?? []);
  const [nameDraftPattern, setNameDraftPattern] = useState<string>("");
  const [nameDraftMode, setNameDraftMode] = useState<FilterMode>("EXCLUDE");
  const [nameDraftMatchType, setNameDraftMatchType] = useState<NameMatchType>("CONTAINS");

  // 종목 조건 (EXCLUDE = Zone②, INCLUDE = Zone③)
  const initialExcludes = (initial?.stockConditions ?? []).filter((c) => c.mode === "EXCLUDE");
  const initialIncludes = (initial?.stockConditions ?? []).filter((c) => c.mode === "INCLUDE");
  const [excludes, setExcludes] = useState<StockCondition[]>(initialExcludes);
  const [includes, setIncludes] = useState<StockCondition[]>(initialIncludes);

  // 미리보기 결과 (태그 적용 후)
  const [tagPreview, setTagPreview] = useState<StockSummary[]>([]);
  const [previewLoading, setPreviewLoading] = useState(false);

  // 전체 종목 목록 (Zone③ 검색용)
  const [allStocks, setAllStocks] = useState<StockListItem[]>([]);
  const [excludeQuery, setExcludeQuery] = useState("");
  const [includeQuery, setIncludeQuery] = useState("");
  const [previewSort, setPreviewSort] = useState<"name" | "code">("name");
  const [previewPage, setPreviewPage] = useState(0);

  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // 시스템 지원 시장 목록 한 번만 로드
  useEffect(() => {
    fetch("/api/market/available")
      .then((res) => res.ok ? res.json() : [])
      .then((data: unknown) => {
        if (Array.isArray(data) && data.every((x) => typeof x === "string")) {
          const markets = data as string[];
          setAvailableMarkets(markets);
          setSelectedMarkets(markets); // 기본 전체 선택
        }
      })
      .catch(() => setAvailableMarkets([]));
  }, []);

  // 분류 메타(태그 차원·값·수치 필드) 한 번만 로드
  useEffect(() => {
    fetch("/api/stock-tags")
      .then((res) => (res.ok ? res.json() : null))
      .then((data: StockTagsResponse | null) => {
        if (!data) return;
        setTags(data);
        if (data.tagFields.length > 0) {
          const first = data.tagFields[0];
          setTagDraftField(first.field);
          setTagDraftValue(firstValueOf(first));
        }
      })
      .catch(() => setTags(null));
  }, []);

  // 전체 종목 목록 한 번만 로드
  useEffect(() => {
    fetch("/api/stocks")
      .then((res) => res.ok ? res.json() : [])
      .then((data: unknown) => {
        if (Array.isArray(data)) {
          setAllStocks(
            data.map((s) => ({
              marketType: (s as { market: string }).market,
              code: (s as { ticker: string }).ticker,
              name: (s as { name: string }).name,
            }))
          );
        }
      })
      .catch(() => setAllStocks([]));
  }, []);

  // 태그 조건 변경 시 debounce 후 미리보기 호출
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPreviewLoading(true);
      fetch("/api/stock-filters/preview/tag", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tagConditions: tagConds,
          numericConditions: [],
          namePatternConditions: nameConds,
          markets: selectedMarkets.length > 0 ? selectedMarkets : null,
        }),
      })
        .then((res) => res.ok ? res.json() : [])
        .then((data: unknown) => {
          if (Array.isArray(data)) setTagPreview(data as StockSummary[]);
          else setTagPreview([]);
        })
        .catch(() => setTagPreview([]))
        .finally(() => setPreviewLoading(false));
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [tagConds, nameConds, selectedMarkets]);

  // 파생 — 최종 카운트
  const excludeKeys = useMemo(
    () => new Set(excludes.map((c) => stockKey(c.marketType, c.stockCode))),
    [excludes]
  );
  const includeKeys = useMemo(
    () => new Set(includes.map((c) => stockKey(c.marketType, c.stockCode))),
    [includes]
  );

  const finalCount = useMemo(() => {
    const set = new Set<string>();
    for (const s of tagPreview) {
      const k = stockKey(s.marketType, s.code);
      if (!excludeKeys.has(k)) set.add(k);
    }
    for (const k of includeKeys) set.add(k);
    return set.size;
  }, [tagPreview, excludeKeys, includeKeys]);

  // 태그 결과에서 EXCLUDE 안 된 것만 좌패널에 표시
  const tagPreviewAvailable = useMemo(
    () => tagPreview.filter((s) => !excludeKeys.has(stockKey(s.marketType, s.code))),
    [tagPreview, excludeKeys]
  );

  const filteredTagPreview = useMemo(() => {
    const q = excludeQuery.trim().toLowerCase();
    const base = q
      ? tagPreviewAvailable.filter((s) => s.name.toLowerCase().includes(q) || s.code.includes(q))
      : tagPreviewAvailable;
    return [...base].sort((a, b) =>
      previewSort === "name" ? a.name.localeCompare(b.name, "ko") : a.code.localeCompare(b.code)
    );
  }, [tagPreviewAvailable, excludeQuery, previewSort]);

  // 조건·검색·정렬이 바뀌면 첫 페이지로
  useEffect(() => { setPreviewPage(0); }, [excludeQuery, previewSort, tagPreviewAvailable]);
  const previewTotalPages = Math.max(1, Math.ceil(filteredTagPreview.length / PREVIEW_PAGE_SIZE));
  const previewSafePage = Math.min(previewPage, previewTotalPages - 1);
  const pagedPreview = filteredTagPreview.slice(
    previewSafePage * PREVIEW_PAGE_SIZE, (previewSafePage + 1) * PREVIEW_PAGE_SIZE);

  // Zone③ — 태그와 무관 추가 검색
  const filteredAddCandidates = useMemo(() => {
    const q = includeQuery.trim().toLowerCase();
    if (!q) return [];
    return allStocks
      .filter((s) => {
        const k = stockKey(s.marketType, s.code);
        if (includeKeys.has(k)) return false;
        return s.name.toLowerCase().includes(q) || s.code.includes(q);
      })
      .slice(0, 50);
  }, [allStocks, includeQuery, includeKeys]);

  // ── 핸들러 ─────────────────────────────────────────────────────────────
  function fieldGroupOf(field: string): StockTagFieldGroup | undefined {
    return tags?.tagFields.find((f) => f.field === field);
  }

  function tagValueOptions(field: string): { value: string; label: string }[] {
    const group = fieldGroupOf(field);
    if (!group) return [];
    if (group.type === "BOOLEAN") return BOOLEAN_OPTIONS;
    return group.values.map((v) => ({ value: v.value, label: v.label }));
  }

  function handleTagFieldChange(field: string) {
    setTagDraftField(field);
    const opts = tagValueOptions(field);
    setTagDraftValue(opts.length > 0 ? opts[0].value : "");
  }

  function handleAddTag() {
    if (readonly || !tagDraftField || !tagDraftValue) return;
    const exists = tagConds.find(
      (c) => c.field === tagDraftField && c.value === tagDraftValue && c.mode === tagDraftMode
    );
    if (exists) return;
    setTagConds([...tagConds, { field: tagDraftField, value: tagDraftValue, mode: tagDraftMode }]);
  }

  function handleRemoveTag(idx: number) {
    if (readonly) return;
    setTagConds(tagConds.filter((_, i) => i !== idx));
  }

  function handleAddName() {
    if (readonly) return;
    const p = nameDraftPattern.trim();
    if (!p) return;
    if (nameConds.some((c) => c.pattern === p && c.mode === nameDraftMode && c.matchType === nameDraftMatchType)) return;
    setNameConds([...nameConds, { pattern: p, mode: nameDraftMode, matchType: nameDraftMatchType }]);
    setNameDraftPattern("");
  }

  function handleRemoveName(idx: number) {
    if (readonly) return;
    setNameConds(nameConds.filter((_, i) => i !== idx));
  }

  function handleExclude(s: StockSummary | StockListItem) {
    if (readonly) return;
    setExcludes([...excludes, { marketType: s.marketType, stockCode: s.code, mode: "EXCLUDE" }]);
  }

  function handleUnExclude(idx: number) {
    if (readonly) return;
    setExcludes(excludes.filter((_, i) => i !== idx));
  }

  function handleAddInclude(s: StockListItem) {
    if (readonly) return;
    setIncludes([...includes, { marketType: s.marketType, stockCode: s.code, mode: "INCLUDE" }]);
    setIncludeQuery("");
  }

  function handleRemoveInclude(idx: number) {
    if (readonly) return;
    setIncludes(includes.filter((_, i) => i !== idx));
  }

  function tagLabel(c: TagCondition): string {
    const group = fieldGroupOf(c.field);
    const fieldLabel = group?.label ?? c.field;
    const opts = tagValueOptions(c.field);
    const valLabel = opts.find((o) => o.value === c.value)?.label ?? c.value;
    const modeLabel = c.mode === "INCLUDE" ? "포함" : "제외";
    return `${modeLabel} · ${fieldLabel}: ${valLabel}`;
  }

  function nameOf(marketType: string, code: string): string {
    return allStocks.find((s) => s.marketType === marketType && s.code === code)?.name ?? code;
  }

  async function handleSave() {
    if (readonly) return;
    if (!name.trim()) {
      setError("이름을 입력해주세요");
      return;
    }
    setError(null);
    setSaving(true);

    const stockConditions: StockCondition[] = [...excludes, ...includes];
    const body = scope === "system"
      ? { name: name.trim(), description: description.trim() || null, tagConditions: tagConds, numericConditions: [], namePatternConditions: nameConds, stockConditions, enabled }
      : { name: name.trim(), description: description.trim() || null, tagConditions: tagConds, numericConditions: [], namePatternConditions: nameConds, stockConditions };

    try {
      let url: string;
      let method: "POST" | "PUT";
      if (mode === "create" && scope === "personal") {
        url = "/api/stock-filters/personal";
        method = "POST";
      } else if (mode === "create" && scope === "system") {
        url = "/api/admin/stock-filters/system";
        method = "POST";
      } else if (scope === "personal") {
        url = `/api/stock-filters/personal/${initial!.id}`;
        method = "PUT";
      } else {
        url = `/api/admin/stock-filters/system/${initial!.id}`;
        method = "PUT";
      }
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const json = await res.json().catch(() => null);
        const detail = (json as { detail?: string } | null)?.detail;
        setError(mapErrorCode(detail) ?? "저장에 실패했습니다");
        setSaving(false);
        return;
      }
      const saved = await res.json();

      // 시스템 + edit 모드에서 enabled 변경 분기 (PUT은 enabled를 안 받으므로)
      if (scope === "system" && mode === "edit" && initial && initial.enabled !== enabled) {
        const patchRes = await fetch(`/api/admin/stock-filters/system/${initial.id}/enabled`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ enabled }),
        });
        if (patchRes.ok) {
          onSaved(await patchRes.json());
          return;
        }
      }
      onSaved(saved);
    } catch {
      setError("네트워크 오류");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (readonly || !initial) return;
    if (!confirm("정말 삭제하시겠어요?")) return;
    const url = scope === "system"
      ? `/api/admin/stock-filters/system/${initial.id}`
      : `/api/stock-filters/personal/${initial.id}`;
    const res = await fetch(url, { method: "DELETE" });
    if (!res.ok && res.status !== 204) {
      setError("삭제에 실패했습니다");
      return;
    }
    if (onDeleted) onDeleted(initial.id);
  }

  const title = mode === "create" ? "새 필터" : mode === "edit" ? "필터 편집" : "필터 보기";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-6xl h-[90vh] flex flex-col bg-slate-800 border border-white/10 rounded-2xl shadow-2xl">
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/10">
          <h2 className="text-lg font-semibold text-white">
            {title}
            <span className="ml-3 text-xs text-slate-400">
              ({scope === "system" ? "시스템 필터" : "내 필터"})
            </span>
          </h2>
          <button
            onClick={onClose}
            className="w-9 h-9 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/10"
            title="닫기"
          >
            ✕
          </button>
        </div>

        {/* 본문 — 좌우 2패널 */}
        <div className="flex-1 min-h-0 grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-white/10">
          {/* 좌패널 — 설정 */}
          <div className="overflow-y-auto px-5 py-5 space-y-5">
            {/* 기본 정보 */}
            <section className="space-y-3">
              <h3 className="text-sm font-semibold text-slate-300">기본 정보</h3>
              <label className="text-xs text-slate-400 block">
                이름 *
                <input
                  className={cx.input + " mt-1"}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  disabled={readonly}
                  placeholder="필터 이름"
                />
              </label>
              <label className="text-xs text-slate-400 block">
                설명
                <input
                  className={cx.input + " mt-1"}
                  value={description ?? ""}
                  onChange={(e) => setDescription(e.target.value)}
                  disabled={readonly}
                  placeholder="간단한 설명 (선택)"
                />
              </label>
              {scope === "system" && (
                <label className="flex items-center gap-2 text-sm text-slate-300">
                  <input
                    type="checkbox"
                    checked={enabled}
                    onChange={(e) => setEnabled(e.target.checked)}
                    disabled={readonly}
                    className="w-4 h-4"
                  />
                  활성 (USER에게 노출)
                </label>
              )}
            </section>

            {/* 태그 조건 */}
            <section className="space-y-3">
              <h3 className="text-sm font-semibold text-slate-300">태그 조건</h3>
              {!readonly && (tags?.tagFields.length ?? 0) > 0 && (
                <div className="flex flex-wrap gap-2 items-end">
                  <select
                    className={cx.select + " min-w-[8rem]"}
                    value={tagDraftField}
                    onChange={(e) => handleTagFieldChange(e.target.value)}
                  >
                    {tags!.tagFields.map((f) => (
                      <option key={f.field} value={f.field}>{f.label}</option>
                    ))}
                  </select>
                  <select
                    className={cx.select + " min-w-[8rem]"}
                    value={tagDraftValue}
                    onChange={(e) => setTagDraftValue(e.target.value)}
                    disabled={tagValueOptions(tagDraftField).length === 0}
                  >
                    {tagValueOptions(tagDraftField).length === 0 ? (
                      <option value="">값 없음</option>
                    ) : (
                      tagValueOptions(tagDraftField).map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))
                    )}
                  </select>
                  <select
                    className={cx.select + " min-w-[6rem]"}
                    value={tagDraftMode}
                    onChange={(e) => setTagDraftMode(e.target.value as FilterMode)}
                  >
                    <option value="INCLUDE">포함</option>
                    <option value="EXCLUDE">제외</option>
                  </select>
                  <button onClick={handleAddTag} className={cx.btnPrimary}>+ 추가</button>
                </div>
              )}
              <div className="flex flex-wrap gap-2">
                {tagConds.length === 0 && (
                  <div className="text-xs text-slate-500">조건이 없으면 모든 종목이 통과합니다.</div>
                )}
                {tagConds.map((c, idx) => (
                  <span
                    key={idx}
                    className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs border ${
                      c.mode === "INCLUDE"
                        ? "bg-indigo-600/15 text-indigo-200 border-indigo-500/30"
                        : "bg-rose-600/15 text-rose-200 border-rose-500/30"
                    }`}
                  >
                    {tagLabel(c)}
                    {!readonly && (
                      <button onClick={() => handleRemoveTag(idx)} className="hover:text-white">×</button>
                    )}
                  </span>
                ))}
              </div>
            </section>

            {/* 이름 패턴 조건 */}
            <section className="space-y-3">
              <h3 className="text-sm font-semibold text-slate-300">이름 패턴 조건</h3>
              {!readonly && (
                <div className="flex flex-wrap gap-2 items-end">
                  <input
                    className={cx.input + " flex-1 min-w-40"}
                    placeholder="예: 스팩"
                    value={nameDraftPattern}
                    onChange={(e) => setNameDraftPattern(e.target.value)}
                    onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddName(); } }}
                  />
                  <select
                    className={cx.select}
                    value={nameDraftMatchType}
                    onChange={(e) => setNameDraftMatchType(e.target.value as NameMatchType)}
                  >
                    <option value="CONTAINS">포함</option>
                    <option value="STARTS_WITH">시작</option>
                    <option value="ENDS_WITH">끝</option>
                  </select>
                  <select
                    className={cx.select}
                    value={nameDraftMode}
                    onChange={(e) => setNameDraftMode(e.target.value as FilterMode)}
                  >
                    <option value="INCLUDE">포함(허용)</option>
                    <option value="EXCLUDE">제외</option>
                  </select>
                  <button onClick={handleAddName} className={cx.btnPrimary}>+ 추가</button>
                </div>
              )}
              <div className="flex flex-wrap gap-2">
                {nameConds.length === 0 && (
                  <div className="text-xs text-slate-500">종목명에 패턴이 포함된 종목을 포함/제외합니다 (예: &quot;스팩&quot; 제외).</div>
                )}
                {nameConds.map((c, idx) => (
                  <span
                    key={idx}
                    className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs border ${
                      c.mode === "INCLUDE"
                        ? "bg-indigo-600/15 text-indigo-200 border-indigo-500/30"
                        : "bg-rose-600/15 text-rose-200 border-rose-500/30"
                    }`}
                  >
                    {c.mode === "INCLUDE" ? "포함" : "제외"} · 이름 {namePhrase(c.pattern, c.matchType)}
                    {!readonly && (
                      <button onClick={() => handleRemoveName(idx)} className="hover:text-white">×</button>
                    )}
                  </span>
                ))}
              </div>
            </section>

            {/* 제외 종목 (EXCLUDE) */}
            <section className="space-y-2">
              <h3 className="text-sm font-semibold text-slate-300">
                제외 종목 <span className="text-xs text-slate-500">({excludes.length})</span>
              </h3>
              {excludes.length === 0 && (
                <div className="text-xs text-slate-500">우측 종목 리스트에서 [×] 클릭하여 제외</div>
              )}
              {excludes.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {excludes.map((c, idx) => (
                    <span
                      key={stockKey(c.marketType, c.stockCode)}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-xs bg-rose-600/15 text-rose-200 border border-rose-500/30"
                    >
                      − {nameOf(c.marketType, c.stockCode)}
                      <span className="text-rose-400/70">({c.stockCode})</span>
                      {!readonly && (
                        <button onClick={() => handleUnExclude(idx)} className="hover:text-white" title="되돌리기">×</button>
                      )}
                    </span>
                  ))}
                </div>
              )}
            </section>

            {/* 추가 종목 (INCLUDE) */}
            <section className="space-y-2">
              <h3 className="text-sm font-semibold text-slate-300">
                태그와 무관 추가 종목 <span className="text-xs text-slate-500">({includes.length})</span>
              </h3>
              {!readonly && (
                <input
                  className={cx.input}
                  placeholder="종목명/코드 검색하여 추가"
                  value={includeQuery}
                  onChange={(e) => setIncludeQuery(e.target.value)}
                />
              )}
              {filteredAddCandidates.length > 0 && (
                <div className="bg-white/5 border border-white/10 rounded-lg max-h-40 overflow-y-auto">
                  {filteredAddCandidates.map((s) => (
                    <div
                      key={stockKey(s.marketType, s.code)}
                      className="flex items-center justify-between px-2 py-1.5 hover:bg-white/5 text-sm"
                    >
                      <span className="text-slate-300 truncate">
                        <span className="text-slate-500 text-xs mr-2">{s.marketType}</span>
                        {s.name} <span className="text-slate-500">({s.code})</span>
                      </span>
                      <button
                        onClick={() => handleAddInclude(s)}
                        className="text-xs text-indigo-300 hover:text-indigo-100 px-2 py-0.5 rounded border border-indigo-500/30"
                      >
                        + 추가
                      </button>
                    </div>
                  ))}
                </div>
              )}
              {includes.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {includes.map((c, idx) => (
                    <span
                      key={stockKey(c.marketType, c.stockCode)}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-xs bg-emerald-600/15 text-emerald-200 border border-emerald-500/30"
                    >
                      + {nameOf(c.marketType, c.stockCode)}
                      <span className="text-emerald-400/70">({c.stockCode})</span>
                      {!readonly && (
                        <button onClick={() => handleRemoveInclude(idx)} className="hover:text-white">×</button>
                      )}
                    </span>
                  ))}
                </div>
              )}
            </section>

            {/* 최종 미리보기 */}
            <section className="bg-white/5 border border-white/10 rounded-lg px-4 py-3 text-sm text-slate-300">
              최종 결과: <span className="text-white font-semibold">{finalCount.toLocaleString()}</span> 종목
              <div className="text-xs text-slate-500 mt-1">
                태그 통과 {tagPreview.length.toLocaleString()} − 제외 {excludes.length} + 추가 {includes.length}
              </div>
            </section>

            {error && <div className="text-sm text-rose-300">{error}</div>}
          </div>

          {/* 우패널 — 태그 결과 종목 미리보기 */}
          <div className="flex flex-col min-h-0 bg-slate-900/30">
            <div className="px-5 py-3 border-b border-white/10 space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-300">태그 결과 종목</h3>
                <span className="text-xs text-slate-400">
                  {previewLoading ? "..." : `${tagPreview.length.toLocaleString()}건`}
                </span>
              </div>
              {/* 시장 토글 (백엔드 설정으로부터 동적 로드) */}
              {availableMarkets.length > 0 && (
                <div className="flex gap-1.5 flex-wrap">
                  {availableMarkets.map((m) => {
                    const active = selectedMarkets.includes(m);
                    return (
                      <button
                        key={m}
                        type="button"
                        onClick={() => {
                          if (active && selectedMarkets.length === 1) return; // 최소 1개 유지
                          setSelectedMarkets(active
                            ? selectedMarkets.filter((x) => x !== m)
                            : [...selectedMarkets, m]);
                        }}
                        className={`px-3 py-1 rounded text-xs transition ${
                          active
                            ? "bg-indigo-600 text-white"
                            : "bg-white/5 text-slate-400 border border-white/10 hover:text-white"
                        }`}
                      >
                        {m}
                      </button>
                    );
                  })}
                </div>
              )}
              <div className="flex items-center gap-2">
                <input
                  className={cx.input + " flex-1"}
                  placeholder="종목명/코드 검색"
                  value={excludeQuery}
                  onChange={(e) => setExcludeQuery(e.target.value)}
                  disabled={readonly}
                />
                <button
                  type="button"
                  onClick={() => setPreviewSort((v) => (v === "name" ? "code" : "name"))}
                  className={cx.btnToggleOff + " flex-shrink-0 whitespace-nowrap"}
                  title="정렬 전환"
                >
                  {previewSort === "name" ? "이름순" : "코드순"}
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-y-auto px-3 py-2">
              {tagConds.length === 0 && tagPreview.length === 0 && !previewLoading && (
                <div className="text-xs text-slate-500 py-12 text-center">
                  좌측에서 태그 조건을 추가하세요
                </div>
              )}
              {tagConds.length > 0 && filteredTagPreview.length === 0 && !previewLoading && (
                <div className="text-xs text-slate-500 py-12 text-center">
                  해당하는 종목이 없습니다
                </div>
              )}
              {pagedPreview.map((s) => (
                <div
                  key={stockKey(s.marketType, s.code)}
                  className="flex items-center justify-between px-2 py-1.5 hover:bg-white/5 rounded text-sm"
                >
                  <span className="text-slate-300 truncate">
                    <span className="text-slate-500 text-xs mr-2">{s.marketType}</span>
                    {s.name} <span className="text-slate-500">({s.code})</span>
                  </span>
                  {!readonly && (
                    <button
                      onClick={() => handleExclude(s)}
                      className="text-xs text-rose-300 hover:text-rose-100 px-2 py-0.5 rounded border border-rose-500/30 flex-shrink-0"
                      title="제외"
                    >
                      제외
                    </button>
                  )}
                </div>
              ))}
            </div>
            {filteredTagPreview.length > PREVIEW_PAGE_SIZE && (
              <div className="flex items-center justify-between gap-2 px-3 py-2 border-t border-white/10 text-xs text-slate-400">
                <button type="button" disabled={previewSafePage === 0}
                  onClick={() => setPreviewPage((p) => Math.max(0, p - 1))}
                  className={cx.btnToggleOff + " disabled:opacity-30 disabled:cursor-not-allowed"}>이전</button>
                <span>
                  {(previewSafePage * PREVIEW_PAGE_SIZE + 1).toLocaleString()}–{Math.min((previewSafePage + 1) * PREVIEW_PAGE_SIZE, filteredTagPreview.length).toLocaleString()} / {filteredTagPreview.length.toLocaleString()}건
                </span>
                <button type="button" disabled={previewSafePage >= previewTotalPages - 1}
                  onClick={() => setPreviewPage((p) => Math.min(previewTotalPages - 1, p + 1))}
                  className={cx.btnToggleOff + " disabled:opacity-30 disabled:cursor-not-allowed"}>다음</button>
              </div>
            )}
          </div>
        </div>

        {/* 푸터 */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-white/10 sticky bottom-0 bg-slate-800">
          <div>
            {!readonly && canDelete && initial && (
              <button
                onClick={handleDelete}
                className="px-4 py-2 rounded-lg text-sm text-rose-300 border border-rose-500/40 hover:bg-rose-500/10"
              >
                삭제
              </button>
            )}
          </div>
          <div className="flex gap-2">
            {readonly ? (
              <button onClick={onClose} className={cx.btnSecondary}>닫기</button>
            ) : (
              <>
                <button onClick={onClose} className={cx.btnSecondary}>취소</button>
                <button onClick={handleSave} disabled={saving} className={cx.btnPrimary}>
                  {saving ? "저장 중..." : "저장"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function mapErrorCode(detail: string | undefined): string | null {
  if (!detail) return null;
  switch (detail) {
    case "DUPLICATE_STOCK_FILTER_NAME": return "같은 이름의 필터가 이미 존재합니다";
    case "STOCK_FILTER_NOT_FOUND": return "필터를 찾을 수 없습니다";
    default: return detail;
  }
}
