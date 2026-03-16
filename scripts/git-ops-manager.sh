#!/usr/bin/env bash
set -euo pipefail

# Lightweight wrapper to centralize git operations per AGENTS.md policy.
# Usage examples:
#   scripts/git-ops-manager.sh commit "feat: message"
#   scripts/git-ops-manager.sh push main

cmd=${1:-}
case "$cmd" in
  commit)
    msg=${2:-"chore: update"}
    git add -A
    git commit -m "$msg" || echo "Nothing to commit" ;;
  push)
    branch=${2:-"main"}
    # Use the configured 'origin' which points to ssh Host github-sibiu
    git push origin "$branch" ;;
  status)
    git status ;;
  log)
    git --no-pager log --oneline -n 10 ;;
  *)
    echo "git-ops-manager: commands: commit <msg> | push [branch] | status | log" ;;
esac

