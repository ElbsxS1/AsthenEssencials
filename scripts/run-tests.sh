#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
javac -d /tmp/asthen-test src/test/java/br/asthen/essentials/TestMain.java \
  src/main/java/br/asthen/essentials/core/*.java \
  src/main/java/br/asthen/essentials/util/*.java
java -cp /tmp/asthen-test br.asthen.essentials.TestMain
