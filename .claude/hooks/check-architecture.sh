#!/bin/bash
set -euo pipefail

cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"

DOMAIN_DIR="shared/src/commonMain/kotlin/com/strakk/shared/domain"
PRESENTATION_DIR="shared/src/commonMain/kotlin/com/strakk/shared/presentation"

fail() {
  printf '%s\n' "$1"
  exit 1
}

if [ -d "$DOMAIN_DIR" ]; then
  if grep -R -nE '^import com\.strakk\.shared\.data\.' "$DOMAIN_DIR" >/tmp/strakk-domain-data-imports 2>/dev/null; then
    cat /tmp/strakk-domain-data-imports
    fail "ARCHITECTURE VIOLATION: domain/ imports from data/."
  fi
  if grep -R -nE '^import com\.strakk\.shared\.presentation\.' "$DOMAIN_DIR" >/tmp/strakk-domain-presentation-imports 2>/dev/null; then
    cat /tmp/strakk-domain-presentation-imports
    fail "ARCHITECTURE VIOLATION: domain/ imports from presentation/."
  fi
  if grep -R -nE '^import (android\.|androidx\.|io\.ktor\.|io\.github\.jan\.supabase\.|org\.koin\.|kotlinx\.serialization\.)' "$DOMAIN_DIR" >/tmp/strakk-domain-framework-imports 2>/dev/null; then
    cat /tmp/strakk-domain-framework-imports
    fail "ARCHITECTURE VIOLATION: domain/ imports framework or data-layer libraries."
  fi
fi

if [ -d "$PRESENTATION_DIR" ]; then
  if grep -R -nE '^import com\.strakk\.shared\.data\.' "$PRESENTATION_DIR" >/tmp/strakk-presentation-data-imports 2>/dev/null; then
    cat /tmp/strakk-presentation-data-imports
    fail "ARCHITECTURE VIOLATION: presentation/ imports from data/."
  fi
fi

exit 0
