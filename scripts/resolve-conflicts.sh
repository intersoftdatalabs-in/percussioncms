#!/usr/bin/env bash

set -euo pipefail

echo "Scanning index for unresolved merge conflicts..."

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Error: this script must be run inside a Git repository." >&2
    exit 1
fi

mapfile -d '' conflicted_files < <(git diff --name-only --diff-filter=U -z)

if [ "${#conflicted_files[@]}" -eq 0 ]; then
    echo "No unresolved merge conflicts found."
    exit 0
fi

echo "Resolving ${#conflicted_files[@]} conflicted file(s) by accepting ours..."

resolved=0
failed=0

for file in "${conflicted_files[@]}"; do
    echo "Processing: $file"

    # Stage 2 is the "ours" side. If it does not exist, our side deleted this file.
    if git show ":2:$file" >/dev/null 2>&1; then
        if git checkout --ours -- "$file" >/dev/null 2>&1 && git add -- "$file"; then
            resolved=$((resolved + 1))
        else
            echo "Warning: failed to resolve with ours: $file" >&2
            failed=$((failed + 1))
        fi
    else
        if git rm --force -- "$file" >/dev/null 2>&1 || git add -u -- "$file" >/dev/null 2>&1; then
            resolved=$((resolved + 1))
        else
            echo "Warning: failed to stage deletion for ours: $file" >&2
            failed=$((failed + 1))
        fi
    fi
done

remaining_conflicts=$(git diff --name-only --diff-filter=U | wc -l | tr -d '[:space:]')

echo
echo "Resolved: $resolved"
echo "Failed: $failed"
echo "Remaining unresolved conflicts: $remaining_conflicts"

if [ "$failed" -gt 0 ] || [ "$remaining_conflicts" -gt 0 ]; then
    echo "Some conflicts could not be resolved automatically. Review with: git status" >&2
    exit 1
fi

echo "Done. All current merge conflicts were resolved using our local version."
echo "Review with: git status"
