import { invoke } from "@tauri-apps/api/core";
import type { Info, InstalledMod, SearchHit, Settings } from "../types";

export const getInfo = () => invoke<Info>("get_info");
export const saveSettings = (settings: Settings) => invoke<void>("save_settings", { settings });
export const listVersions = () => invoke<string[]>("list_versions");
export const setVersion = (version: string) => invoke<void>("set_version", { version });
export const searchMods = (query: string, offset: number, sort: string, categories: string[]) =>
  invoke<{ hits: SearchHit[]; total: number }>("search_mods", { query, offset, sort, categories });
export const installedMods = () => invoke<InstalledMod[]>("installed_mods");
export const installMod = (projectId: string) => invoke<unknown>("install_mod", { projectId });
export const removeMod = (projectId: string) => invoke<unknown>("remove_mod", { projectId });
export const setModEnabled = (projectId: string, enabled: boolean) =>
  invoke<unknown>("set_mod_enabled", { projectId, enabled });
export const selectAccount = (uuid: string) => invoke<void>("select_account", { uuid });
export const removeAccount = (uuid: string) => invoke<void>("remove_account", { uuid });
export const addOfflineAccount = (name: string) => invoke<void>("add_offline_account", { name });
export const signIn = () => invoke<void>("sign_in");
export const updateLauncher = () => invoke<string | null>("update_launcher");
export const play = () => invoke<void>("play");
export const openGameDirectory = () => invoke<void>("open_game_directory");
