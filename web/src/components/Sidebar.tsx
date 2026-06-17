"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useEffect } from "react";
import { hasCapability } from "@/utils/capability";

interface Props {
  role: string;
  capabilities: string[];
  mobileOpen: boolean;
  onMobileClose: () => void;
}

interface NavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
}

/** capability로 게이트되는 메뉴 항목. capability 없으면 항상 접근 가능. */
interface MenuItem extends NavItem {
  capability?: string;
  /** 권한 없을 때 정책: LOCK=잠금 표시, HIDE=숨김. 기본 LOCK. */
  deny?: "LOCK" | "HIDE";
}

interface MenuGroup {
  key: string;
  label: string;
  icon: React.ReactNode;
  items: MenuItem[];
}

interface RenderedItem extends MenuItem {
  accessible: boolean;
  locked: boolean;
}

interface RenderedGroup {
  key: string;
  label: string;
  icon: React.ReactNode;
  items: RenderedItem[];
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

const SEARCH_ICON = (
  <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

const FILTER_ICON = (
  <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <line x1="4" y1="6" x2="20" y2="6" /><line x1="8" y1="12" x2="16" y2="12" /><line x1="11" y1="18" x2="13" y2="18" />
  </svg>
);

const GRID_ICON = (
  <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="18" height="18" rx="2" /><line x1="9" y1="9" x2="15" y2="9" /><line x1="9" y1="12" x2="15" y2="12" /><line x1="9" y1="15" x2="12" y2="15" />
  </svg>
);

const STOCK_GROUP_ICON = (
  <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
  </svg>
);

const LOCK_ICON = (
  <svg className="w-3.5 h-3.5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="11" width="18" height="11" rx="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);

/** 메뉴 매니페스트(프론트 소유). 노출/잠금은 capability로 결정한다. */
const MENU_GROUPS: MenuGroup[] = [
  {
    key: "STOCK",
    label: "주식",
    icon: STOCK_GROUP_ICON,
    items: [
      { href: "/stock-search", label: "종목 조회", icon: SEARCH_ICON, capability: "STOCK_VIEW", deny: "LOCK" },
      { href: "/search-filters", label: "필터 관리", icon: FILTER_ICON, capability: "STOCK_SEARCH", deny: "LOCK" },
      { href: "/stock-filters", label: "종목 필터", icon: GRID_ICON, capability: "STOCK_SEARCH", deny: "LOCK" },
    ],
  },
];

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

const ROOT_BACKFILL_ITEM: NavItem = {
  href: "/root/ops/backfill",
  label: "데이터 재조회",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="1 4 1 10 7 10" />
      <path d="M3.51 15a9 9 0 1 0 .49-4.37" />
    </svg>
  ),
};

const ROOT_SYSTEM_EVENTS_ITEM: NavItem = {
  href: "/root/ops/system-events",
  label: "시스템 이벤트",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
  ),
};

const ROOT_STOCK_TAGS_ITEM: NavItem = {
  href: "/root/ops/stock-tags",
  label: "분류 표시명",
  icon: (
    <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
      <line x1="7" y1="7" x2="7.01" y2="7" />
    </svg>
  ),
};

const STORAGE_KEY = "sidebar-collapsed";

export default function Sidebar({ role, capabilities, mobileOpen, onMobileClose }: Props) {
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

  // 매니페스트 → capability 기준으로 접근/잠금/숨김 계산. HIDE는 제외, LOCK은 잠금 표시.
  const groups: RenderedGroup[] = MENU_GROUPS.map((group) => {
    const items: RenderedItem[] = group.items
      .map((item) => {
        const accessible = !item.capability || hasCapability(capabilities, item.capability);
        return { ...item, accessible, locked: !accessible && item.deny !== "HIDE" };
      })
      .filter((item) => item.accessible || item.deny !== "HIDE");
    return { ...group, items };
  }).filter((group) => group.items.length > 0);

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
        <>
          <NavLink item={DASHBOARD_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />

          {groups.map((group) => (
            <MenuSection
              key={group.key}
              group={group}
              pathname={pathname}
              collapsed={collapsed}
              showLabel={showLabel}
            />
          ))}

          {isAdmin && (
            <>
              <div className="my-2 border-t border-white/10 mx-1" />
              <NavLink item={ADMIN_USER_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
            </>
          )}

          {isRoot && (
            <>
              <div className="my-2 border-t border-white/10 mx-1" />
              <NavLink item={ROOT_USER_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
              <NavLink item={ROOT_INVITE_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
              <div className="my-2 border-t border-white/10 mx-1" />
              {showLabel && (
                <div className="px-2.5 py-1 text-xs font-semibold uppercase tracking-wide text-slate-500">
                  운영
                </div>
              )}
              <NavLink item={ROOT_BACKFILL_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
              <NavLink item={ROOT_STOCK_TAGS_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
              <NavLink item={ROOT_SYSTEM_EVENTS_ITEM} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
            </>
          )}
        </>

        <div className="flex-1" />
      </nav>
    </aside>
  );
}

function MenuSection({
  group,
  pathname,
  collapsed,
  showLabel,
}: {
  group: RenderedGroup;
  pathname: string;
  collapsed: boolean;
  showLabel: boolean;
}) {
  const isAnyActive = group.items.some((s) => pathname === s.href);

  if (collapsed && !showLabel) {
    // 접힌 상태: 하위 항목 아이콘만 표시
    return (
      <>
        {group.items.map((sub) =>
          sub.locked ? (
            <LockedRow key={sub.href} item={sub} collapsed showLabel={false} />
          ) : (
            <NavLink key={sub.href} item={sub} pathname={pathname} collapsed={collapsed} mobileOpen={false} />
          )
        )}
      </>
    );
  }

  return (
    <div>
      {/* 그룹 헤더 */}
      <div
        className={`flex items-center gap-3 px-2.5 py-2 rounded-lg text-xs font-semibold uppercase tracking-wide ${
          isAnyActive ? "text-indigo-300" : "text-slate-500"
        }`}
      >
        {group.icon}
        {showLabel && <span className="truncate">{group.label}</span>}
      </div>
      {/* 하위 항목 */}
      <div className="ml-2 pl-3 border-l border-white/10 flex flex-col gap-0.5">
        {group.items.map((sub) =>
          sub.locked ? (
            <LockedRow key={sub.href} item={sub} collapsed={false} showLabel={showLabel} />
          ) : (
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
          )
        )}
      </div>
    </div>
  );
}

function LockedRow({
  item,
  collapsed,
  showLabel,
}: {
  item: RenderedItem;
  collapsed: boolean;
  showLabel: boolean;
}) {
  return (
    <div
      title="권한이 없습니다"
      className={`flex items-center gap-2.5 ${collapsed ? "px-2.5 py-2.5" : "px-2 py-2"} rounded-lg text-sm text-slate-600 cursor-not-allowed select-none`}
    >
      {item.icon}
      {showLabel && <span className="truncate flex-1">{item.label}</span>}
      {showLabel && LOCK_ICON}
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
