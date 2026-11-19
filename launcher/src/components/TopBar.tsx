import { useLayoutEffect, useRef } from "react";

export type ViewId = "play" | "mods" | "accounts" | "settings" | "logs";

export type UpdateNotice = { text: string; link?: { href: string; label: string } };

const NAV_ITEMS: { id: ViewId; label: string }[] = [
  { id: "play", label: "플레이" },
  { id: "mods", label: "모드" },
  { id: "accounts", label: "계정" },
  { id: "settings", label: "설정" },
  { id: "logs", label: "게임 로그" },
];

export function TopBar({
  view,
  onNavigate,
  update,
  launcherVersionText,
}: {
  view: ViewId;
  onNavigate: (view: ViewId) => void;
  update: UpdateNotice | null;
  launcherVersionText: string;
}) {
  const indicatorRef = useRef<HTMLDivElement>(null);
  const buttonRefs = useRef<Partial<Record<ViewId, HTMLButtonElement>>>({});

  /** Slides the shared highlight pill behind whichever nav button is active. Runs before
   * paint so switching views never shows a one-frame jump, matching the original's synchronous
   * offsetLeft/offsetWidth read in the click handler. */
  useLayoutEffect(() => {
    const indicator = indicatorRef.current;
    const button = buttonRefs.current[view];
    if (!indicator || !button) return;
    indicator.style.left = `${button.offsetLeft}px`;
    indicator.style.width = `${button.offsetWidth}px`;
  }, [view]);

  return (
    <header className="topbar">
      <div className="wordmark">Lucent</div>
      <div className="nav-group">
        <div className="nav-indicator" ref={indicatorRef} aria-hidden="true" />
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            className={"nav" + (view === item.id ? " active" : "")}
            ref={(el) => {
              if (el) buttonRefs.current[item.id] = el;
            }}
            onClick={() => onNavigate(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>
      <div className="status-group">
        {update && (
          <div className="update">
            {update.text}
            {update.link && (
              <a href={update.link.href} target="_blank" rel="noreferrer">
                {update.link.label}
              </a>
            )}
          </div>
        )}
        <div className="version">{launcherVersionText}</div>
      </div>
    </header>
  );
}
