#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

exec java -jar "$SRC_DIR/runTodoManagerRestAPI-1.5.5.jar" "$@"
