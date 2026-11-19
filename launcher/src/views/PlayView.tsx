import { useEffect, useMemo, useRef, useState, type ChangeEvent } from "react";
import { useInfo } from "../context/InfoContext";
import { useTauriEvent } from "../lib/useTauriEvent";
import { listVersions, play, setVersion } from "../lib/tauri";
import type { Progress } from "../types";

const STAGES: Record<Progress["stage"], { label: string; from: number; to: number }> = {
  metadata: { label: "버전 정보 확인", from: 0, to: 0.03 },
  libraries: { label: "라이브러리", from: 0.03, to: 0.2 },
  assets: { label: "에셋", from: 0.2, to: 0.7 },
  java: { label: "Java", from: 0.7, to: 0.95 },
  mods: { label: "모드", from: 0.95, to: 1 },
};

const PLAY_LABEL = { idle: "플레이", preparing: "준비 중", running: "실행 중" } as const;

type PlayingState = "idle" | "preparing" | "running";

export function PlayView({ hidden, clearLog }: { hidden: boolean; clearLog: () => void }) {
  const { info, refresh } = useInfo();
  const [playing, setPlaying] = useState<PlayingState>("idle");
  const [progressPct, setProgressPct] = useState(0);
  const [progressText, setProgressText] = useState("");
  const [noteVisible, setNoteVisible] = useState(false);
  const [noteText, setNoteText] = useState("");
  const [errorVisible, setErrorVisible] = useState(false);
  const [errorText, setErrorText] = useState("");
  const [versions, setVersions] = useState<string[]>([]);
  const versionsLoadedRef = useRef(false);

  // Every refresh() re-derives the play button's state from the backend's playing flag,
  // exactly like the original's setPlaying(info.playing ? "running" : "idle") inside refresh().
  useEffect(() => {
    if (info) setPlaying(info.playing ? "running" : "idle");
  }, [info]);

  // The version list is fetched once at startup only, not on every subsequent info refresh.
  useEffect(() => {
    if (!info || versionsLoadedRef.current) return;
    versionsLoadedRef.current = true;
    listVersions()
      .then(setVersions)
      .catch(() => setVersions([...info.lucentVersions].reverse()));
  }, [info]);

  function showError(message: string) {
    setPlaying("idle");
    setErrorText(message);
    setErrorVisible(true);
  }
  function showNote(message: string) {
    setNoteText(message);
    setNoteVisible(true);
  }

  useTauriEvent<Progress>("install-progress", (payload) => {
    const stage = STAGES[payload.stage];
    const fraction = payload.total === 0 ? 1 : payload.done / payload.total;
    setProgressPct((stage.from + (stage.to - stage.from) * fraction) * 100);
    const count = payload.total > 1 ? ` ${payload.done.toLocaleString()} / ${payload.total.toLocaleString()}` : "";
    setProgressText(`${stage.label}${count}`);
  });
  useTauriEvent("game-started", () => setPlaying("running"));
  useTauriEvent<number | null>("game-exit", (payload) => {
    setPlaying("idle");
    if (payload !== 0 && payload !== null) {
      showError(`게임이 오류 코드 ${payload}로 종료됐어요. 자세한 내용은 게임 로그에서 확인하세요.`);
    }
  });
  useTauriEvent<string>("play-error", (payload) => showError(payload));
  useTauriEvent<string[]>("mods-skipped", (payload) => {
    showNote(`${info?.version}용 파일이 없어서 빼고 실행한 모드: ${payload.join(", ")}`);
  });
  useTauriEvent<string>("mods-warning", () => {
    showNote("모드를 최신으로 맞추지 못해서, 이미 받아 둔 모드로 실행했어요. 인터넷 연결을 확인하세요.");
  });
  useTauriEvent<{ modName: string; missing: string[] }[]>("mods-dependency-warning", (payload) => {
    const summary = payload.map((issue) => `${issue.modName} (${issue.missing.join(", ")} 필요)`).join(", ");
    showNote(`일부 모드에 필요한 다른 모드가 빠져 있어요: ${summary}`);
  });

  const account = info?.accounts.find((a) => a.uuid === info.selected) ?? null;

  const versionOptions = useMemo(() => {
    if (!info) return [];
    return versions.includes(info.version) ? versions : [info.version, ...versions];
  }, [versions, info]);

  async function handleVersionChange(event: ChangeEvent<HTMLSelectElement>) {
    await setVersion(event.target.value);
    await refresh();
  }

  async function handlePlayClick() {
    setErrorVisible(false);
    setNoteVisible(false);
    clearLog();
    setPlaying("preparing");
    setProgressPct(0);
    setProgressText("");
    try {
      await play();
    } catch (error) {
      showError(String(error));
    }
  }

  const stackText = info
    ? info.lucentVersions.includes(info.version)
      ? `Fabric과 Lucent Client ${info.modVersion}로 실행합니다.`
      : "Lucent Client가 아직 이 버전을 지원하지 않아서, Fabric과 설치한 모드로만 실행합니다."
    : "";

  return (
    <section className="view" id="view-play" hidden={hidden}>
      <div className="stage">
        <div className="version-line">
          <label htmlFor="version">버전</label>
          <select id="version" value={info?.version ?? ""} onChange={handleVersionChange}>
            {versionOptions.map((version) => (
              <option key={version} value={version}>
                {info?.lucentVersions.includes(version) ? `${version} (Lucent Client)` : version}
              </option>
            ))}
          </select>
        </div>
        <p className="stack">{stackText}</p>
        <button className="play" disabled={playing !== "idle" || !account} onClick={handlePlayClick}>
          <span className={"lamp" + (playing === "running" ? " on" : "") + (playing === "preparing" ? " loading" : "")} aria-hidden="true" />
          <span className="play-label">{PLAY_LABEL[playing]}</span>
        </button>
        <div className="progress" hidden={playing !== "preparing"}>
          <div className="bar">
            <div className="fill" style={{ width: `${progressPct}%` }} />
          </div>
          <p className="progress-text">{progressText}</p>
        </div>
        <p className="note" hidden={!noteVisible}>
          {noteText}
        </p>
        <p className="error" hidden={!errorVisible}>
          {errorText}
        </p>
      </div>
    </section>
  );
}
