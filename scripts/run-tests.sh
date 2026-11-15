#!/usr/bin/env bash
# Self-contained tests (JDK only): everything except the thin Paper layer in bukkit/.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${TMPDIR:-/tmp}/ae-test-classes"
rm -rf "$OUT" && mkdir -p "$OUT"
SOURCES=$(find src/main/java -name '*.java' ! -path '*/bukkit/*')
javac -Xlint:all,-serial --release 21 -d "$OUT" $SOURCES src/test/java/br/asthen/essentials/TestMain.java
java -cp "$OUT" br.asthen.essentials.TestMain
