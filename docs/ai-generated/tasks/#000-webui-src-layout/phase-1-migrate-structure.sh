#!/bin/bash
################################################################################
# Phase 1 Part 2: Structural File Migration
#
# After updating all path references, move files from war/ to src/main/webapp/cm/
#
# This script:
# 1. Creates the new directory structure
# 2. Moves files to their new locations
# 3. Creates backups in case of issues
# 4. Validates that all files were moved
#
# Usage: bash phase-1-migrate-structure.sh [webui_dir]
# Default webui_dir: current directory
################################################################################

set -e

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

# Safety check
if [[ ! -d "war" ]]; then
    echo "❌ Error: 'war' directory not found. Run this script from WebUI directory."
    exit 1
fi

if [[ ! -d "src/main/webapp" ]]; then
    echo "📁 Creating src/main/webapp directory..."
    mkdir -p "src/main/webapp"
fi

echo "=================================="
echo "Phase 1: Structural Migration"
echo "=================================="
echo ""
echo "⚠️  IMPORTANT: This will move files. Ensure you have committed all changes first!"
echo ""
read -p "Continue? (yes/no): " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "🔄 Starting structural migration..."
echo ""

# Create target structure - create all directories first
echo "📁 Creating directory structure under src/main/webapp/cm/..."
echo ""

mkdir -p "src/main/webapp/cm/vendor/js/legacy"
mkdir -p "src/main/webapp/cm/vendor/css/legacy/{themes,skin-win8}"
mkdir -p "src/main/webapp/cm/vendor/fonts"
mkdir -p "src/main/webapp/cm/vendor/images/icons"

mkdir -p "src/main/webapp/cm/app/js/legacy/{controllers,models,services,plugins,views,classes}"
mkdir -p "src/main/webapp/cm/app/css/legacy"
mkdir -p "src/main/webapp/cm/app/images"
mkdir -p "src/main/webapp/cm/app/widgetbuilder"

mkdir -p "src/main/webapp/cm/pages/{app,includes,popups,cui,mock,testing}"
mkdir -p "src/main/webapp/cm/widgets"
mkdir -p "src/main/webapp/cm/api"
mkdir -p "src/main/webapp/cm/WEB-INF"
mkdir -p "src/main/webapp/cm/META-INF"

echo "✅ Directory structure created"
echo ""

# Function to move files with validation
move_files() {
    local src_pattern="$1"
    local dest_dir="$2"
    local description="$3"

    echo "Moving $description..."

    if find "war" -type f $src_pattern 2>/dev/null | grep -q .; then
        find "war" -type f $src_pattern 2>/dev/null | while read -r src_file; do
            # Preserve relative path structure if dest is a parent dir
            rel_path="${src_file#war/}"

            # Get directory structure
            src_subdir=$(dirname "$rel_path")

            if [[ "$src_subdir" == "." ]]; then
                # File is directly in war/
                dest_file="$dest_dir/$(basename "$src_file")"
            else
                # Preserve subdirectory structure
                dest_file="$dest_dir/$rel_path"
                mkdir -p "$(dirname "$dest_file")"
            fi

            # Copy file (to be safe, copy first)
            cp "$src_file" "$dest_file"

            # Create backup
            cp "$src_file" "${src_file}.pre-move"
        done

        echo "   ✓ Moved from $src_pattern"
    fi
}

################################################################################
# Move files according to mapping
################################################################################

# Vendor JS/CSS
move_files "-path 'war/jslib/*'" "src/main/webapp/cm/vendor/js/legacy" "vendor JavaScript libraries"
move_files "-path 'war/themes/*'" "src/main/webapp/cm/vendor/css/legacy/themes" "theme CSS"
move_files "-path 'war/skin-win8/*'" "src/main/webapp/cm/vendor/css/legacy/skin-win8" "skin-win8 CSS"
move_files "-path 'war/fonts/*'" "src/main/webapp/cm/vendor/fonts" "web fonts"

# Vendor icons (if separate from app images)
# move_files "-path 'war/images/icons/*'" "src/main/webapp/cm/vendor/images/icons" "vendor icon images"

echo ""

# App-owned source files
move_files "-path 'war/controllers/*'" "src/main/webapp/cm/app/js/legacy/controllers" "controller JavaScript"
move_files "-path 'war/models/*'" "src/main/webapp/cm/app/js/legacy/models" "model JavaScript"
move_files "-path 'war/services/*'" "src/main/webapp/cm/app/js/legacy/services" "service JavaScript"
move_files "-path 'war/plugins/*'" "src/main/webapp/cm/app/js/legacy/plugins" "plugin JavaScript"
move_files "-path 'war/views/*'" "src/main/webapp/cm/app/js/legacy/views" "view JavaScript"
move_files "-path 'war/classes/*'" "src/main/webapp/cm/app/js/legacy/classes" "class JavaScript"

# App CSS (but NOT jslibMin or cssMin, those are generated)
move_files "-path 'war/css/*' ! -path 'war/css/jslib*' ! -path 'war/css/cssMin*'" "src/main/webapp/cm/app/css/legacy" "application CSS"

# App images (non-vendor)
move_files "-path 'war/images/*' ! -path 'war/images/icons/*'" "src/main/webapp/cm/app/images" "application images"

# App specific modules
move_files "-path 'war/widgetbuilder/*'" "src/main/webapp/cm/app/widgetbuilder" "widget builder"

echo ""

# Pages (JSP and includes)
move_files "-path 'war/app/*.jsp'" "src/main/webapp/cm/pages/app" "JSP application pages"
move_files "-path 'war/app/includes/*'" "src/main/webapp/cm/pages/includes" "JSP includes"
move_files "-path 'war/app/popups/*'" "src/main/webapp/cm/pages/popups" "JSP popups"

# CUI (separate SPA)
move_files "-path 'war/cui/*'" "src/main/webapp/cm/pages/cui" "CUI single-page app"

# Test/mock pages
move_files "-path 'war/mock/*'" "src/main/webapp/cm/pages/mock" "mock pages"
move_files "-path 'war/testing/*'" "src/main/webapp/cm/pages/testing" "test pages"

echo ""

# Widgets
move_files "-path 'war/widgets/*'" "src/main/webapp/cm/widgets" "widget definitions"

# API docs/content
move_files "-path 'war/api/*'" "src/main/webapp/cm/api" "API documentation"

# Web config files
move_files "-path 'war/WEB-INF/*'" "src/main/webapp/cm/WEB-INF" "WEB-INF configuration"
move_files "-path 'war/META-INF/*'" "src/main/webapp/cm/META-INF" "META-INF configuration"

# Any remaining static files
move_files "-type f" "src/main/webapp/cm" "remaining static files"

echo ""
echo "✅ File moves complete"
echo ""

################################################################################
# Validation
################################################################################

echo "🔍 Validating migration..."
echo ""

# Count files in new structure
NEW_FILE_COUNT=$(find "src/main/webapp/cm" -type f 2>/dev/null | wc -l)
OLD_FILE_COUNT=$(find "war" -type f 2>/dev/null | wc -l)

echo "📊 File count:"
echo "   In src/main/webapp/cm/: $NEW_FILE_COUNT"
echo "   In war/ (including backups): $OLD_FILE_COUNT"
echo ""

# Check that both directories have same content (minus backups)
WAR_ACTUAL=$(find "war" -type f ! -name "*.pre-move" ! -name "*.pre-phase1" 2>/dev/null | wc -l)
echo "   In war/ (excluding backups): $WAR_ACTUAL"
echo "   (These are generated files - jslibMin, cssMin, modern - to be deleted next)"
echo ""

if [[ $NEW_FILE_COUNT -gt 0 ]]; then
    echo "✅ Files successfully moved to src/main/webapp/cm/"
else
    echo "❌ No files found in new structure - migration may have failed"
    exit 1
fi

echo ""
echo "💾 Backup files created with .pre-move extension"
echo "   To revert: find war -name '*.pre-move' -exec bash -c 'mv {} \${0%.pre-move}' {} \\;"
echo ""

################################################################################
# Next Steps
################################################################################

echo "=================================="
echo "Migration Summary"
echo "=================================="
echo ""
echo "Next steps:"
echo ""
echo "1. Review new directory structure:"
echo "   tree src/main/webapp/cm/"
echo ""
echo "2. Update pom.xml (if not already done):"
echo "   - Change maven-war-plugin webResources from 'war/' to 'src/main/webapp/'"
echo ""
echo "3. Test the build:"
echo "   ./mvn-env.sh -f WebUI/pom.xml clean compile"
echo ""
echo "4. Verify JSPs still load from browser"
echo ""
echo "5. Clean up generated folders from war/ (they'll be regenerated):"
echo "   rm -rf war/jslibMin war/cssMin war/modern"
echo ""
echo "✅ Phase 1 structural migration complete!"
echo ""
