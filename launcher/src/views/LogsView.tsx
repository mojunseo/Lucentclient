import type { RefObject } from "react";

type LogLine = { line: string; error: boolean };

export function LogsView({
  hidden,
  log,
  logRef,
}: {
  hidden: boolean;
  log: LogLine[];
  logRef: RefObject<HTMLPreElement | null>;
}) {
  return (
    <section className="view" id="view-logs" hidden={hidden}>
      <h1>게임 로그</h1>
      <pre className="log" ref={logRef}>
        {log.map((entry, index) => (
          <div key={index} className={entry.error ? "err" : undefined}>
            {entry.line}
          </div>
        ))}
      </pre>
    </section>
  );
}
