#!/bin/bash
# Starts each build's dev client with every mixin applied up front, reports mixin failures, and
# closes the game as soon as the check is done. Usage: tools/mixin_check.sh [build ...]
cd "$(dirname "$0")/.."
builds=("$@")
[ ${#builds[@]} -eq 0 ] && builds=($(grep -oP '^\["\K[^"]+' stonecutter.properties.toml))
status=0
for build in "${builds[@]}"; do
  log="build/mixin-check-$build.log"
  mkdir -p build
  ./gradlew ":$build:runClient" --console=plain -Plucentclient.audit=true > "$log" 2>&1 &
  gradle=$!
  result="timeout"
  for _ in $(seq 1 240); do
    if grep -q "Mixin audit finished" "$log"; then result="ok"; break; fi
    if grep -qE "Mixin apply for mod lucentclient failed|MixinTransformerError|InvalidInjectionException|Crash report saved" "$log"; then result="failed"; break; fi
    if ! kill -0 $gradle 2>/dev/null; then result="exited"; break; fi
    sleep 1
  done
  # The game runs as a child of the Gradle daemon; it is the only JVM started with the audit flag.
  for pid in $(pgrep -f -- "-Dlucentclient.auditMixins=true"); do kill "$pid" 2>/dev/null; done
  kill $gradle 2>/dev/null
  wait $gradle 2>/dev/null
  echo "$build: $result"
  if [ "$result" != ok ]; then
    status=1
    grep -E "Invalid descriptor|failed|Could not find|No refMap|Mixin apply|Exception" "$log" | grep -v "Failed to fetch user properties" | head -5
  fi
done
exit $status
