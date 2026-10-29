#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Use the caller's JDK and SDK instead of a machine-specific ARM installation.
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${ANDROID_JAR:-}" ]]; then
    if [[ -n "$SDK_ROOT" ]]; then ANDROID_JAR="$SDK_ROOT/platforms/android-35/android.jar";
    else ANDROID_JAR="$ROOT/tools/android-35/android.jar"; fi
fi
RESOURCE_JAR="${RESOURCE_JAR:-$ANDROID_JAR}"
R8_JAR="${R8_JAR:-$ROOT/tools/r8-8.3.37.jar}"
SIGN_DIR="${SIGN_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/pelican/signing}"
export ANDROID_JAR
printf 'Local signing directory: %s\n' "$SIGN_DIR"
echo 'A newly generated local key cannot update the official release APK.'
for tool in java javac jar keytool aapt2 zipalign apksigner python3; do command -v "$tool" >/dev/null || { echo "Missing tool: $tool"; exit 2; }; done
for file in "$ANDROID_JAR" "$RESOURCE_JAR" "$R8_JAR"; do test -f "$file" || { echo "Missing dependency: $file";exit 2; };done
mkdir -p "$ROOT/build" "$ROOT/verification/v2.4"
rm -rf "$ROOT/build/classes" "$ROOT/build/generated" "$ROOT/build/dex" "$ROOT/build/tests"
mkdir -p "$ROOT/build/"{classes,generated,dex,tests}
python3 - "$ROOT" <<'PY'
from pathlib import Path
import sys,re,xml.etree.ElementTree as ET
r=Path(sys.argv[1]);gradle=(r/'app/build.gradle').read_text();ns='{http://schemas.android.com/apk/res/android}';ET.register_namespace('android','http://schemas.android.com/apk/res/android')
tree=ET.parse(r/'app/src/main/AndroidManifest.xml');m=tree.getroot();m.set('package',re.search(r"applicationId '([^']+)'",gradle)[1]);m.set(ns+'versionCode',re.search(r'versionCode (\d+)',gradle)[1]);m.set(ns+'versionName',re.search(r"versionName '([^']+)'",gradle)[1]);m.insert(0,ET.Element('uses-sdk',{ns+'minSdkVersion':re.search(r'minSdk (\d+)',gradle)[1],ns+'targetSdkVersion':re.search(r'targetSdk (\d+)',gradle)[1]}));tree.write(r/'build/AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
PY
echo '[1/6] Compile resources'
aapt2 compile --dir "$ROOT/app/src/main/res" -o "$ROOT/build/compiled-resources.zip" > "$ROOT/verification/resource-build.log" 2>&1
aapt2 link -I "$RESOURCE_JAR" --manifest "$ROOT/build/AndroidManifest.xml" --java "$ROOT/build/generated" --custom-package app.pelican.check -A "$ROOT/app/src/main/assets" -o "$ROOT/build/resources.ap_" "$ROOT/build/compiled-resources.zip" >> "$ROOT/verification/resource-build.log" 2>&1
echo '[2/6] Compile Java sources'
find "$ROOT/app/src/main/java" "$ROOT/build/generated" -name '*.java' | sort > "$ROOT/build/sources.txt"
CP="$ANDROID_JAR:$(find "$ROOT/app/libs" -name '*.jar' | sort | tr '\n' ':')"
javac --release 8 -encoding UTF-8 -nowarn -classpath "$CP" -d "$ROOT/build/classes" @"$ROOT/build/sources.txt" > "$ROOT/verification/java-build.log" 2>&1
echo '[3/6] Run local regression checks'
javac --release 8 -encoding UTF-8 -classpath "$ROOT/build/classes:$CP" -d "$ROOT/build/tests" "$ROOT/verification/RecoverySmokeTest.java" "$ROOT/verification/PelicanRegressionTest.java" "$ROOT/verification/LocalTransportTest.java"
java -classpath "$ROOT/build/tests:$ROOT/build/classes:$CP" RecoverySmokeTest > "$ROOT/verification/previous-regression.log" 2>&1
java -Xmx768m -classpath "$ROOT/build/tests:$ROOT/build/classes:$CP" PelicanRegressionTest > "$ROOT/verification/pelican-regression.log" 2>&1
java -Xmx768m -classpath "$ROOT/build/tests:$ROOT/build/classes:$CP" LocalTransportTest > "$ROOT/verification/transport-regression.log" 2>&1
python3 "$ROOT/verification/check-contracts.py" > "$ROOT/verification/contracts.log" 2>&1
if [[ -f "$ROOT/tools/robolectric/robolectric-4.16.1.jar" ]];then bash "$ROOT/scripts/test-runtime.sh" > "$ROOT/verification/v2.4/runtime-build.log" 2>&1;fi
tail -2 "$ROOT/verification/previous-regression.log";tail -2 "$ROOT/verification/pelican-regression.log";tail -2 "$ROOT/verification/transport-regression.log";cat "$ROOT/verification/contracts.log"
echo '[4/6] Convert Java bytecode and dependencies to Android DEX'
jar cf "$ROOT/build/application-classes.jar" -C "$ROOT/build/classes" .
mapfile -t LIBS < <(find "$ROOT/app/libs" -name '*.jar' | sort)
java -Xmx1024m -cp "$R8_JAR" com.android.tools.r8.D8 --release --min-api 26 --lib "$ANDROID_JAR" --output "$ROOT/build/dex" "$ROOT/build/application-classes.jar" "${LIBS[@]}" > "$ROOT/verification/dex-build.log" 2>&1
python3 - "$ROOT" <<'PY'
import sys,zipfile
from pathlib import Path
r=Path(sys.argv[1]);seen=set()
with zipfile.ZipFile(r/'build/resources.ap_') as src,zipfile.ZipFile(r/'build/unsigned.apk','w',zipfile.ZIP_DEFLATED) as out:
 for entry in src.infolist():out.writestr(entry,src.read(entry.filename));seen.add(entry.filename)
 for path in sorted((r/'build/dex').glob('classes*.dex')):out.write(path,path.name);seen.add(path.name)
 for path in sorted((r/'app/libs').glob('*.jar')):
  with zipfile.ZipFile(path) as jar:
   for info in jar.infolist():
    n=info.filename;upper=n.upper()
    if info.is_dir() or n.endswith('.class') or upper=='META-INF/MANIFEST.MF' or upper.endswith(('.SF','.RSA','.DSA')) or n in seen:continue
    out.writestr(n,jar.read(n));seen.add(n)
print('Packaged',len(seen),'APK entries including required runtime resources')
PY
echo '[5/6] Align and sign with the configured local key'
mkdir -p "$SIGN_DIR";chmod 700 "$SIGN_DIR"
if [[ ! -e "$SIGN_DIR/pelican.p12" ]]; then
    [[ ! -e "$SIGN_DIR/store-password.txt" ]] || { echo 'Password exists without matching key; refusing to overwrite';exit 3; }
    python3 - "$SIGN_DIR/store-password.txt" <<'PY'
import secrets,sys,os
p=sys.argv[1];fd=os.open(p,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
with os.fdopen(fd,'w') as f:f.write(secrets.token_urlsafe(32)+'\n')
PY
    keytool -genkeypair -keystore "$SIGN_DIR/pelican.p12" -storetype PKCS12 -storepass:file "$SIGN_DIR/store-password.txt" -keypass:file "$SIGN_DIR/store-password.txt" -alias pelican -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Pelican Local Build,O=Pelican,C=ZZ' > "$ROOT/verification/key-creation.log" 2>&1
    chmod 600 "$SIGN_DIR/pelican.p12"
fi
zipalign -f 4 "$ROOT/build/unsigned.apk" "$ROOT/build/aligned.apk"
apksigner sign --ks "$SIGN_DIR/pelican.p12" --ks-key-alias pelican --ks-pass "file:$SIGN_DIR/store-password.txt" --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false --out "$ROOT/build/Pelican.apk" "$ROOT/build/aligned.apk"
echo '[6/6] Verify final installation artifact'
apksigner verify --verbose --print-certs "$ROOT/build/Pelican.apk" > "$ROOT/verification/apk-signature.log" 2>&1
zipalign -c -v 4 "$ROOT/build/Pelican.apk" > "$ROOT/verification/apk-alignment.log" 2>&1
aapt2 dump badging "$ROOT/build/Pelican.apk" > "$ROOT/verification/apk-badging.log"
(cd "$ROOT/build" && sha256sum Pelican.apk > Pelican.apk.sha256)
grep -E 'Verified using|Signer #1 certificate SHA-256' "$ROOT/verification/apk-signature.log"
grep -E 'package:|sdkVersion|targetSdkVersion|application-label:' "$ROOT/verification/apk-badging.log" | head -5
ls -lh "$ROOT/build/Pelican.apk";cat "$ROOT/build/Pelican.apk.sha256"
