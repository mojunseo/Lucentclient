import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { getInfo } from "../lib/tauri";
import type { Info } from "../types";

type InfoContextValue = {
  info: Info | undefined;
  refresh: () => Promise<Info>;
  /** Optimistic local patch, used only by settings (no refresh() call). */
  patchSettings: (change: Partial<Info["settings"]>) => void;
};

const InfoContext = createContext<InfoContextValue | null>(null);

export function InfoProvider({ children }: { children: ReactNode }) {
  const [info, setInfo] = useState<Info | undefined>(undefined);

  const refresh = useCallback(async () => {
    const next = await getInfo();
    setInfo(next);
    return next;
  }, []);

  const patchSettings = useCallback((change: Partial<Info["settings"]>) => {
    setInfo((prev) => (prev ? { ...prev, settings: { ...prev.settings, ...change } } : prev));
  }, []);

  return <InfoContext.Provider value={{ info, refresh, patchSettings }}>{children}</InfoContext.Provider>;
}

export function useInfo() {
  const value = useContext(InfoContext);
  if (!value) throw new Error("useInfo must be used within InfoProvider");
  return value;
}
