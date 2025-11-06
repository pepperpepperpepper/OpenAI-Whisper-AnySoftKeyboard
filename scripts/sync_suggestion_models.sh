#!/usr/bin/env bash
#
# Copies pre-downloaded language-model assets from the shared ~/suggestions workspace
# into the app's bundled assets tree. The .gitignore files under
# ime/app/src/main/assets/models/**/* ensure large binaries remain untracked while
# still letting local builds package them.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"${REPO_ROOT}/scripts/sync_suggestion_models_internal.sh"
