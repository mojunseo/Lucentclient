#!/bin/bash
# FPS benchmark on each build (see SelfTest.setUpBenchmark): 15 s with the player's video settings at
# 12 chunks, then 15 s with Lightweight Mode. Usage: tools/benchmark.sh [build ...]
# PERF_MODS=1 also loads the performance mods the launcher installs (Sodium, Lithium, FerriteCore,
# ImmediatelyFast, Entity Culling, More Culling), downloaded from Modrinth into build/perf-mods/<build>.
cd "$(dirname "$0")/.."
builds=("$@")
[ ${#builds[@]} -eq 0 ] && builds=($(grep -oP '^\["\K[^"]+' stonecutter.properties.toml))
if [ -n "$PERF_MODS" ]; then
  for build in "${builds[@]}"; do
    dir="build/perf-mods/$build"
    mkdir -p "$dir"
    # The same choice as the launcher: newest release (else newest) for the build, plus required dependencies.
    python3 - "$build" "$dir" <<'PY'
import json, os, sys, urllib.parse, urllib.request
build, dest = sys.argv[1], sys.argv[2]
api = "https://api.modrinth.com/v2"
def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "lucentclient-benchmark"})) as r:
        return json.load(r)
queue = ["sodium", "lithium", "ferrite-core", "immediatelyfast", "entityculling", "moreculling"]
seen = set(queue) | {"P7dR8mSH"}
while queue:
    project = queue.pop()
    q = urllib.parse.urlencode({"loaders": '["fabric"]', "game_versions": f'["{build}"]'})
    versions = get(f"{api}/project/{project}/version?{q}")
    if not versions:
        print(f"no {project} for {build}")
        continue
    version = next((v for v in versions if v["version_type"] == "release"), versions[0])
    file = next(f for f in version["files"] if f["primary"])
    path = os.path.join(dest, file["filename"])
    if not os.path.exists(path):
        urllib.request.urlretrieve(file["url"], path)
    for dep in version["dependencies"]:
        if dep["dependency_type"] == "required" and dep["project_id"] not in seen:
            seen.add(dep["project_id"])
            queue.append(dep["project_id"])
PY
  done
  export EXTRA_MODS="$PWD/build/perf-mods/{build}"
fi
BENCHMARK=1 exec tools/self_test.sh "${builds[@]}"
