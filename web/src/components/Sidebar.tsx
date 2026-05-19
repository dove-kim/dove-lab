"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useEffect } from "react";
import type { UserMenu } from "@/types/user";

interface Props {
  role: string;
  menu: UserMenu;
  mobileOpen: boolean;
  onMobileClose: () => void;
}

interface NavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
}

interface FeatureGroup {
  featureCode: string;
  label: string;
  icon: React.ReactNode;
  subMenus: NavItem[];
}

const DASHBOARD_ITEM: NavItem = {
  href: "/",
  label: "대시보드",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </svg>
  ),
};

/** 기능별 메타 정보 (아이콘 + 레이블) */
const FEATURE_META: Record<string, { label: string; icon: React.ReactNode }> = {
  STOCK_SEARCH: {
    label: "주식 검색",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
      </svg>
    ),
  },
  STOCK_LEDGER: {
    label: "주식 장부",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="18" height="18" rx="2" />
        <line x1="3" y1="9" x2="21" y2="9" />
        <line x1="9" y1="21" x2="9" y2="9" />
      </svg>
    ),
  },
  BUDGET: {
    label: "가계부",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <line x1="12" y1="1" x2="12" y2="23" />
        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
      </svg>
    ),
  },
};

/** 기능별 하위 메뉴 코드 목록 (프론트 기준) */
const FEATURE_SUB_MENUS: Record<string, string[]> = {
  STOCK_SEARCH: ["STOCK_SEARCH_MAIN", "STOCK_SEARCH_FILTER", "STOCK_SEARCH_SETS"],
  STOCK_LEDGER: [],
  BUDGET: [],
};

/** 하위 메뉴 코드 → NavItem */
const SUB_MENU_NAV: Record<string, NavItem> = {
  STOCK_SEARCH_MAIN: {
    href: "/stock-search",
    label: "종목 검색",
    icon: (
      <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
    ),
  },
  STOCK_SEARCH_FILTER: {
    href: "/search-filters",
    label: "필터 관리",
    icon: (
      <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <line x1="4" y1="6" x2="20" y2="6" /><line x1="8" y1="12" x2="16" y2="12" /><line x1="11" y1="18" x2="13" y2="18" />
      </svg>
    ),
  },
  STOCK_SEARCH_SETS: {
    href: "/stock-sets",
    label: "종목 필터 관리",
    icon: (
      <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="18" height="18" rx="2" /><line x1="9" y1="9" x2="15" y2="9" /><line x1="9" y1="12" x2="15" y2="12" /><line x1="9" y1="15" x2="12" y2="15" />
      </svg>
    ),
  },
};

const SETTINGS_ITEM: NavItem = {
  href: "/settings/menu",
  label: "메뉴 설정",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  ),
};

const ADMIN_USER_ITEM: NavItem = {
  href: "/admin/users",
  label: "기능 권한 관리",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
};

const ROOT_USER_ITEM: NavItem = {
  href: "/root/users",
  label: "사용자 관리",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  ),
};

const ROOT_INVITE_ITEM: NavItem = {
  href: "/root/invite-codes",
  label: "초대 코드",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="7" width="20" height="14" rx="2" />
      <path d="M16 3l-4 4-4-4" />
    </svg>
  ),
};

const STORAGE_KEY = "sidebar-collapsed";

export default function Sidebar({ role, menu, mobileOpen, onMobileClose }: Props) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const isAdmin = role === "ADMIN" || role === "ROOT";
  const isRoot = role === "ROOT";

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored !== null) setCollapsed(stored === "true");
  }, []);

  useEffect(() => {
    onMobileClose();
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps

  function toggleCollapsed() {
    setCollapsed((v) => {
      const next = !v;
      localStorage.setItem(STORAGE_KEY, String(next));
      return next;
    });
  }

  // 2계층 기능 그룹 빌드 (모듈 무시, 기능 단위로 평탄화)
  const featureGroups: FeatureGroup[] = [];
  const sortedModules = [...menu.modules].sort((a, b) => a.displayOrder - b.displayOrder);
  for (const mod of sortedModules) {
    const sortedFeatures = [...mod.features].sort((a, b) => a.displayOrder - b.displayOrder);
    for (const feat of sortedFeatures) {
      if (feat.hidden) continue;
      const meta = FEATURE_META[feat.featureCode];
      if (!meta) continue;
      const subMenuItems = (FEATURE_SUB_MENUS[feat.featureCode] ?? [])
        .map((code) => SUB_MENU_NAV[code])
        .filter(Boolean);
      featureGroups.push({ featureCode: feat.featureCode, ...meta, subMenus: subMenuItems });
    }
  }

  const showLabel = mobileOpen || !collapsed;

  return (
    <aside
      className={[
        "flex flex-col flex-shrink-0 border-r border-white/10 bg-slate-900/95 transition-all duration-200",
        "fixed lg:relative inset-y-0 left-0 z-30 h-full",
        mobileOpen ? "translate-x-0 w-64" : "-translate-x-full",
        collapsed ? "lg:translate-x-0 lg:w-14" : "lg:translate-x-0 lg:w-48",
      ].join(" ")}
    >
      {/* 상단 버튼 행 */}
      <div className="flex items-center border-b border-white/10 h-12 px-2 flex-shrink-0">
        <button
          onClick={onMobileClose}
          className="lg:hidden flex items-center justify-center w-10 h-10 rounded-lg text-slate-400 hover:text-white hover:bg-white/8 transition cursor-pointer"
          title="메뉴 닫기"
        >
          <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
        <button
          onClick={toggleCollapsed}
          className="hidden lg:flex items-center justify-center w-9 h-9 rounded-lg text-slate-400 hover:text-white hover:bg-white/8 transition cursor-pointer"
          title={collapsed ? "메뉴 펼치기" : "메뉴 접기"}
        >
          {collapsed ? (
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18" /><path d="m14 9 3 3-3 3" />
            </svg>
          ) : (
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18" /><path d="m16 15-3-3 3-3" />
            </svg>
          )}
        </button>
      </div>

      {/* 메뉴 */}
      <nav className="flex flex-col flex-1 py-2 gap-0.5 px-1.5 overflow-y-auto">
        {isRoot ? (
          /* ROOT: 사용자 관리 → 초대 코드 → 기능 권한 관리 */
          <>
            <NavLink item={ROOT_USER_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
            <NavLink item={ROOT_INVITE_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
            <NavLink item={ADMIN_USER_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
          </>
        ) : (
          /* USER / ADMIN: 대시보드 → 기능 그룹 → (ADMIN) 기능 권한 관리 */
          <>
            <NavLink item={DASHBOARD_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />

            {featureGroups.map((group) =>
              group.subMenus.length === 0 ? (
                // 하위 메뉴 없음 → 기능 자체가 직접 링크
                <NavLink
                  key={group.featureCode}
                  item={{ href: "#", label: group.label, icon: group.icon }}
                  pathname={pathname}
                  collapsed={collapsed}
                  mobileOpen={mobileOpen}
                />
              ) : (
                // 하위 메뉴 있음 → 섹션 헤더 + 하위 메뉴
                <FeatureSection
                  key={group.featureCode}
                  group={group}
                  pathname={pathname}
                  collapsed={collapsed}
                  showLabel={showLabel}
                />
              )
            )}

            {isAdmin && (
              <>
                <div className="my-2 border-t border-white/10 mx-1" />
                <NavLink item={ADMIN_USER_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
              </>
            )}
          </>
        )}

        <div className="flex-1" />

        {/* 메뉴 설정 — 하단 고정 (ROOT 제외) */}
        {!isRoot && (
          <div className="border-t border-white/10 mx-1 mt-1 pt-1">
            <NavLink item={SETTINGS_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
          </div>
        )}
      </nav>
    </aside>
  );
}

function FeatureSection({
  group,
  pathname,
  collapsed,
  showLabel,
}: {
  group: FeatureGroup;
  pathname: string;
  collapsed: boolean;
  showLabel: boolean;
}) {
  const isAnyActive = group.subMenus.some((s) => pathname === s.href);

  if (collapsed && !showLabel) {
    // 접힌 상태: 첫 번째 하위 메뉴 아이콘만 표시
    return (
      <>
        {group.subMenus.map((sub) => (
          <NavLink key={sub.href} item={sub} pathname={pathname} collapsed={collapsed} mobileOpen={false} />
        ))}
      </>
    );
  }

  return (
    <div>
      {/* 기능 섹션 헤더 */}
      <div
        className={`flex items-center gap-3 px-2.5 py-2 rounded-lg text-xs font-semibold uppercase tracking-wide ${
          isAnyActive ? "text-indigo-300" : "text-slate-500"
        }`}
      >
        {group.icon}
        {showLabel && <span className="truncate">{group.label}</span>}
      </div>
      {/* 하위 메뉴 */}
      <div className="ml-2 pl-3 border-l border-white/10 flex flex-col gap-0.5">
        {group.subMenus.map((sub) => (
          <Link
            key={sub.href}
            href={sub.href}
            className={`flex items-center gap-2.5 px-2 py-2 rounded-lg text-sm transition cursor-pointer ${
              pathname === sub.href
                ? "bg-indigo-600/25 text-indigo-300 border border-indigo-500/30"
                : "text-slate-400 hover:text-white hover:bg-white/5"
            }`}
          >
            {sub.icon}
            {showLabel && <span className="truncate">{sub.label}</span>}
          </Link>
        ))}
      </div>
    </div>
  );
}

function NavLink({
  item,
  pathname,
  collapsed,
  mobileOpen,
}: {
  item: NavItem;
  pathname: string;
  collapsed: boolean;
  mobileOpen: boolean;
}) {
  const isActive = pathname === item.href;
  const showLabel = mobileOpen || !collapsed;
  return (
    <Link
      href={item.href}
      title={!showLabel ? item.label : undefined}
      className={`flex items-center gap-3 px-2.5 py-2.5 rounded-lg text-sm transition cursor-pointer ${
        isActive
          ? "bg-indigo-600/25 text-indigo-300 border border-indigo-500/30"
          : "text-slate-400 hover:text-white hover:bg-white/5"
      }`}
    >
      {item.icon}
      {showLabel && <span className="truncate">{item.label}</span>}
    </Link>
  );
}
