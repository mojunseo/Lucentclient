import { useEffect, useRef, useState } from "react";
import { useInfo } from "../context/InfoContext";
import { activateProps } from "../lib/activate";
import { openGameDirectory, saveSettings } from "../lib/tauri";
import type { Settings } from "../types";

export function SettingsView({ hidden }: { hidden: boolean }) {
  const { info, patchSettings } = useInfo();
  const memoryRef = useRef<HTMLInputElement>(null);
  const jvmRef = useRef<HTMLInputElement>(null);
  const [memoryLabelMb, setMemoryLabelMb] = useState(info?.settings.memoryMb ?? 4096);

  // The visible "GB" label follows the slider live (native 'input'); info.settings.memoryMb
  // itself (and the backend) is only touched on commit (native 'change'), exactly like the
  // original's separate input/change listeners.
  useEffect(() => {
    if (info) setMemoryLabelMb(info.settings.memoryMb);
  }, [info?.settings.memoryMb]);

  async function commit(change: Partial<Settings>) {
    if (!info) return;
    const next = { ...info.settings, ...change };
    patchSettings(change);
    await saveSettings(next);
  }

  useEffect(() => {
    const el = memoryRef.current;
    if (!el) return;
    const onCommit = () => void commit({ memoryMb: Number(el.value) });
    el.addEventListener("change", onCommit);
    return () => el.removeEventListener("change", onCommit);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [info]);

  useEffect(() => {
    const el = jvmRef.current;
    if (!el) return;
    const onCommit = () => void commit({ jvmArgs: el.value.trim() });
    el.addEventListener("change", onCommit);
    return () => el.removeEventListener("change", onCommit);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [info]);

  if (!info) return <section className="view" id="view-settings" hidden={hidden} />;

  return (
    <section className="view" id="view-settings" hidden={hidden}>
      <h1>설정</h1>
      <ul className="rows">
        <li className="row">
          <label htmlFor="memory">메모리</label>
          <div className="control">
            <input
              type="range"
              id="memory"
              min={1024}
              max={16384}
              step={512}
              ref={memoryRef}
              defaultValue={info.settings.memoryMb}
              onChange={(event) => setMemoryLabelMb(Number(event.target.value))}
            />
            <output id="memory-value">{(memoryLabelMb / 1024).toFixed(1)} GB</output>
          </div>
        </li>
        <li className="row">
          <label htmlFor="jvm-args">추가 JVM 인자</label>
          <div className="control">
            <input id="jvm-args" placeholder="-XX:+UseZGC" autoComplete="off" ref={jvmRef} defaultValue={info.settings.jvmArgs} />
          </div>
        </li>
        <li
          className="row toggle-row"
          id="perf-row"
          aria-checked={info.settings.performanceMods}
          {...activateProps(() => commit({ performanceMods: !info.settings.performanceMods }), "switch")}
        >
          <span>
            성능 모드 함께 설치
            <small className="hint">Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, More Culling</small>
          </span>
          <span className={"lamp" + (info.settings.performanceMods ? " on" : "")} aria-hidden="true" />
        </li>
        <li
          className="row toggle-row"
          id="hide-row"
          aria-checked={info.settings.hideOnLaunch}
          {...activateProps(() => commit({ hideOnLaunch: !info.settings.hideOnLaunch }), "switch")}
        >
          <span>게임 중에는 런처 숨기기</span>
          <span className={"lamp" + (info.settings.hideOnLaunch ? " on" : "")} aria-hidden="true" />
        </li>
      </ul>
      <div className="actions">
        <button className="button quiet" onClick={() => openGameDirectory()}>
          게임 폴더 열기
        </button>
      </div>
    </section>
  );
}
