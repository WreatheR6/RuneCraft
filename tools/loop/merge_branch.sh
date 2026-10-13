#!/usr/bin/env bash
# Merges a finished work branch into the current branch, verifies, commits regenerated files and pushes.
#   tools/loop/merge_branch.sh <branch>
# Exit codes: 2 = code conflicts to resolve by hand (then commit and run tools/loop/verify.sh),
#             3+ = a verification step failed (see tools/loop/verify.sh).
# Conflicts confined to src/main/resources are taken from the branch: those files are regenerated anyway.
# Commit trailers (attribution lines) can be supplied with the COMMIT_TRAILERS environment variable.
set -uo pipefail
cd "$(dirname "$0")/../.."
BR="$1"
TARGET=$(git rev-parse --abbrev-ref HEAD)
msg() { printf '%s\n\n%s' "$1" "${COMMIT_TRAILERS:-}"; }
git merge --no-edit "$BR" 2>&1 | grep -i conflict
if git diff --name-only --diff-filter=U | grep -q .; then
  if git diff --name-only --diff-filter=U | grep -v '^src/main/resources/'; then
    echo "CODE CONFLICTS - resolve manually"; exit 2
  fi
  git checkout --theirs -- src/main/resources/ 2>/dev/null; git add -A
  git commit -qm "$(msg "Merge $BR (generated resources taken from branch)")"
fi
bash tools/loop/verify.sh || exit $?
git add -A
git diff --cached --quiet || git commit -qm "$(msg "Regenerate assets after merging $BR")"
git push -q -u origin "$TARGET" 2>&1 | tail -1
git log --oneline -1
