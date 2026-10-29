#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${ANDROID_JAR:-}" ]]; then
    if [[ -n "$SDK_ROOT" ]]; then ANDROID_JAR="$SDK_ROOT/platforms/android-35/android.jar";
    else ANDROID_JAR="$ROOT/tools/android-35/android.jar"; fi
fi
for tool in java javac; do command -v "$tool" >/dev/null || { echo "Missing tool: $tool"; exit 2; }; done
[[ -f "$ANDROID_JAR" ]] || { echo "Missing dependency: $ANDROID_JAR"; exit 2; }
[[ -f "$ROOT/tools/robolectric/robolectric-4.16.1.jar" ]] || { echo 'Missing Robolectric dependencies in tools/robolectric'; exit 2; }
[[ -d "$ROOT/build/classes" ]] || { echo 'Compile application classes first with scripts/build.sh'; exit 2; }
TESTLIBS=$(find "$ROOT/tools/robolectric" -name '*.jar' ! -name 'android-all*' | sort | tr '\n' ':')
CP="$ROOT/build/classes:$ANDROID_JAR:$(find "$ROOT/app/libs" -name '*.jar' | sort | tr '\n' ':')$TESTLIBS"
mkdir -p "$ROOT/build/android-tests" "$ROOT/verification/v2.4"
javac --release 8 -encoding UTF-8 -classpath "$CP" -d "$ROOT/build/android-tests" "$ROOT"/verification/android-runtime/src/test/java/*.java > "$ROOT/verification/v2.4/runtime-compile.log" 2>&1
java -Xmx1536m "-Dpelican.root=$ROOT" -Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -cp "$ROOT/build/android-tests:$CP" org.junit.runner.JUnitCore RuntimeSmokeTest PelicanRuntimeTest SqlStoreRuntimeTest LiveResponseReplayTest > "$ROOT/verification/v2.4/runtime-regression.log" 2>&1
cat "$ROOT/verification/v2.4/runtime-regression.log"
