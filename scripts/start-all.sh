#!/usr/bin/env bash
# macOS / Linux: build and start every service in the background (logs in ./logs), then the frontend.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || cp .env.example .env
./scripts/build-backend.sh
mkdir -p logs
for s in eureka-server auth-service tutor-service booking-service notification-service api-gateway; do
  java -Xmx384m -jar "backend/$s/target/$s-1.0.0.jar" > "logs/$s.log" 2>&1 &
  echo "started $s (log: logs/$s.log)"
  sleep 2
done
cd frontend && { [ -d node_modules ] || npm install; } && npm run dev
