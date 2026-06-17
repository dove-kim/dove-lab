"use client";

import { useState, useCallback, useEffect } from "react";
import Sidebar from "./Sidebar";
import ForcePasswordChangeModal from "./ForcePasswordChangeModal";
import { CapabilitiesProvider } from "@/states/capabilities";

interface Props {
  role: string;
  capabilities: string[];
  mustChangePassword?: boolean;
  children: React.ReactNode;
}

export default function ContentLayout({ role, capabilities, mustChangePassword = false, children }: Props) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeMobile = useCallback(() => setMobileOpen(false), []);

  useEffect(() => {
    const handler = () => setMobileOpen(true);
    window.addEventListener("sidebar:open", handler);
    return () => window.removeEventListener("sidebar:open", handler);
  }, []);

  return (
    <CapabilitiesProvider value={capabilities}>
      <div className="flex flex-1 overflow-hidden">
        {mustChangePassword && <ForcePasswordChangeModal />}
        {mobileOpen && (
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-20 lg:hidden"
            onClick={closeMobile}
          />
        )}

        <Sidebar role={role} capabilities={capabilities} mobileOpen={mobileOpen} onMobileClose={closeMobile} />

        <div className="flex flex-col flex-1 overflow-y-auto min-w-0 min-h-0">
          {children}
        </div>
      </div>
    </CapabilitiesProvider>
  );
}
