# !/bin/bash

# README for WebUI Phase 1 Migration Scripts

# 

# This file is meant to be read, not executed. It provides guidance on running

# the Phase 1 migration scripts in the correct order.

# 

# Generated: 2026-03-05

# Project: Percussion CMS - WebUI Module Refactoring

cat << 'EOF'
============

PHASE 1 MIGRATION: Script Execution Guide
=========================================

This directory contains automated scripts to safely execute Phase 1 of the WebUI
source layout migration. Phase 1 includes:

1. Updating JSP/HTML/CSS/JS reference paths
2. Moving source files to new structure (src/main/webapp/cm/)
3. Moving build configuration to src/main/frontend/
4. Updating pom.xml configuration

================================================================================
PREREQUISITES
=============

Before running any scripts:

1. Ensure you are in the WebUI directory:
   cd /home/nate/projects/percussioncms/WebUI

2. Make sure all work is committed:
   git status                    # should be clean
   git add . && git commit -m "checkpoint"

3. Create a feature branch:
   git checkout -b feature/webui-src-layout-refactor

4. Make scripts executable:
   chmod +x ../docs/ai-generated/tasks/#000-webui-src-layout/*.sh

================================================================================
STEP-BY-STEP EXECUTION
======================

STEP 1: Validate Current State
==============================

Purpose: See what files will be affected before making changes

Script: phase-1-validate-changes.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/phase-1-validate-changes.sh

Output: Shows file counts that will be modified
Time:   ~2 minutes

Example:
$ bash phase-1-validate-changes.sh
JSP Files: 45
HTML Files: 12
CSS Files: 18
JavaScript Files: 67
Files with '/cm/jslib/' pattern: 45
...

STEP 2: Test on Single JSP File
===============================

Purpose: Run migration on one small JSP file to verify sed patterns work
Shows diff preview before applying

Script: phase-1-test-single.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/phase-1-test-single.sh

Output: Shows before/after diff for careful inspection
Time:   ~5 minutes
Action: User reviews diff, then confirms (1) or rejects (2)

Example:
$ bash phase-1-test-single.sh
Test file: war/app/includes/common_js.jsp

Original content (first 20 lines with /cm/ paths):
--------------------------------------------------

  <script src="/cm/jslib/jquery/jquery-3.6.0.js"></script>
  <script src="/cm/jslib/bootstrap/5.3.8/js/bootstrap.js"></script>
  ...

Changes preview (diff):
-----------------------

- 

  <script src="/cm/jslib/jquery/jquery-3.6.0.js"></script>

+ 

  <script src="/cm/app/js/legacy/jquery/jquery-3.6.0.js"></script>

...

VALIDATION REQUIRED:
Option 1: APPROVE - Accept and apply to test file
Option 2: REJECT - Restore backup and exit
Enter 1 (approve) or 2 (reject): 1

✅ Changes applied! Test file updated: war/app/includes/common_js.jsp

Next steps:
1. Test in browser: Load the page that uses this JSP
2. Check browser console for any 404 errors
3. Verify CSS and JS load correctly
4. Once confirmed, run: bash phase-1-migrate-all-files.sh

IMPORTANT: After running this step, TEST IN BROWSER before proceeding!
- Load a page that uses the updated JSP
- Check browser console (F12 → Console tab)
- Verify no 404 errors for CSS/JS
- If successful, proceed to Step 3
- If errors, revert: cp war/app/includes/common_js.jsp.backup war/app/includes/common_js.jsp

STEP 3: Migrate All Files (Path References)
===========================================

Purpose: Apply path reference updates to ALL JSP, HTML, CSS, and JS files

Script: phase-1-migrate-all-files.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-all-files.sh

Precondition: Step 2 test must have shown positive results in browser

Output: Shows total files modified and statistics
Creates *.pre-phase1 backups for each changed file
Time:   ~5 minutes

Example:
$ bash phase-1-migrate-all-files.sh
🔄 Migrating reference paths in JSP, HTML, CSS, JS files...
Processing JSP files...
✓ war/app/includes/common_js.jsp (15 changes)
✓ war/app/admin.jsp (3 changes)
...

Migration Complete

Statistics:
Total files modified: 67
Total line changes: 284

Next steps:
1. Run: ./mvnw -f WebUI/pom.xml clean compile
2. Check that JSPs resolve without errors
3. Load one page in browser and verify CSS/JS load
4. If all looks good, proceed to Phase 1 Part 2

STEP 4: Migrate File Structure
==============================

Purpose: Move files from war/ to new src/main/webapp/cm/ structure
This is a critical step - large file operations

Script: phase-1-migrate-structure.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-structure.sh

Precondition: Step 3 must be complete (paths updated)
All changes should be committed to git

Output: Creates new directory structure
Copies and validates files to new locations
Creates *.pre-move backups in old locations
Time:   ~10 minutes (depends on file count)

Example:
$ bash phase-1-migrate-structure.sh
⚠️  IMPORTANT: This will move files. Ensure you have committed all changes first!
Continue? (yes/no): yes

📁 Creating directory structure under src/main/webapp/cm/...
✅ Directory structure created

Moving vendor JavaScript libraries...
✓ Moved from war/jslib/*

Moving application CSS...
✓ Moved from war/css/*

... [many more moves] ...

Migration Complete

Statistics:
Files in src/main/webapp/cm/: 256
Files in war/ (excluding backups): 8
✅ Files successfully moved

STEP 5: Migrate Build Configuration
===================================

Purpose: Move package.json, vite configs, scripts to src/main/frontend/
Prepare for Phase 2 build configuration updates

Script: phase-1-migrate-build-config.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-build-config.sh

Output: Copies build config files to new location
Creates *.pre-move backups
Time:   ~2 minutes

Example:
$ bash phase-1-migrate-build-config.sh
📁 Creating src/main/frontend directory structure...
✅ Directory structure created

Moving build configuration files...
✓ package.json
✓ package-lock.json
✓ vite.config.ts
✓ vite.legacy.config.ts
✓ scripts/build-legacy-bundles.js

✅ 5 files moved to src/main/frontend/

NEXT: Update pom.xml manually

Required pom.xml changes:
1. Update frontend-maven-plugin workingDirectory:
FROM: <workingDirectory>${project.basedir}</workingDirectory>
TO:   <workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>

2. Update maven-war-plugin webResources:
   FROM: <directory>war</directory>
   TO:   <directory>src/main/webapp</directory>

STEP 6: Update pom.xml
======================

Purpose: Update Maven configuration to point to new directories

Actions: Edit WebUI/pom.xml

Changes needed:

A) Update frontend-maven-plugin workingDirectory:

FIND:
<plugin>
<groupId>com.github.eirslett</groupId>
<artifactId>frontend-maven-plugin</artifactId>
<configuration>
<workingDirectory>${project.basedir}</workingDirectory>

REPLACE WITH:
<plugin>
<groupId>com.github.eirslett</groupId>
<artifactId>frontend-maven-plugin</artifactId>
<configuration>
<workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>

B) Update maven-war-plugin source:

FIND:
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-war-plugin</artifactId>
<configuration>
<webResources>
<resource>
<directory>war</directory>

REPLACE WITH:
<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-war-plugin</artifactId>
<configuration>
<webResources>
<resource>
<directory>src/main/webapp</directory>

Time: ~5-10 minutes (manual)

After editing, validate:
git diff pom.xml        # review changes
git add pom.xml
git commit -m "phase-1: update pom.xml for new paths"

STEP 7: Validate Migration
==========================

Purpose: Check that Phase 1 is complete and everything is in place

Script: check-migration-status.sh
Usage:  bash ../docs/ai-generated/tasks/#000-webui-src-layout/check-migration-status.sh

Output: Status report showing what's complete and what remains
Time:   ~1 minute

Example:
$ bash check-migration-status.sh

PHASE 1 PART 1: Reference Path Updates
✅ COMPLETED - All old paths updated

PHASE 1 PART 2: File Structure Migration
✅ New structure created: src/main/webapp/cm/

PHASE 1 PART 3: Build Configuration Migration
✅ Build config files moved to src/main/frontend/

PHASE 1 PART 4: pom.xml Updates
✅ frontend-maven-plugin workingDirectory -> src/main/frontend
✅ maven-war-plugin webResources -> src/main/webapp

✅ PHASE 1 STATUS: COMPLETE

Next: Phase 2 - Build Output Separation

STEP 8: Test Build
==================

Purpose: Verify that Maven build works with new structure

Commands:
./mvnw -f WebUI/pom.xml clean compile
./mvnw -f WebUI/pom.xml clean package

Expected output:
- No compilation errors
- No errors in frontend-maven-plugin execution
- WAR file created: target/WebUI-*.war

If errors occur:
- Check that frontend-maven-plugin can find package.json in src/main/frontend/
- Ensure Vite configs updated (covered in Phase 2)
- Check maven-war-plugin can find source in src/main/webapp/

================================================================================
CLEANUP & ROLLBACK
==================

Backup files created:
*.pre-phase1     - Backups from path reference updates
*.pre-move       - Backups from structural moves

To revert changes:
- Single file:  cp file.ext.pre-phase1 file.ext
- All backups:  find . -name "*.pre-phase1" -o -name "*.pre-move" | xargs ...

To clean up backups (after validation):
find war -name "*.pre-phase1" -delete
find war -name "*.pre-move" -delete

================================================================================
COMMON ISSUES & SOLUTIONS
=========================

Issue: "sed command not found"
Solution: These are standard shell scripts for Linux/macOS.
On Windows, use WSL or Git Bash.

Issue: "Permission denied" when running scripts
Solution: chmod +x phase-1-*.sh check-migration-status.sh

Issue: Browser shows 404 for CSS/JS after test
Solution: Path references may not have been updated correctly.
Check grep results for remaining old paths.

Issue: Maven build fails with "package.json not found"
Solution: Ensure frontend-maven-plugin workingDirectory updated in pom.xml
Verify src/main/frontend/package.json exists

Issue: Files not found in src/main/webapp/cm/
Solution: Check phase-1-migrate-structure.sh completed successfully
Verify no errors in the script output

================================================================================
SUCCESS CRITERIA (Phase 1 Complete)
===================================

✓ All old path references updated (/cm/jslib/ → /cm/app/js/legacy/, etc.)
✓ All source files moved to src/main/webapp/cm/
✓ Build configuration moved to src/main/frontend/
✓ pom.xml updated to point to new locations
✓ Maven build succeeds: ./mvnw -f WebUI/pom.xml clean package
✓ Browser test confirms pages load with CSS/JS intact
✓ No 404 errors in browser console

================================================================================
NEXT PHASE
==========

After Phase 1 is complete and validated, proceed to:

PHASE 2: Build Output Separation
- Update Vite output paths to target/generated-webui/cm/
- Update build-legacy-bundles.js output path
- Update maven-war-plugin overlay configuration
- Test that generated outputs go to target/ instead of source folders

Documentation: See webui-src-layout-migration-plan.md Phase 2 section

================================================================================
EOF
