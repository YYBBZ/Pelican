#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA=/usr/lib/jvm/java-17-openjdk-arm64/bin
TESTLIBS=$(find "$ROOT/tools/robolectric" -name '*.jar' ! -name 'android-all*' | sort | tr '\n' ':')
CP="$ROOT/build/classes:$ROOT/../PelicanCheck-RecoveryTools/android-platform-35/android.jar:$(find "$ROOT/app/libs" -name '*.jar' | sort | tr '\n' ':')$TESTLIBS"
mkdir -p "$ROOT/build/android-tests"
"$JAVA/javac" --release 8 -encoding UTF-8 -classpath "$CP" -d "$ROOT/build/android-tests" "$ROOT"/verification/android-runtime/src/test/java/*.java > "$ROOT/verification/v2.4/runtime-compile.log" 2>&1
"$JAVA/java" -Xmx1536m -Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -cp "$ROOT/build/android-tests:$CP" org.junit.runner.JUnitCore RuntimeSmokeTest PelicanRuntimeTest SqlStoreRuntimeTest LiveResponseReplayTest > "$ROOT/verification/v2.4/runtime-regression.log" 2>&1
cat "$ROOT/verification/v2.4/runtime-regression.log"
