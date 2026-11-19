export type Settings = { memoryMb: number; jvmArgs: string; hideOnLaunch: boolean; performanceMods: boolean };
export type AccountView = { kind: "microsoft" | "offline"; name: string; uuid: string };
export type Info = {
  lucentVersions: string[];
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
export type Progress = { stage: "metadata" | "libraries" | "assets" | "java" | "mods"; done: number; total: number };
export type DeviceCode = { user_code: string; verification_uri: string };
export type SearchHit = {
  projectId: string;
  title: string;
  author: string;
  description: string;
  downloads: number;
  iconUrl: string | null;
};
export type InstalledMod = { projectId: string; title: string; iconUrl: string | null; enabled: boolean; missing: boolean };
export type UpdateProgress = { version: string; done: number; total: number | null };
