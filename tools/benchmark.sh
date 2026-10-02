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
    for project in sodium lithium ferrite-core immediatelyfast entityculling moreculling; do
      url=$(curl -s "https://api.modrinth.com/v2/project/$project/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22$build%22%5D" \
        | python3 -c "import json,sys;v=json.load(sys.stdin);print(next(f['url'] for f in v[0]['files'] if f['primary']) if v else '')")
      [ -n "$url" ] && [ ! -f "$dir/$(basename "$url")" ] && curl -sL -o "$dir/$(basename "$url")" "$url"
    done
  done
  export EXTRA_MODS="$PWD/build/perf-mods/{build}"
fi
BENCHMARK=1 exec tools/self_test.sh "${builds[@]}"
