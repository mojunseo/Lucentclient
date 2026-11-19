import { useEffect, useRef } from "react";
import { listen } from "@tauri-apps/api/event";

/**
 * Subscribes to a Tauri event for the lifetime of the component. `listen()` resolves
 * asynchronously, so unmounting before it resolves is handled by a `cancelled` flag rather
 * than assuming the unlisten function is available synchronously in the cleanup.
 *
 * The handler is read through a ref so the effect itself can keep an empty dependency array
 * (subscribe once) without the listener closing over a stale callback.
 */
export function useTauriEvent<T>(event: string, handler: (payload: T) => void) {
  const handlerRef = useRef(handler);
  handlerRef.current = handler;

  useEffect(() => {
    let unlisten: (() => void) | undefined;
    let cancelled = false;
    listen<T>(event, (e) => handlerRef.current(e.payload)).then((fn) => {
      if (cancelled) fn();
      else unlisten = fn;
    });
    return () => {
      cancelled = true;
      unlisten?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [event]);
}
