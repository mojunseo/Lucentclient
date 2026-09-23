import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { openUrl } from "@tauri-apps/plugin-opener";

type Settings = { memoryMb: number; jvmArgs: string; hideOnLaunch: boolean };
type AccountView = { kind: "microsoft" | "offline"; name: string; uuid: string };
type Info = {
  minecraft: string;
  version: string;
  fabricLoader: string;
  modVersion: string;
  launcherVersion: string;
  settings: Settings;
  accounts: AccountView[];
  selected: string | null;
  microsoftReady: boolean;
  development: boolean;
  playing: boolean;
};
type Progress = { stage: "metadata" | "libraries" | "assets" | "java" | "mods"; done: number; total: number };
type DeviceCode = { user_code: string; verification_uri: string };
type SearchHit = { projectId: string; title: string; author: string; description: string; downloads: number; iconUrl: string | null };
type InstalledMod = { projectId: string; title: string; filename: string; iconUrl: string | null; dependency: boolean; enabled: boolean };

const $ = <T extends HTMLElement = HTMLElement>(id: string) => document.getElementById(id) as T;
let info: Info;

// --- Navigation -------------------------------------------------------------------------------

function show(view: string) {
  document.querySelectorAll<HTMLElement>(".view").forEach((el) => (el.hidden = el.id !== `view-${view}`));
  document.querySelectorAll<HTMLButtonElement>(".nav").forEach((el) => el.classList.toggle("active", el.dataset.view === view));
}
document.querySelectorAll<HTMLButtonElement>(".nav").forEach((el) =>
  el.addEventListener("click", () => {
    show(el.dataset.view!);
    if (el.dataset.view === "mods") openMods();
  }),
);

// --- Accounts ---------------------------------------------------------------------------------

const avatar = (uuid: string, size: number) => `https://mc-heads.net/avatar/${uuid}/${size}`;

function selectedAccount() {
  return info.accounts.find((a) => a.uuid === info.selected) ?? null;
}

function renderAccounts() {
  const current = selectedAccount();
  const chip = $("account-chip");
  chip.replaceChildren();
  if (current) {
    const img = document.createElement("img");
    img.src = avatar(current.uuid, 28);
    img.alt = "";
    const text = document.createElement("div");
    text.textContent = current.name;
    const small = document.createElement("small");
    small.textContent = "계정 바꾸기";
    text.append(small);
    chip.append(img, text);
  } else {
    chip.textContent = "로그인하기";
  }

  const list = $("account-list");
  list.replaceChildren();
  if (info.accounts.length === 0) {
    const empty = document.createElement("li");
    empty.className = "empty";
    empty.textContent = "아직 로그인한 계정이 없어요.";
    list.append(empty);
  }
  for (const account of info.accounts) {
    const row = document.createElement("li");
    row.className = "row account-row";
    row.innerHTML = `<span class="who"><img alt="" src="${avatar(account.uuid, 24)}"><span></span></span>
      <span class="control"><button class="remove">제거</button><span class="lamp"></span></span>`;
    const name = row.querySelector(".who span")!;
    name.textContent = account.name;
    if (account.kind === "offline") {
      const tag = document.createElement("small");
      tag.textContent = "오프라인";
      name.append(tag);
    }
    row.querySelector(".lamp")!.classList.toggle("on", account.uuid === info.selected);
    row.addEventListener("click", async () => {
      await invoke("select_account", { uuid: account.uuid });
      await refresh();
    });
    row.querySelector(".remove")!.addEventListener("click", async (event) => {
      event.stopPropagation();
      await invoke("remove_account", { uuid: account.uuid });
      await refresh();
    });
    list.append(row);
  }

  const signIn = $<HTMLButtonElement>("sign-in");
  signIn.disabled = !info.microsoftReady;
  const note = $("sign-in-note");
  note.hidden = info.microsoftReady;
  note.textContent = "Microsoft 로그인은 아직 쓸 수 없어요. 런처의 Azure 앱이 Mojang 승인을 받으면 열립니다.";
  $("offline-form").hidden = !info.development;
}

$("account-chip").addEventListener("click", () => show("accounts"));

$("offline-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const input = $<HTMLInputElement>("offline-name");
  try {
    await invoke("add_offline_account", { name: input.value.trim() });
    input.value = "";
    await refresh();
  } catch (error) {
    showNote(String(error));
  }
});

function showNote(message: string) {
  const note = $("sign-in-note");
  note.hidden = false;
  note.textContent = message;
}

// Microsoft sign-in with a device code.
let deviceCode: DeviceCode | null = null;
$("sign-in").addEventListener("click", async () => {
  try {
    await invoke("sign_in");
  } catch (error) {
    showNote(String(error));
  }
});
listen<DeviceCode>("auth-code", ({ payload }) => {
  deviceCode = payload;
  $("device-code").textContent = payload.user_code;
  $("device-url").textContent = payload.verification_uri;
  $("dialog-status").textContent = "로그인을 기다리는 중…";
  $<HTMLDialogElement>("code-dialog").showModal();
});
$("copy-open").addEventListener("click", async () => {
  if (!deviceCode) return;
  await navigator.clipboard?.writeText(deviceCode.user_code).catch(() => {});
  await openUrl(deviceCode.verification_uri);
});
$("close-dialog").addEventListener("click", () => $<HTMLDialogElement>("code-dialog").close());
listen("auth-done", async () => {
  $<HTMLDialogElement>("code-dialog").close();
  await refresh();
});
listen<string>("auth-error", ({ payload }) => {
  $("dialog-status").textContent = payload;
});

// --- Play -------------------------------------------------------------------------------------

const STAGES: Record<Progress["stage"], { label: string; from: number; to: number }> = {
  metadata: { label: "버전 정보 확인", from: 0, to: 0.03 },
  libraries: { label: "라이브러리", from: 0.03, to: 0.2 },
  assets: { label: "에셋", from: 0.2, to: 0.7 },
  java: { label: "Java", from: 0.7, to: 0.95 },
  mods: { label: "모드", from: 0.95, to: 1 },
};

function setPlaying(state: "idle" | "preparing" | "running") {
  const button = $<HTMLButtonElement>("play");
  button.disabled = state !== "idle" || !selectedAccount();
  button.querySelector(".lamp")!.classList.toggle("on", state === "running");
  $("play-label").textContent = { idle: "플레이", preparing: "준비 중", running: "실행 중" }[state];
  $("progress").hidden = state !== "preparing";
}

$("play").addEventListener("click", async () => {
  $("play-error").hidden = true;
  $("log").replaceChildren();
  setPlaying("preparing");
  $<HTMLElement>("progress-fill").style.width = "0";
  $("progress-text").textContent = "";
  try {
    await invoke("play");
  } catch (error) {
    showPlayError(String(error));
  }
});

listen<Progress>("install-progress", ({ payload }) => {
  const stage = STAGES[payload.stage];
  const fraction = payload.total === 0 ? 1 : payload.done / payload.total;
  $<HTMLElement>("progress-fill").style.width = `${(stage.from + (stage.to - stage.from) * fraction) * 100}%`;
  const count = payload.total > 1 ? ` ${payload.done.toLocaleString()} / ${payload.total.toLocaleString()}` : "";
  $("progress-text").textContent = `${stage.label}${count}`;
});
listen("game-started", () => setPlaying("running"));
listen<number | null>("game-exit", ({ payload }) => {
  setPlaying("idle");
  if (payload !== 0 && payload !== null) {
    showPlayError(`게임이 오류 코드 ${payload}로 종료됐어요. 자세한 내용은 게임 로그에서 확인하세요.`);
  }
});
listen<string>("play-error", ({ payload }) => showPlayError(payload));

function showPlayError(message: string) {
  setPlaying("idle");
  const error = $("play-error");
  error.hidden = false;
  error.textContent = message;
}

// --- Logs -------------------------------------------------------------------------------------

const MAX_LOG_LINES = 5000;
listen<{ line: string; error: boolean }>("game-log", ({ payload }) => {
  const log = $("log");
  const atBottom = log.scrollTop + log.clientHeight >= log.scrollHeight - 8;
  const line = document.createElement("div");
  line.textContent = payload.line;
  if (payload.error) line.className = "err";
  log.append(line);
  while (log.childElementCount > MAX_LOG_LINES) log.firstElementChild!.remove();
  if (atBottom) log.scrollTop = log.scrollHeight;
});

// --- Settings ---------------------------------------------------------------------------------

function renderSettings() {
  const memory = $<HTMLInputElement>("memory");
  memory.value = String(info.settings.memoryMb);
  $("memory-value").textContent = `${(info.settings.memoryMb / 1024).toFixed(1)} GB`;
  $<HTMLInputElement>("jvm-args").value = info.settings.jvmArgs;
  $("hide-lamp").classList.toggle("on", info.settings.hideOnLaunch);
}

async function saveSettings(change: Partial<Settings>) {
  info.settings = { ...info.settings, ...change };
  renderSettings();
  await invoke("save_settings", { settings: info.settings });
}

$<HTMLInputElement>("memory").addEventListener("input", (event) => {
  const value = Number((event.target as HTMLInputElement).value);
  $("memory-value").textContent = `${(value / 1024).toFixed(1)} GB`;
});
$<HTMLInputElement>("memory").addEventListener("change", (event) =>
  saveSettings({ memoryMb: Number((event.target as HTMLInputElement).value) }),
);
$<HTMLInputElement>("jvm-args").addEventListener("change", (event) =>
  saveSettings({ jvmArgs: (event.target as HTMLInputElement).value.trim() }),
);
$("hide-row").addEventListener("click", () => saveSettings({ hideOnLaunch: !info.settings.hideOnLaunch }));
$("open-folder").addEventListener("click", () => invoke("open_game_directory"));

// --- Versions ---------------------------------------------------------------------------------

async function loadVersions() {
  const select = $<HTMLSelectElement>("version");
  const versions = await invoke<string[]>("list_versions").catch(() => [info.minecraft]);
  if (!versions.includes(info.version)) versions.unshift(info.version);
  select.replaceChildren(
    ...versions.map((version) => {
      const option = document.createElement("option");
      option.value = version;
      option.textContent = version === info.minecraft ? `${version} (Lucent Client)` : version;
      return option;
    }),
  );
  select.value = info.version;
}

$<HTMLSelectElement>("version").addEventListener("change", async (event) => {
  await invoke("set_version", { version: (event.target as HTMLSelectElement).value });
  await refresh();
});

// --- Mods -------------------------------------------------------------------------------------

let modQuery = "";
let modOffset = 0;

function modIcon(url: string | null) {
  if (!url) {
    const blank = document.createElement("span");
    blank.className = "no-icon";
    return blank;
  }
  const img = document.createElement("img");
  img.src = url;
  img.alt = "";
  img.loading = "lazy";
  return img;
}

function modRow(icon: string | null, title: string, detail: string, tag?: string) {
  const row = document.createElement("li");
  row.className = "row mod-row";
  const main = document.createElement("div");
  main.className = "mod-main";
  const text = document.createElement("div");
  text.className = "mod-text";
  const strong = document.createElement("strong");
  strong.textContent = title;
  text.append(strong);
  if (tag) {
    const small = document.createElement("small");
    small.textContent = tag;
    text.append(small);
  }
  const p = document.createElement("p");
  p.textContent = detail;
  text.append(p);
  main.append(modIcon(icon), text);
  const control = document.createElement("div");
  control.className = "control";
  row.append(main, control);
  return { row, control };
}

let installed: InstalledMod[] = [];

async function renderInstalled() {
  installed = await invoke<InstalledMod[]>("installed_mods");
  const list = $("installed-mods");
  list.replaceChildren();
  if (installed.length === 0) {
    const empty = document.createElement("li");
    empty.className = "empty";
    empty.textContent = "아직 설치한 모드가 없어요. 아래에서 찾아 설치하세요.";
    list.append(empty);
  }
  for (const mod of installed) {
    const { row, control } = modRow(mod.iconUrl, mod.title, mod.filename, mod.dependency ? "다른 모드에 필요" : undefined);
    const remove = document.createElement("button");
    remove.className = "button quiet";
    remove.textContent = "삭제";
    remove.addEventListener("click", async () => {
      await invoke("remove_mod", { projectId: mod.projectId });
      await renderInstalled();
      renderResults();
    });
    const lamp = document.createElement("span");
    lamp.className = "lamp" + (mod.enabled ? " on" : "");
    lamp.title = mod.enabled ? "켜짐" : "꺼짐";
    lamp.addEventListener("click", async () => {
      await invoke("set_mod_enabled", { filename: mod.filename, enabled: !mod.enabled });
      await renderInstalled();
    });
    control.append(remove, lamp);
    list.append(row);
  }
}

let results: SearchHit[] = [];

function renderResults() {
  const list = $("mod-results");
  list.replaceChildren();
  for (const hit of results) {
    const downloads = `${hit.author}, 다운로드 ${hit.downloads.toLocaleString()}회`;
    const { row, control } = modRow(hit.iconUrl, hit.title, hit.description, downloads);
    const done = installed.some((mod) => mod.projectId === hit.projectId);
    const button = document.createElement("button");
    button.className = done ? "button quiet" : "button";
    button.textContent = done ? "설치됨" : "설치";
    button.disabled = done;
    button.addEventListener("click", async () => {
      button.disabled = true;
      button.textContent = "설치 중";
      $("mods-error").hidden = true;
      try {
        await invoke("install_mod", { projectId: hit.projectId });
        await renderInstalled();
        renderResults();
      } catch (error) {
        const message = $("mods-error");
        message.hidden = false;
        message.textContent = `${hit.title}: ${error}`;
        button.disabled = false;
        button.textContent = "설치";
      }
    });
    control.append(button);
    list.append(row);
  }
}

async function searchMods(reset: boolean) {
  if (reset) {
    modOffset = 0;
    results = [];
  }
  $("mods-error").hidden = true;
  try {
    const page = await invoke<{ hits: SearchHit[]; total: number }>("search_mods", { query: modQuery, offset: modOffset });
    results = results.concat(page.hits);
    modOffset += 20;
    $("more-mods").hidden = modOffset >= page.total;
    renderResults();
  } catch (error) {
    const message = $("mods-error");
    message.hidden = false;
    message.textContent = String(error);
  }
}

async function openMods() {
  $("mods-title").textContent = `${info.version}용 모드`;
  await renderInstalled();
  if (results.length === 0) await searchMods(true);
}

$("mod-search").addEventListener("submit", (event) => {
  event.preventDefault();
  modQuery = $<HTMLInputElement>("mod-query").value;
  searchMods(true);
});
$("more-mods").addEventListener("click", () => searchMods(false));

// --- Startup ----------------------------------------------------------------------------------

async function refresh() {
  info = await invoke<Info>("get_info");
  const lucent = info.version === info.minecraft;
  $("stack").textContent = lucent
    ? `Fabric ${info.fabricLoader}와 Lucent Client ${info.modVersion}로 실행합니다.`
    : `Lucent Client는 ${info.minecraft} 전용이라, 이 버전은 Fabric과 설치한 모드로만 실행합니다.`;
  // Search results depend on the version; start over when it changes.
  results = [];
  $("launcher-version").textContent = `런처 ${info.launcherVersion}`;
  renderAccounts();
  renderSettings();
  setPlaying(info.playing ? "running" : "idle");
}

refresh().then(() => {
  loadVersions();
  show(selectedAccount() ? "play" : "accounts");
});
