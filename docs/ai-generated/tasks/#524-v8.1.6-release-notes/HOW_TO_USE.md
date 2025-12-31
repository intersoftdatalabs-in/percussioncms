# How to Use These Documents - Quick Start Guide

## For the Release Manager

### Step 1: Choose Your Starting Point

**If you have 5 minutes:**
→ Use `QUICK_REFERENCE_v8.1.6.md` - Copy and paste the corrections directly into the GitHub release

**If you have 10 minutes:**
→ Use `RELEASE_NOTES_v8.1.6_GITHUB_BODY.md` - Replace the entire release body with this clean version

**If you need details:**
→ Use `PR_REFERENCE_CORRECTIONS.md` - See exactly which PRs need updating and why

**If you want the full story:**
→ Use `RELEASE_NOTES_v8.1.6_CORRECTED.md` - Comprehensive documentation with all details

### Step 2: Access the Draft Release

Go to: https://github.com/intersoftdatalabs-in/percussioncms/releases/tag/untagged-4aa88975603d6010e701

### Step 3: Edit the Release Notes

Click "Edit" and choose one of these approaches:

#### Option A: Quick Edits (5 minutes)

Open `QUICK_REFERENCE_v8.1.6.md` and use the copy-paste corrections:

1. Find PR #405 in the release → Replace or remove (MyFaces 3.0.3 → rolled back)
2. Find PR #412 in the release → Replace or remove (Shindig 3.0.0-beta4 → rolled back)
3. Find PR #283 in the release → Update version (PDFBox 3.0.6 → 2.0.30)
4. Find PR #63 in the release → Update version (4.5.0-jakarta → 4.5.0)
5. Find PR #103 in the release → Update version (2.20 → 2.20.1)
6. Find PR #474 in the release → Remove (superseded by PR #511)

#### Option B: Complete Rewrite (10 minutes)

Copy the entire contents of `RELEASE_NOTES_v8.1.6_GITHUB_BODY.md` and paste it as the new release body.

### Step 4: Add Compatibility Note

Add this paragraph at the top of the release notes:

```markdown
## Important Note on Java 8 Compatibility

This release maintains full compatibility with JDK 1.8.0 (Java 8). Several 
attempted dependency upgrades to version 3.x were rolled back because they 
require Java 11 or higher. All dependencies listed below are confirmed to 
work with Java 8 and include the latest security updates and bug fixes 
available for Java 8-compatible versions.
```

### Step 5: Save as Draft or Publish

Review your changes and either:
- Save as draft for further review
- Publish the release

## For Technical Reviewers

### Verify the Corrections

1. Check `PR_REFERENCE_CORRECTIONS.md` for the complete list of changes
2. Compare against the actual pom.xml file (already verified)
3. Review `SUMMARY.md` for the executive summary

### Questions to Ask

- ✅ Are all incorrect version numbers corrected?
- ✅ Are rolled back PRs clearly marked?
- ✅ Is Java 8 compatibility clearly explained?
- ✅ Are users informed about why upgrades were rolled back?

## For Future Reference

These documents serve as historical reference for:
- Why certain dependencies were not upgraded
- The actual state of dependencies in v8.1.6
- Java 8 compatibility decisions
- Lessons learned for future Java 8 releases

Keep these documents in the repository for future release managers.

---

## Quick Verification Checklist

Before publishing the release, verify:

- [ ] MyFaces shown as 2.3.11 (not 3.0.3)
- [ ] Shindig shown as 1.1-BETA5-incubating (not 3.0.0-beta4)
- [ ] PDFBox shown as 2.0.30 (not 3.0.6)
- [ ] CSRF Guard shown as 4.5.0 (not 4.5.0-jakarta)
- [ ] Jackson shown as 2.20.1 (not just 2.20)
- [ ] ICU4J shown as 77.1 (using PR #511, not PR #474)
- [ ] Java 8 compatibility note included
- [ ] Rolled back updates clearly marked

---

**Need help?** Refer to the other documents in this directory for more details.
