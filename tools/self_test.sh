#!/bin/bash
# Runs SelfTest on each build: creates a world, turns everything on, screenshots the HUD, cosmetics and
# menus into run/screenshots/selftest-<build>-*.png, then closes the game. The dev config is restored
# afterwards. Usage: tools/self_test.sh [build ...]
# EXTRA_MODS=/path/to/instances/{build}/mods also loads the other mods from that folder (with {build}
# replaced), to test against a player's real mod list.
cd "$(dirname "$0")/.."
builds=("$@")
[ ${#builds[@]} -eq 0 ] && builds=($(grep -oP '^\["\K[^"]+' stonecutter.properties.toml))
backup=$(mktemp -d)
cp run/config/lucentclient*.json run/options.txt "$backup"/ 2>/dev/null
trap 'cp "$backup"/lucentclient*.json run/config/ 2>/dev/null; cp "$backup"/options.txt run/ 2>/dev/null; rm -rf "$backup"' EXIT
status=0
for build in "${builds[@]}"; do
  log="build/self-test-$build.log"
  mkdir -p build run/screenshots
  rm -f run/screenshots/selftest-"$build"-*.png
  rm -rf run/mods; mkdir -p run/mods
  if [ -n "$EXTRA_MODS" ]; then
    for jar in "${EXTRA_MODS//\{build\}/$build}"/*.jar; do
      case "$(basename "$jar")" in lucentclient-*|fabric-api-*) ;; *) cp "$jar" run/mods/ ;; esac
    done
  fi
  ./gradlew ":$build:runClient" --console=plain -Plucentclient.selfTest="$build" ${BENCHMARK:+-Plucentclient.benchmark=true} > "$log" 2>&1 &
  gradle=$!
  result="timeout"
  for _ in $(seq 1 300); do
    if grep -q "\[selftest\] finished" "$log"; then result="ok"; break; fi
    if grep -qE "Mixin apply for mod lucentclient failed|Crash report saved|Exception in thread \"Render thread\"" "$log"; then result="crashed"; break; fi
    if ! kill -0 $gradle 2>/dev/null; then result="exited"; break; fi
    sleep 1
  done
  sleep 3
  for pid in $(pgrep -f -- "-Dlucentclient.selfTest=$build"); do kill "$pid" 2>/dev/null; done
  kill $gradle 2>/dev/null
  wait $gradle 2>/dev/null
  rm -rf run/mods
  grep -o "\[benchmark\].*" "$log"
  echo "$build: $result ($(ls run/screenshots/selftest-"$build"-*.png 2>/dev/null | wc -l) screenshots)"
  [ "$result" = ok ] || status=1
  grep -E "ERROR|Exception|WARN.*lucentclient" "$log" | grep -vE "Failed to fetch user properties|InvalidCredentials|Status: 401|realms|Realms|flite|narrator|Native library" | sort | uniq -c | sort -rn | head -8
done
exit $status
