import type { ReactNode } from "react";

/** Shared "icon + title/tag/detail + control" row, used by both the installed and the
 * Modrinth-search mod lists. */
export function ModRow({
  icon,
  title,
  warning,
  tag,
  detail,
  children,
}: {
  icon: string | null;
  title: string;
  warning?: string;
  tag?: string;
  detail: string;
  children?: ReactNode;
}) {
  return (
    <li className="row mod-row">
      <div className="mod-main">
        {icon ? <img src={icon} alt="" loading="lazy" /> : <span className="no-icon" />}
        <div className="mod-text">
          <strong>{title}</strong>
          {warning && <span className="missing">{warning}</span>}
          {tag && <small>{tag}</small>}
          <p>{detail}</p>
        </div>
      </div>
      <div className="control">{children}</div>
    </li>
  );
}
