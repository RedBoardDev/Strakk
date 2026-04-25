#!/bin/bash
set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(printf '%s' "$INPUT" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("tool_input", {}).get("file_path", ""))')

case "$FILE_PATH" in
  *.swift)
    if command -v swiftlint >/dev/null 2>&1; then
      swiftlint lint --fix "$FILE_PATH" >/dev/null 2>&1 || true
    fi
    ;;
esac

exit 0
