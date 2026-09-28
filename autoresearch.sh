#!/usr/bin/env bash
# Docs quality benchmark: lint + spec-consistency checks over all markdown.
# Prints METRIC issues=<n> and METRIC score=<0..1> (higher is better).
set -euo pipefail
cd "$(dirname "$0")"
python3 scripts/doc_check.py
