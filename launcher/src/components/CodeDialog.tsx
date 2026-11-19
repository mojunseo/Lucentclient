import { useEffect, useRef } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import type { DeviceCode } from "../types";

export function CodeDialog({
  open,
  deviceCode,
  status,
  onClose,
}: {
  open: boolean;
  deviceCode: DeviceCode | null;
  status: string;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    else if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog ref={ref} onClose={onClose}>
      <p>브라우저에서 아래 코드를 입력하세요.</p>
      <p className="code">{deviceCode?.user_code}</p>
      <p className="note">{deviceCode?.verification_uri}</p>
      <div className="actions">
        <button
          className="button"
          onClick={async () => {
            if (!deviceCode) return;
            await navigator.clipboard?.writeText(deviceCode.user_code).catch(() => {});
            await openUrl(deviceCode.verification_uri);
          }}
        >
          코드 복사하고 페이지 열기
        </button>
        <button className="button quiet" onClick={onClose}>
          닫기
        </button>
      </div>
      <p className="note">{status}</p>
    </dialog>
  );
}
