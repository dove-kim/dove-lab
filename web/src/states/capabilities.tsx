"use client";

import { createContext, useContext } from "react";

const CapabilitiesContext = createContext<string[]>([]);

/** 클라이언트 트리에 현재 사용자의 capability 집합을 제공한다. */
export function CapabilitiesProvider({ value, children }: { value: string[]; children: React.ReactNode }) {
  return <CapabilitiesContext.Provider value={value}>{children}</CapabilitiesContext.Provider>;
}

export function useCapabilities(): string[] {
  return useContext(CapabilitiesContext);
}

export function useHasCapability(capability: string): boolean {
  return useContext(CapabilitiesContext).includes(capability);
}
