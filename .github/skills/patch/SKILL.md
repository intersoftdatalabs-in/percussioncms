---
name: patch
description: "Use when the user needs to create, apply, verify, or revert patches for source files using the Linux diff and patch utilities, or when generating sed-based inline edits as a lightweight alternative."
---

# Linux Patch Manager

Manage file changes using the standard Linux `diff`, `patch`, and `sed` utilities.
This skill covers single-file patches, multi-file patches, directory diffs, Git-format
patches, and inline `sed` edits.

---

## 1. Creating Patches

### 1.1 Single-File Unified Diff

Always produce unified diffs (`-u`); they are the most readable format and the
standard expected by most tools.

```bash
diff -u original_file modified_file > changes.patch
```

### 1.2 Directory / Recursive Diff

For patching entire directory trees, use `-ruN`:

| Flag | Purpose |
|------|---------|
| `-r` | Recurse into subdirectories |
| `-u` | Unified format |
| `-N` | Treat absent files as empty (captures new/deleted files) |

```bash
diff -ruN original_dir/ modified_dir/ > changes.patch
```

### 1.3 Git-Format Patches

When working inside a Git repository, prefer `git diff` or `git format-patch`
because they embed commit metadata, binary diffs, and rename detection.

```bash
# Unstaged changes
git diff > changes.patch

# Staged changes
git diff --cached > changes.patch

# Between commits / branches
git diff <base>..<head> -- path/to/file > changes.patch

# One patch file per commit (for email / review workflows)
git format-patch -n <base>
```

### 1.4 Creating a Patch from `sed` Commands

For small, surgical edits (one or two lines), a `sed` one-liner is often simpler
than a full patch file. Document the intent in a comment.

```bash
# Replace the first occurrence of OLD with NEW on line 42
sed -i '42s/OLD/NEW/' path/to/file

# Insert text after line 176
sed -i '176a\    new line of code here' path/to/file

# Insert contents of another file after line 176
sed -i '176r /tmp/fragment.java' path/to/file

# Delete lines 10 through 15
sed -i '10,15d' path/to/file
```

> **Tip:** On macOS, `sed -i` requires an explicit backup extension
> (e.g., `sed -i '' ...`). On GNU/Linux, `sed -i` edits in place with no
> backup unless you add an extension (e.g., `sed -i.bak ...`).

---

## 2. Applying Patches

### 2.1 Standard Apply

```bash
patch < changes.patch
```

### 2.2 Applying with Path Stripping (`-p`)

Patches generated from a different directory depth require stripping leading path
components. The most common value is `-p1`, which removes the first directory
component (e.g., `a/src/Main.java` becomes `src/Main.java`).

```bash
patch -p1 < changes.patch
```

### 2.3 Applying to a Specific File or Directory

```bash
# Apply to a single target file
patch target_file < changes.patch

# Apply from a specific working directory
cd /path/to/project && patch -p1 < /tmp/changes.patch
```

### 2.4 Applying a Git-Format Patch

```bash
# Apply and create a commit with the original author/message
git am < 0001-fix-typo.patch

# Apply without committing
git apply changes.patch
```

---

## 3. Verifying Before Applying

**Always dry-run first** to confirm the patch applies cleanly.

```bash
patch --dry-run < changes.patch
```

For Git patches:

```bash
git apply --check changes.patch
```

If the dry run reports failures, inspect the reject files (`.rej`) or fuzz
warnings before proceeding.

---

## 4. Reverting (Reversing) a Patch

```bash
patch -R < changes.patch
```

For Git patches:

```bash
git apply -R changes.patch
```

---

## 5. Handling Conflicts and Rejects

When a patch does not apply cleanly:

1. **Check `.rej` files** — `patch` writes rejected hunks to `<file>.rej`.
2. **Review fuzz** — A fuzz factor > 0 means context lines were shifted;
   verify the result manually.
3. **Resolve manually** — Edit the target file to incorporate the rejected
   hunks, then delete the `.rej` file.
4. **Re-create the patch** if the target has diverged significantly.

To increase the allowed fuzz (use with caution):

```bash
patch --fuzz=3 < changes.patch
```

---

## 6. Common Options Reference

| Option | Description |
|--------|-------------|
| `-u` | Unified diff format (for `diff`) |
| `-r` | Recursive (for `diff`) |
| `-N` | Treat absent files as empty (for `diff`) |
| `-p<N>` | Strip `N` leading path components |
| `-b` | Create a `.orig` backup of each patched file |
| `--dry-run` | Test without modifying files |
| `-R` | Reverse (unapply) a patch |
| `--fuzz=<N>` | Allow `N` lines of fuzz when matching context |
| `-d <dir>` | Change to `<dir>` before applying |
| `--verbose` | Print detailed progress |
| `-o <file>` | Write output to `<file>` instead of patching in place |
| `-i <file>` | Read patch from `<file>` (alternative to stdin redirection) |

---

## 7. Workflow: Step-by-Step Patch Cycle

Follow this sequence for every patch operation:

1. **Generate** the patch (`diff -u` or `git diff`).
2. **Review** the patch contents (`cat changes.patch` or open in editor).
3. **Dry-run** to verify (`patch --dry-run < changes.patch`).
4. **Backup** original files (`patch -b < changes.patch` or `cp` manually).
5. **Apply** the patch (`patch -p1 < changes.patch`).
6. **Verify** the result (compile, run tests, inspect changed files).
7. **Clean up** backup and reject files when satisfied.

---

## 8. Best Practices

- **Unified format only** — Never use the default (context or normal) format;
  unified diffs are universally understood and easier to read.
- **Minimal scope** — Keep patches focused on a single logical change.
- **Descriptive filenames** — Name patch files to reflect their purpose
  (e.g., `fix-npe-in-asset-adaptor.patch`).
- **Always dry-run** before applying to catch offset or conflict issues early.
- **Backup originals** — Use `-b` or commit to version control before patching.
- **Prefer Git when possible** — `git diff` / `git apply` handle renames, binary
  files, and permissions better than plain `diff` / `patch`.
- **Avoid patching binary files** — `diff` and `patch` are designed for text;
  use Git LFS or binary-aware tools for non-text assets.
- **Document context** — When sharing a patch, include a description of what it
  changes and why (similar to a commit message).
- **Use `sed` for trivial edits** — For one-line substitutions or insertions,
  a `sed` command is faster and more transparent than a patch file.
- **Validate after apply** — Always compile and run relevant tests after
  applying a patch to confirm correctness.

---

## 9. Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `Hunk #N FAILED` | Target file has diverged from the patch context | Inspect `.rej`, resolve manually, or regenerate the patch |
| `can't find file to patch` | Wrong working directory or path strip level | Use `-p0`, `-p1`, etc., or `-d <dir>` to set the correct base |
| `Reversed (or previously applied) patch detected` | Patch was already applied | Confirm with `--dry-run`; use `-R` if you need to unapply |
| `patch unexpectedly ends in middle of line` | Missing trailing newline in patch file | Ensure patch file ends with a newline character |
| `Fuzz factor N` warning | Context lines shifted but hunk still matched | Verify the patched file manually; the match may be incorrect |
| Permission denied | Insufficient write access to target file | Run with appropriate permissions or fix file ownership |
