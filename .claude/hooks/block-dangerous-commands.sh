#!/bin/bash
set -euo pipefail

INPUT=$(cat)
COMMAND=$(printf '%s' "$INPUT" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("tool_input", {}).get("command", ""))')

if printf '%s' "$COMMAND" | grep -Eq '(^|[;&|[:space:]])rm[[:space:]]+-rf[[:space:]]+(/|~|\$HOME)([[:space:]]|$)|git[[:space:]]+push[[:space:]].*(-f|--force)|git[[:space:]]+reset[[:space:]]+--hard|supabase[[:space:]]+db[[:space:]]+reset'; then
  python3 - <<'PY'
import json
print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": "Dangerous command blocked by Strakk project hook.",
    }
}))
PY
  exit 0
fi

exit 0
