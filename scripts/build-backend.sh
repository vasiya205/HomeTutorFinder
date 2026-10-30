#!/usr/bin/env bash
# macOS / Linux: build all backend services.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -f backend/pom.xml -B -ntp -DskipTests package
