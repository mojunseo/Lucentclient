import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { InfoProvider, useInfo } from "./context/InfoContext";
import { TopBar, type UpdateNotice, type ViewId } from "./components/TopBar";
import { CodeDialog } from "./components/CodeDialog";
import { PlayView } from "./views/PlayView";
import { ModsView } from "./views/ModsView";
import { AccountsView } from "./views/AccountsView";
import { SettingsView } from "./views/SettingsView";
import { LogsView } from "./views/LogsView";
import { useTauriEvent } from "./lib/useTauriEvent";
import { updateLauncher } from "./lib/tauri";
import type { DeviceCode, Info, UpdateProgress } from "./types";

const MAX_LOG_LINES = 5000;
type LogLine = { line: string; error: boolean };

export function App() {
  return (
    <InfoProvider>
      <Shell />
    </InfoProvider>
  );
}

function Shell() {
  const { info, refresh } = useInfo();
  const [view, setView] = useState<ViewId>("play");
  const [update, setUpdate] = useState<UpdateNotice | null>(null);
  const [deviceCode, setDeviceCode] = useState<DeviceCode | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogStatus, setDialogStatus] = useState("로그인을 기다리는 중…");
  const [log, setLog] = useState<LogLine[]>([]);
  const logRef = useRef<HTMLPreElement>(null);
  const stickToBottomRef = useRef(true);
  const startedRef = useRef(false);

  // Startup sequence: refresh() once, pick the initial view the same way the original did,
  // then check for a launcher update (skipped on development builds).
  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    async function checkForUpdate(current: Info) {
      if (current.development) return;
      try {
        await updateLauncher();
      } catch (error) {
        // Installed in a way the launcher can't replace itself (e.g. .deb or .rpm): point to the download.
        const [version] = String(error).split("|");
        setUpdate({
          text: "새 버전이 있어요. ",
          link: {
            href: "https://puritymc.kr/client/",
            label: /^\d/.test(version) ? `런처 ${version} 받기` : "새 런처 받기",
          },
        });
        console.warn("update failed", error);
      }
    }

    (async () => {
      const current = await refresh();
      setView(current.accounts.some((a) => a.uuid === current.selected) ? "play" : "accounts");
      await checkForUpdate(current);
    })();
  }, [refresh]);

  useTauriEvent<DeviceCode>("auth-code", (payload) => {
    setDeviceCode(payload);
    setDialogStatus("로그인을 기다리는 중…");
    setDialogOpen(true);
  });
  useTauriEvent("auth-done", () => {
    setDialogOpen(false);
    void refresh();
  });
  useTauriEvent<string>("auth-error", (payload) => setDialogStatus(payload));
  useTauriEvent<UpdateProgress>("update-progress", ({ version, done, total }) => {
    setUpdate({
      text: total ? `런처 ${version} 받는 중 ${Math.floor((done / total) * 100)}%` : `런처 ${version} 받는 중`,
    });
  });
  useTauriEvent<LogLine>("game-log", (payload) => {
    const el = logRef.current;
    stickToBottomRef.current = !el || el.scrollTop + el.clientHeight >= el.scrollHeight - 8;
    setLog((prev) => {
      const next = [...prev, payload];
      return next.length > MAX_LOG_LINES ? next.slice(next.length - MAX_LOG_LINES) : next;
    });
  });

  useLayoutEffect(() => {
    const el = logRef.current;
    if (el && stickToBottomRef.current) el.scrollTop = el.scrollHeight;
  }, [log]);

  const clearLog = useCallback(() => setLog([]), []);

  return (
    <div className="app">
      <TopBar
        view={view}
        onNavigate={setView}
        update={update}
        launcherVersionText={info ? `런처 ${info.launcherVersion}` : ""}
      />
      <main>
        <PlayView hidden={view !== "play"} clearLog={clearLog} />
        <ModsView hidden={view !== "mods"} />
        <AccountsView hidden={view !== "accounts"} />
        <SettingsView hidden={view !== "settings"} />
        <LogsView hidden={view !== "logs"} log={log} logRef={logRef} />
      </main>
      <CodeDialog open={dialogOpen} deviceCode={deviceCode} status={dialogStatus} onClose={() => setDialogOpen(false)} />
    </div>
  );
}
