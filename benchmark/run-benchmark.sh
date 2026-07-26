#!/usr/bin/env bash
#
# Runs the benchmark against a Slimefun build's classes directory.
#
# Usage:
#   ./run-benchmark.sh <sf-classes-dir> <label>
#
# Example:
#   ./run-benchmark.sh ../target/classes 4.9.3-optimized
#   ./run-benchmark.sh ../../sf-4.9.2-baseline/target/classes 4.9.2-baseline
#
# The classes directory is packaged into a jar (system-scope dependency
# "slimefun-under-test") so it lands on the compile classpath. The benchmark
# is then executed with plain `java -cp ...` so that everything lives on the
# system class loader - required because MockBukkit's plugin class loader
# delegates to the system class loader (this mirrors how the project's own
# surefire tests run). Results are written to report/results-<label>.txt.
#
set -euo pipefail
cd "$(dirname "$0")"

SF_CLASSES="${1:-../target/classes}"
LABEL="${2:-current}"
SF_JAR="target/slimefun-under-test-$LABEL.jar"
# Maven requires an absolute Windows path for system-scope dependencies.
SF_JAR_ABS="$(cygpath -m "$PWD/$SF_JAR")"

mkdir -p target report

# Locate the JDK's jar tool via java.home (the jar binary is frequently not
# on PATH on Windows, and the Oracle javapath shim does not resolve with -L).
JAVA_HOME_DETECTED="$(java -XshowSettings:properties -version 2>&1 | grep -E 'java\.home' | sed -E 's/.*=[[:space:]]*//' | tr -d '\r' | head -1)"
JAR="$(echo "$JAVA_HOME_DETECTED" | sed 's|\\\\|/|g')/bin/jar.exe"

# Package the unshaded Slimefun classes (including plugin.yml) as a jar.
rm -f "$SF_JAR"
"$JAR" cf "$SF_JAR" -C "$SF_CLASSES" .

# Compile the harness and resolve the full dependency classpath
# (includes the system-scope slimefun-under-test jar).
mvn -q compile -Dsf.jar="$SF_JAR_ABS"
mvn -q dependency:build-classpath -Dsf.jar="$SF_JAR_ABS" -Dmdep.outputFile=target/cp.txt

CP="target/classes;$(cat target/cp.txt)"
java -cp "$CP" benchmark.BenchMain "$LABEL" "report/results-$LABEL.txt"
