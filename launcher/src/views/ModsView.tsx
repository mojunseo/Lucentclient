import { useEffect, useRef, useState, type FormEvent } from "react";
import { useInfo } from "../context/InfoContext";
import { activateProps } from "../lib/activate";
import { ModRow } from "../components/ModRow";
import { installMod, installedMods, removeMod, searchMods, setModEnabled } from "../lib/tauri";
import type { InstalledMod, SearchHit } from "../types";

/** Modrinth mod categories with Korean names, most useful for a client first. */
const CATEGORIES: [string, string][] = [
  ["optimization", "최적화"],
  ["utility", "유틸리티"],
  ["decoration", "장식"],
  ["adventure", "모험"],
  ["worldgen", "월드 생성"],
  ["technology", "기술"],
  ["magic", "마법"],
  ["storage", "저장"],
  ["equipment", "장비"],
  ["mobs", "몹"],
  ["food", "음식"],
  ["game-mechanics", "게임 메커니즘"],
  ["transportation", "이동 수단"],
  ["social", "소셜"],
  ["management", "관리"],
  ["economy", "경제"],
  ["minigame", "미니게임"],
  ["library", "라이브러리"],
  ["cursed", "기묘한 것"],
];

export function ModsView({ hidden }: { hidden: boolean }) {
  const { info } = useInfo();
  const [modSort, setModSort] = useState("downloads");
  const [modCategories, setModCategories] = useState<Set<string>>(new Set());
  const [modOffset, setModOffset] = useState(0);
  const [modQuery, setModQuery] = useState("");
  const [installed, setInstalled] = useState<InstalledMod[]>([]);
  const [results, setResults] = useState<SearchHit[]>([]);
  const [modCount, setModCount] = useState("");
  const [moreHidden, setMoreHidden] = useState(true);
  const [modsError, setModsError] = useState<string | null>(null);
  const [installingIds, setInstallingIds] = useState<Set<string>>(new Set());
  const queryInputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<SearchHit[]>([]);
  resultsRef.current = results;

  async function refreshInstalled() {
    const list = await installedMods();
    setInstalled(list);
  }

  async function runSearch(reset: boolean, overrides: { query?: string; sort?: string; categories?: Set<string> }) {
    const query = overrides.query ?? modQuery;
    const sort = overrides.sort ?? modSort;
    const categories = overrides.categories ?? modCategories;
    const offset = reset ? 0 : modOffset;
    if (reset) setResults([]);
    setModsError(null);
    try {
      const page = await searchMods(query, offset, sort, [...categories]);
      setResults((prev) => (reset ? page.hits : [...prev, ...page.hits]));
      setModCount(page.total === 0 ? "조건에 맞는 모드가 없어요." : `${page.total.toLocaleString()}개`);
      setModOffset(offset + 20);
      setMoreHidden(offset + 20 >= page.total);
    } catch (error) {
      setModsError(String(error));
    }
  }

  // Mirrors the original's openMods(): runs every time the user navigates to this view (not
  // just on first mount, since every view stays mounted and only toggles `hidden`), always
  // refreshing the installed list but only searching if there's no cached result set yet.
  useEffect(() => {
    if (hidden) return;
    void (async () => {
      await refreshInstalled();
      if (resultsRef.current.length === 0) await runSearch(true, {});
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hidden]);

  async function changeMods(action: () => Promise<unknown>) {
    setModsError(null);
    try {
      await action();
    } catch (error) {
      setModsError(String(error));
    }
    await refreshInstalled();
  }

  async function handleInstallClick(hit: SearchHit) {
    setInstallingIds((prev) => new Set(prev).add(hit.projectId));
    setModsError(null);
    try {
      await installMod(hit.projectId);
      await refreshInstalled();
    } catch (error) {
      setModsError(`${hit.title}: ${error}`);
    } finally {
      setInstallingIds((prev) => {
        const next = new Set(prev);
        next.delete(hit.projectId);
        return next;
      });
    }
  }

  function handleCategoryClick(id: string) {
    const next = new Set(modCategories);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setModCategories(next);
    void runSearch(true, { categories: next });
  }

  function handleSearchSubmit(event: FormEvent) {
    event.preventDefault();
    const query = queryInputRef.current?.value ?? "";
    let sort = modSort;
    // Typing a query without choosing a sort means "best match".
    if (query.trim() && sort === "downloads") sort = "relevance";
    setModQuery(query);
    setModSort(sort);
    void runSearch(true, { query, sort });
  }

  if (!info) return <section className="view" id="view-mods" hidden={hidden} />;

  return (
    <section className="view" id="view-mods" hidden={hidden}>
      <h1>모드</h1>
      <div className="mods-layout">
        <div className="mods-installed">
          <h2>내 모드</h2>
          <p className="note list-note">
            모든 버전이 함께 쓰는 목록이에요. 플레이할 때 고른 버전에 맞는 파일을 자동으로 받아 넣습니다.
          </p>
          <ul className="rows">
            {installed.length === 0 && <li className="empty">아직 설치한 모드가 없어요. 아래에서 찾아 설치하세요.</li>}
            {installed.map((mod) => {
              const status = mod.missing
                ? `${info.version}용 파일이 없어서 이 버전에서는 빠져요`
                : mod.enabled
                  ? "켜짐"
                  : "꺼짐";
              return (
                <ModRow
                  key={mod.projectId}
                  icon={mod.iconUrl}
                  title={mod.title}
                  detail={status}
                  warning={mod.missing ? "이 버전용 없음" : undefined}
                >
                  <button className="button quiet" onClick={() => changeMods(() => removeMod(mod.projectId))}>
                    삭제
                  </button>
                  <span
                    className={"lamp" + (mod.enabled ? " on" : "")}
                    title={mod.enabled ? "켜짐" : "꺼짐"}
                    aria-checked={mod.enabled}
                    {...activateProps(() => changeMods(() => setModEnabled(mod.projectId, !mod.enabled)), "switch")}
                  />
                </ModRow>
              );
            })}
          </ul>
        </div>
        <div className="mods-search">
          <h2>Modrinth에서 {info.version}용 모드 찾기</h2>
          <form className="inline search" onSubmit={handleSearchSubmit}>
            <input ref={queryInputRef} placeholder="모드 이름" autoComplete="off" />
            <select
              aria-label="정렬"
              value={modSort}
              onChange={(event) => {
                const sort = event.target.value;
                setModSort(sort);
                void runSearch(true, { sort });
              }}
            >
              <option value="relevance">관련도순</option>
              <option value="downloads">인기순</option>
              <option value="follows">팔로우 많은 순</option>
              <option value="newest">새로 나온 순</option>
              <option value="updated">최근 업데이트순</option>
            </select>
            <button className="button quiet" type="submit">
              검색
            </button>
          </form>
          <div className="categories">
            {CATEGORIES.map(([id, name]) => (
              <button
                key={id}
                type="button"
                className={"category" + (modCategories.has(id) ? " active" : "")}
                aria-pressed={modCategories.has(id)}
                onClick={() => handleCategoryClick(id)}
              >
                {name}
              </button>
            ))}
          </div>
          <p className="note result-count">{modCount}</p>
          <ul className="rows">
            {results.map((hit) => {
              const done = installed.some((mod) => mod.projectId === hit.projectId);
              const installing = installingIds.has(hit.projectId);
              return (
                <ModRow
                  key={hit.projectId}
                  icon={hit.iconUrl}
                  title={hit.title}
                  tag={`${hit.author}, 다운로드 ${hit.downloads.toLocaleString()}회`}
                  detail={hit.description}
                >
                  <button
                    className={done ? "button quiet" : "button"}
                    disabled={done || installing}
                    onClick={() => handleInstallClick(hit)}
                  >
                    {installing ? "설치 중" : done ? "설치됨" : "설치"}
                  </button>
                </ModRow>
              );
            })}
          </ul>
          <div className="actions">
            <button className="button quiet" hidden={moreHidden} onClick={() => void runSearch(false, {})}>
              더 보기
            </button>
          </div>
          <p className="error" hidden={!modsError}>
            {modsError}
          </p>
        </div>
      </div>
    </section>
  );
}
