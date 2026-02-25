#!/bin/bash

# Script to resolve merge conflicts by accepting "ours" (current branch changes)
# and rejecting "theirs" (incoming branch changes)

# Find all files with conflict markers (search all files, excluding .git)
echo "Finding files with merge conflict markers..."
files_with_conflicts=$(grep -rl "^>>>>>>> " . 2>/dev/null | grep -v ".git" | grep -v "resolve-conflicts.sh")

count=0
for file in $files_with_conflicts; do
    echo "Processing: $file"
    
    # Use perl to remove conflict markers and keep only "ours"
    # This removes:
    # - Lines with <<<<<<< HEAD
    # - Everything between ======= and >>>>>>> (including those markers)
    perl -i -pe '
        BEGIN { $in_conflict = 0; $in_theirs = 0; }
        if (/^<<<<<<< HEAD/) {
            $in_conflict = 1;
            $_ = "";
        } elsif (/^=======/ && $in_conflict) {
            $in_theirs = 1;
            $_ = "";
        } elsif (/^>>>>>>> / && $in_conflict) {
            $in_conflict = 0;
            $in_theirs = 0;
            $_ = "";
        } elsif ($in_theirs) {
            $_ = "";
        }
    ' "$file"
    
    ((count++))
done

echo ""
echo "Processed $count files"
echo "All conflicts resolved by accepting 'our' changes"
