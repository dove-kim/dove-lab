"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useEffect } from "react";

interface Props {
  role: string;
  mobileOpen: boolean;
  onMobileClose: () => void;
}

interface NavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  {
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
  },
  {
    href: "/stocks",
    label: "종목",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
      </svg>
    ),
  },
  {
    href: "/filters",
    label: "필터 관리",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <line x1="4" y1="6" x2="20" y2="6" />
        <line x1="8" y1="12" x2="16" y2="12" />
        <line x1="11" y1="18" x2="13" y2="18" />
      </svg>
    ),
  },
];

const ADMIN_ITEMS: NavItem[] = [
  {
    href: "/admin/invite-codes",
    label: "초대 코드",
    icon: (
      <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="7" width="20" height="14" rx="2" />
        <path d="M16 3l-4 4-4-4" />
      </svg>
    ),
  },
];

export default function Sidebar({ role, mobileOpen, onMobileClose }: Props) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const isAdmin = role === "ADMIN";

  useEffect(() => {
    onMobileClose();
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <aside
      className={[
        "flex flex-col flex-shrink-0 border-r border-white/10 bg-slate-900/95 transition-all duration-200",
        // 모바일: fixed overlay, 데스크탑: relative (flex 흐름에 포함)
        "fixed lg:relative inset-y-0 left-0 z-30 h-full",
        // 모바일 열림/닫힘
        mobileOpen ? "translate-x-0 w-64" : "-translate-x-full",
        // 데스크탑은 항상 표시, 너비는 collapsed 상태에 따라
        collapsed ? "lg:translate-x-0 lg:w-14" : "lg:translate-x-0 lg:w-48",
      ].join(" ")}
    >
      {/* 상단 버튼 행 */}
      <div className="flex items-center border-b border-white/10 h-12 px-2 flex-shrink-0">
        {/* 모바일: 닫기 버튼 */}
        <button
          onClick={onMobileClose}
          className="lg:hidden flex items-center justify-center w-10 h-10 rounded-lg text-slate-400 hover:text-white hover:bg-white/8 transition cursor-pointer"
          title="메뉴 닫기"
        >
          <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>

        {/* 데스크탑: 접기/펼치기 버튼 */}
        <button
          onClick={() => setCollapsed((v) => !v)}
          className="hidden lg:flex items-center justify-center w-9 h-9 rounded-lg text-slate-400 hover:text-white hover:bg-white/8 transition cursor-pointer"
          title={collapsed ? "메뉴 펼치기" : "메뉴 접기"}
        >
          {collapsed ? (
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <path d="M9 3v18" />
              <path d="m14 9 3 3-3 3" />
            </svg>
          ) : (
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <path d="M9 3v18" />
              <path d="m16 15-3-3 3-3" />
            </svg>
          )}
        </button>
      </div>

      {/* 메뉴 */}
      <nav className="flex flex-col flex-1 py-2 gap-0.5 px-1.5 overflow-y-auto">
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.href} item={item} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
        ))}

        {isAdmin && (
          <>
            <div className="my-2 border-t border-white/10 mx-1" />
            {ADMIN_ITEMS.map((item) => (
              <NavLink key={item.href} item={item} pathname={pathname} collapsed={collapsed} mobileOpen={mobileOpen} />
            ))}
          </>
        )}
      </nav>
    </aside>
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
