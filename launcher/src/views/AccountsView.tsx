import { useEffect, useState, type FormEvent } from "react";
import { useInfo } from "../context/InfoContext";
import { activateProps } from "../lib/activate";
import { addOfflineAccount, removeAccount, selectAccount, signIn } from "../lib/tauri";

const avatar = (uuid: string, size: number) => `https://mc-heads.net/avatar/${uuid}/${size}`;

const DEFAULT_NOTE = "Microsoft 로그인은 아직 쓸 수 없어요. 런처의 Azure 앱이 Mojang 승인을 받으면 열립니다.";

export function AccountsView({ hidden }: { hidden: boolean }) {
  const { info, refresh } = useInfo();
  const [noteOverride, setNoteOverride] = useState<string | null>(null);
  const [offlineName, setOfflineName] = useState("");

  // Every refresh() recomputes the note from scratch in the original, which clears any
  // transient error shown by a failed sign-in/offline-account attempt.
  useEffect(() => {
    setNoteOverride(null);
  }, [info]);

  if (!info) return <section className="view" id="view-accounts" hidden={hidden} />;

  const noteVisible = noteOverride !== null || !info.microsoftReady;
  const noteText = noteOverride ?? DEFAULT_NOTE;

  async function handleSignIn() {
    try {
      await signIn();
    } catch (error) {
      setNoteOverride(String(error));
    }
  }

  async function handleOfflineSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await addOfflineAccount(offlineName.trim());
      setOfflineName("");
      await refresh();
    } catch (error) {
      setNoteOverride(String(error));
    }
  }

  return (
    <section className="view" id="view-accounts" hidden={hidden}>
      <h1>계정</h1>
      <ul className="rows">
        {info.accounts.length === 0 && <li className="empty">아직 로그인한 계정이 없어요.</li>}
        {info.accounts.map((account) => (
          <li
            key={account.uuid}
            className="row account-row"
            {...activateProps(async () => {
              await selectAccount(account.uuid);
              await refresh();
            })}
          >
            <span className="who">
              <img alt="" src={avatar(account.uuid, 24)} />
              <span>
                {account.name}
                {account.kind === "offline" && <small>오프라인</small>}
              </span>
            </span>
            <span className="control">
              <button
                className="remove"
                onClick={async (event) => {
                  event.stopPropagation();
                  await removeAccount(account.uuid);
                  await refresh();
                }}
              >
                제거
              </button>
              <span className={"lamp" + (account.uuid === info.selected ? " on" : "")} />
            </span>
          </li>
        ))}
      </ul>
      <div className="actions">
        <button className="button" disabled={!info.microsoftReady} onClick={handleSignIn}>
          Microsoft 계정으로 로그인
        </button>
      </div>
      <p className="note" hidden={!noteVisible}>
        {noteText}
      </p>
      <form className="offline" hidden={!info.development} onSubmit={handleOfflineSubmit}>
        <label htmlFor="offline-name">개발용 오프라인 계정</label>
        <div className="inline">
          <input
            id="offline-name"
            maxLength={16}
            placeholder="닉네임"
            autoComplete="off"
            value={offlineName}
            onChange={(event) => setOfflineName(event.target.value)}
          />
          <button className="button quiet" type="submit">
            추가
          </button>
        </div>
        <p className="note">개발 빌드에서만 보입니다. 싱글플레이와 오프라인 서버에서만 쓸 수 있어요.</p>
      </form>
    </section>
  );
}
