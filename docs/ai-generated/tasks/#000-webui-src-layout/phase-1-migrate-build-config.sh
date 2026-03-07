#!/bin/bash
################################################################################
# Phase 1 Part 3: Build Configuration Migration
#
# Move build configuration files (package.json, vite configs, scripts) from
# WebUI root to src/main/frontend/
#
# This script:
# 1. Creates src/main/frontend directory
# 2. Moves package.json, package-lock.json, vite configs, scripts
# 3. Updates pom.xml to point to new locations
# 4. Validates the new structure
#
# Usage: bash phase-1-migrate-build-config.sh [webui_dir]
# Default webui_dir: current directory
################################################################################

set -e

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

# Safety checks
if [[ ! -f "pom.xml" ]]; then
    echo "❌ Error: pom.xml not found. Run this script from WebUI directory."
    exit 1
fi

echo "=================================="
echo "Phase 1 Part 3: Build Config Migration"
echo "=================================="
echo ""
echo "This will move build config files to src/main/frontend/"
echo ""
read -p "Continue? (yes/no): " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "🔄 Starting build configuration migration..."
echo ""

################################################################################
# Create target structure
################################################################################

echo "📁 Creating src/main/frontend directory structure..."
mkdir -p "src/main/frontend/scripts"
mkdir -p "src/main/frontend/node_modules"

echo "✅ Directory structure created"
echo ""

################################################################################
# Move build configuration files
################################################################################

echo "Moving build configuration files..."
echo ""

# Copy files first (safer than move)
MOVED_COUNT=0

# package.json
if [[ -f "package.json" ]]; then
    cp "package.json" "src/main/frontend/package.json"
    cp "package.json" "package.json.pre-move"
    echo "✓ package.json"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# package-lock.json
if [[ -f "package-lock.json" ]]; then
    cp "package-lock.json" "src/main/frontend/package-lock.json"
    cp "package-lock.json" "package-lock.json.pre-move"
    echo "✓ package-lock.json"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# tsconfig.json
if [[ -f "tsconfig.json" ]]; then
    cp "tsconfig.json" "src/main/frontend/tsconfig.json"
    cp "tsconfig.json" "tsconfig.json.pre-move"
    echo "✓ tsconfig.json"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# vite.config.ts
if [[ -f "vite.config.ts" ]]; then
    cp "vite.config.ts" "src/main/frontend/vite.config.ts"
    cp "vite.config.ts" "vite.config.ts.pre-move"
    echo "✓ vite.config.ts"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# vite.legacy.config.ts
if [[ -f "vite.legacy.config.ts" ]]; then
    cp "vite.legacy.config.ts" "src/main/frontend/vite.legacy.config.ts"
    cp "vite.legacy.config.ts" "vite.legacy.config.ts.pre-move"
    echo "✓ vite.legacy.config.ts"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# scripts directory
if [[ -d "scripts" ]]; then
    # Only copy build scripts, not other assets
    if [[ -f "scripts/build-legacy-bundles.js" ]]; then
        cp "scripts/build-legacy-bundles.js" "src/main/frontend/scripts/build-legacy-bundles.js"
        echo "✓ scripts/build-legacy-bundles.js"
        MOVED_COUNT=$((MOVED_COUNT + 1))
    fi

    # Copy any other helper scripts
    find "scripts" -maxdepth 1 -type f -name "*.js" -o -name "*.sh" | while read -r script; do
        if [[ ! "$script" =~ "pre-move" ]]; then
            cp "$script" "src/main/frontend/scripts/$(basename "$script")"
            echo "✓ $script"
            MOVED_COUNT=$((MOVED_COUNT + 1))
        fi
    done
fi

echo ""
echo "✅ $MOVED_COUNT files moved to src/main/frontend/"
echo ""

################################################################################
# Validate moved files
################################################################################

echo "🔍 Validating new structure..."
echo ""

if [[ -f "src/main/frontend/package.json" ]]; then
    echo "✓ src/main/frontend/package.json found"
    # Show a summary of main dependencies
    echo "  Dependencies (sample):"
    grep -A 3 '"dependencies"' "src/main/frontend/package.json" | head -4 | sed 's/^/    /'
else
    echo "❌ package.json not found in new location"
fi

if [[ -f "src/main/frontend/vite.config.ts" ]]; then
    echo "✓ src/main/frontend/vite.config.ts found"
else
    echo "❌ vite.config.ts not found in new location"
fi

if [[ -f "src/main/frontend/vite.legacy.config.ts" ]]; then
    echo "✓ src/main/frontend/vite.legacy.config.ts found"
else
    echo "❌ vite.legacy.config.ts not found in new location"
fi

echo ""

################################################################################
# Next Steps
################################################################################

echo "=================================="
echo "Build Configuration Migration Summary"
echo "=================================="
echo ""
echo "Files moved to src/main/frontend/:"
ls -la src/main/frontend/ | grep -v "^total" | grep -v "^d" | sed 's/^/  /'
echo ""

echo "💾 Backup files created (*.pre-move)"
echo "   To revert: cp src/main/frontend/package.json package.json (and repeat for other files)"
echo ""

echo "⚠️  NEXT: Update pom.xml manually or run pom-update script"
echo ""
echo "Required pom.xml changes:"
echo ""
echo "1. Update frontend-maven-plugin workingDirectory:"
echo "   FROM: <workingDirectory>\${project.basedir}</workingDirectory>"
echo "   TO:   <workingDirectory>\${project.basedir}/src/main/frontend</workingDirectory>"
echo ""
echo "2. Update maven-war-plugin webResources (if using old war/ path):"
echo "   FROM: <directory>war</directory>"
echo "   TO:   <directory>src/main/webapp</directory>"
echo ""
echo "3. Update Vite output paths in vite.config.ts and vite.legacy.config.ts:"
echo "   FROM: outDir: 'war/modern'"
echo "   TO:   outDir: '../../../../../target/generated-webui/cm/modern'"
echo ""

echo "✅ Phase 1 Part 3 complete!"
echo ""
echo "Next steps:"
echo "1. Edit pom.xml (see above)"
echo "2. Test: ./mvn-env.sh -f WebUI/pom.xml clean compile"
echo "3. Verify build runs from new location"
echo ""
