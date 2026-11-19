import type { KeyboardEvent } from "react";

/** Click and keyboard (Enter/Space) both trigger the same action, for row-as-control elements. */
export function activateProps(onActivate: () => void, role: "button" | "switch" = "button") {
  return {
    tabIndex: 0,
    role,
    onClick: onActivate,
    onKeyDown: (event: KeyboardEvent) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        onActivate();
      }
    },
  };
}
