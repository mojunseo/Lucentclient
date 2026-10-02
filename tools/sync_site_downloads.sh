#!/bin/bash
# Copies the latest GitHub release of Lucent Client into a website folder under fixed names, so the
# site can offer direct downloads that keep working across versions, and writes downloads.json with
# the version and file sizes.
#   tools/sync_site_downloads.sh /path/to/site/client/download
set -euo pipefail
dest=${1:?usage: $0 <download folder>}
repo=mojunseo/Lucentclient
mkdir -p "$dest"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

release=$(curl -fsSL "https://api.github.com/repos/$repo/releases/latest")
tag=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["tag_name"])' <<<"$release")
version=${tag#v}

# GitHub asset name pattern -> fixed name on the site.
declare -A map=(
  ['_x64-setup\.exe$']=lucent-launcher-windows.exe
  ['_x64_en-US\.msi$']=lucent-launcher-windows.msi
  ['_universal\.dmg$']=lucent-launcher-macos.dmg
  ['_amd64\.AppImage$']=lucent-launcher-linux.AppImage
  ['_amd64\.deb$']=lucent-launcher-linux.deb
  ['\.x86_64\.rpm$']=lucent-launcher-linux.rpm
)
python3 -c 'import json,sys; [print(a["name"], a["browser_download_url"]) for a in json.load(sys.stdin)["assets"]]' <<<"$release" > "$tmp/assets"

while read -r name url; do
  target=""
  for pattern in "${!map[@]}"; do
    [[ $name =~ $pattern ]] && target=${map[$pattern]}
  done
  # Mod jars: lucentclient-<version>+<build>.jar -> lucentclient-<build>.jar
  [[ $name =~ ^lucentclient-.*\+(.+)\.jar$ ]] && target="lucentclient-${BASH_REMATCH[1]}.jar"
  [ -z "$target" ] && continue
  curl -fsSL -o "$tmp/$target" "$url"
  echo "$name -> $target"
done < "$tmp/assets"

# Replace the old files only once everything downloaded.
mv "$tmp"/lucent* "$dest"/
python3 - "$dest" "$version" <<'PY'
import json, os, sys
dest, version = sys.argv[1], sys.argv[2]
files = {name: {"size": os.path.getsize(os.path.join(dest, name))}
         for name in sorted(os.listdir(dest)) if name.startswith("lucent")}
json.dump({"version": version, "files": files}, open(os.path.join(dest, "downloads.json"), "w"), indent=2)
PY
echo "synced $tag into $dest"
