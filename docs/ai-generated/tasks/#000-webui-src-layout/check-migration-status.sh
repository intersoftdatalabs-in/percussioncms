#!/bin/bash
################################################################################
# Migration Status Checker
#
# This script checks the current state of the migration and reports:
# - What phases have been completed
# - What still needs to be done
# - File counts and validation
#
# Usage: bash check-migration-status.sh [webui_dir]
# Default webui_dir: current directory
################################################################################

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

echo "=================================="
echo "WebUI Migration Status Report"
echo "=================================="
echo ""
echo "Generated: $(date)"
echo ""

################################################################################
# Phase 1 Part 1: Reference Updates
################################################################################

echo "📋 PHASE 1 PART 1: Reference Path Updates"
echo "========================================="
echo ""

# Check for old patterns
OLD_JSLIB_COUNT=$(grep -r "/cm/jslib/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" 2>/dev/null | wc -l || echo "0")
OLD_CSS_COUNT=$(grep -r "/cm/css/" war --include="*.jsp" --include="*.html" --include="*.css" 2>/dev/null | wc -l || echo "0")

if [[ "$OLD_JSLIB_COUNT" -eq 0 ]] && [[ "$OLD_CSS_COUNT" -eq 0 ]]; then
    echo "✅ COMPLETED - All old paths updated"
    echo "   - No /cm/jslib/ references found"
    echo "   - No /cm/css/ references found"
else
    echo "⏳ IN PROGRESS or NOT STARTED:"
    echo "   - Old /cm/jslib/ patterns: $OLD_JSLIB_COUNT"
    echo "   - Old /cm/css/ patterns: $OLD_CSS_COUNT"
    echo ""
    echo "Action: Run 'bash phase-1-test-single.sh' then 'bash phase-1-migrate-all-files.sh'"
fi

echo ""

################################################################################
# Phase 1 Part 2: Structural Migration
################################################################################

echo "📋 PHASE 1 PART 2: File Structure Migration"
echo "=========================================="
echo ""

# Check if new directory structure exists
if [[ -d "src/main/webapp/cm" ]]; then
    NEW_WEBAPP_COUNT=$(find "src/main/webapp/cm" -type f ! -name ".git*" 2>/dev/null | wc -l)
    echo "✅ New structure created: src/main/webapp/cm/"
    echo "   Files in new location: $NEW_WEBAPP_COUNT"

    # Check for remaining files in old war/ location (excluding generated)
    OLD_SRC_COUNT=$(find "war" -type f ! -name "*.pre-move" ! -name "*.pre-phase1" ! -path "*/jslibMin/*" ! -path "*/cssMin/*" ! -path "*/modern/*" 2>/dev/null | wc -l)
    if [[ "$OLD_SRC_COUNT" -eq 0 ]]; then
        echo "   ✓ All source files moved from war/"
    else
        echo "   ⚠️  $OLD_SRC_COUNT files still in war/ (should be only generated files)"
    fi
else
    echo "⏳ NOT STARTED - Structure not created yet"
    echo ""
    echo "Action: Run 'bash phase-1-migrate-structure.sh'"
fi

echo ""

################################################################################
# Phase 1 Part 3: Build Configuration
################################################################################

echo "📋 PHASE 1 PART 3: Build Configuration Migration"
echo "================================================"
echo ""

# Check if build files moved
FRONTEND_DIR_EXISTS=0
if [[ -d "src/main/frontend" ]]; then
    FRONTEND_DIR_EXISTS=1
    FRONTEND_COUNT=$(ls "src/main/frontend" | wc -l)
    echo "✅ Build config files moved to src/main/frontend/"
    echo "   Files in src/main/frontend: $FRONTEND_COUNT"

    [[ -f "src/main/frontend/package.json" ]] && echo "   ✓ package.json"
    [[ -f "src/main/frontend/vite.config.ts" ]] && echo "   ✓ vite.config.ts"
    [[ -f "src/main/frontend/vite.legacy.config.ts" ]] && echo "   ✓ vite.legacy.config.ts"
else
    echo "⏳ NOT STARTED - Build files not moved yet"
    echo ""
    echo "Action: Run 'bash phase-1-migrate-build-config.sh'"
fi

echo ""

################################################################################
# POM.XML Configuration
################################################################################

echo "📋 PHASE 1 PART 4: pom.xml Updates"
echo "=================================="
echo ""

# Check frontend-maven-plugin config
WORKING_DIR=$(grep -A 5 "frontend-maven-plugin" pom.xml 2>/dev/null | grep "workingDirectory" | head -1 || echo "")

if [[ "$WORKING_DIR" =~ "src/main/frontend" ]]; then
    echo "✅ frontend-maven-plugin workingDirectory -> src/main/frontend"
else
    echo "⏳ frontend-maven-plugin workingDirectory not updated yet"
    echo "   Current setting: $(echo "$WORKING_DIR" | sed 's/<[^>]*>//g' | xargs)"
    if [[ ! -z "$WORKING_DIR" ]]; then
        echo "   Update to: \${project.basedir}/src/main/frontend"
    fi
fi

# Check maven-war-plugin source
WAR_SOURCE=$(grep -A 3 "maven-war-plugin" pom.xml 2>/dev/null | grep "<directory>" | head -1 || echo "")

if [[ "$WAR_SOURCE" =~ "src/main/webapp" ]] || [[ "$WAR_SOURCE" =~ "src/main/resources" ]]; then
    echo "✅ maven-war-plugin webResources -> src/main/webapp"
else
    echo "⏳ maven-war-plugin webResources source not updated yet"
    if [[ ! -z "$WAR_SOURCE" ]]; then
        echo "   Current setting: $(echo "$WAR_SOURCE" | sed 's/<[^>]*>//g' | xargs)"
    fi
fi

echo ""

################################################################################
# Directory Structure Overview
################################################################################

echo "📁 Current Directory Structure"
echo "============================="
echo ""

echo "Source files:"
if [[ -d "src/main/webapp/cm" ]]; then
    echo "  src/main/webapp/cm/"
    find "src/main/webapp/cm" -maxdepth 1 -type d ! -name "cm" 2>/dev/null | sed 's|^|    ├─ |' | sed 's|.*/|  |'
    DIR_COUNT=$(find "src/main/webapp/cm" -maxdepth 1 -type d ! -name "cm" 2>/dev/null | wc -l)
    echo "    └─ ($DIR_COUNT top-level directories)"
else
    echo "  ⚠️  src/main/webapp/ not yet created"
fi

echo ""

if [[ $FRONTEND_DIR_EXISTS -eq 1 ]]; then
    echo "  src/main/frontend/"
    ls -d "src/main/frontend"/* 2>/dev/null | sed 's|^|    ├─ |' | sed 's|.*/|  |'
else
    echo "  ⚠️  src/main/frontend/ not yet created"
fi

echo ""

echo "Generated files (WAR packages):"
echo "  war/"
[[ -d "war/jslibMin" ]] && echo "    ├─ jslibMin/ (legacy bundle outputs)"
[[ -d "war/cssMin" ]] && echo "    ├─ cssMin/ (legacy CSS outputs)"
[[ -d "war/modern" ]] && echo "    ├─ modern/ (React build output)"
[[ -d "war/WEB-INF" ]] && echo "    └─ WEB-INF/ (web config)"

echo ""

################################################################################
# Summary and Next Steps
################################################################################

PHASE_COMPLETE=0
if [[ "$OLD_JSLIB_COUNT" -eq 0 ]] && [[ "$OLD_CSS_COUNT" -eq 0 ]] && [[ "$FRONTEND_DIR_EXISTS" -eq 1 ]] && [[ -d "src/main/webapp/cm" ]]; then
    PHASE_COMPLETE=1
fi

echo "=================================="
echo "Summary"
echo "=================================="
echo ""

if [[ $PHASE_COMPLETE -eq 1 ]]; then
    echo "✅ PHASE 1 STATUS: MOSTLY COMPLETE"
    echo ""
    echo "Remaining: Update pom.xml if not done already"
    echo ""
    echo "Next: Phase 2 - Build Output Separation"
    echo "  1. Update Vite output paths"
    echo "  2. Update maven-war-plugin overlay"
    echo "  3. Update build-legacy-bundles.js output path"
else
    echo "⏳ PHASE 1 STATUS: IN PROGRESS"
    echo ""
    echo "Remaining steps:"
    if [[ "$OLD_JSLIB_COUNT" -gt 0 ]]; then
        echo "  • Update path references in JSP/HTML/CSS files"
    fi
    if [[ ! -d "src/main/webapp/cm" ]]; then
        echo "  • Migrate file structure to src/main/webapp/cm/"
    fi
    if [[ $FRONTEND_DIR_EXISTS -eq 0 ]]; then
        echo "  • Move build config files to src/main/frontend/"
    fi
    if [[ ! "$WORKING_DIR" =~ "src/main/frontend" ]]; then
        echo "  • Update pom.xml frontend-maven-plugin configuration"
    fi
fi

echo ""
echo "For detailed status, run:"
echo "  cd WebUI && bash check-migration-status.sh"
echo ""
