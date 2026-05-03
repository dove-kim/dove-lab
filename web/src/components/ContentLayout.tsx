"use client";

import { useState, useCallback, useEffect } from "react";
import Sidebar from "./Sidebar";

interface Props {
  role: string;
  children: React.ReactNode;
}

export default function ContentLayout({ role, children }: Props) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeMobile = useCallback(() => setMobileOpen(false), []);

  useEffect(() => {
    const handler = () => setMobileOpen(true);
    window.addEventListener("sidebar:open", handler);
    return () => window.removeEventListener("sidebar:open", handler);
  }, []);

  return (
    <div className="flex flex-1 overflow-hidden">
      {mobileOpen && (
        <div
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-20 lg:hidden"
          onClick={closeMobile}
        />
      )}

      <Sidebar role={role} mobileOpen={mobileOpen} onMobileClose={closeMobile} />

      <div className="flex flex-col flex-1 overflow-hidden min-w-0">
        {children}
      </div>
    </div>
  );
}
