#!/bin/bash
################################################################################
# Phase 1 Migration: Update ALL reference paths in JSP, HTML, CSS, JS files
#
# This script applies the path reference updates to all relevant files.
# RUN ONLY AFTER testing with phase-1-test-single.sh
#
# Usage: bash phase-1-migrate-all-files.sh [webui_dir]
# Default webui_dir: current directory
################################################################################

set -e

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

# Safety check - ensure we're in the right directory
if [[ ! -d "war" ]]; then
    echo "❌ Error: 'war' directory not found. Run this script from WebUI directory."
    exit 1
fi

echo "=================================="
echo "Phase 1 Migration: Update All Files"
echo "=================================="
echo ""

# Counters
TOTAL_FILES=0
TOTAL_CHANGES=0

################################################################################
# Helper function to apply sed replacements to a file type
################################################################################
apply_replacements() {
    local pattern="$1"
    local description="$2"

    echo "🔄 Processing $description..."

    # Use find with -exec to apply sed to each matching file
    while IFS= read -r file; do
        # Check if file contains any /cm/ paths
        if grep -q "/cm/" "$file" 2>/dev/null; then
            # Create backup first
            cp "$file" "${file}.pre-phase1"

            # Apply all replacements to this file
            sed -i 's|src="/cm/jslib/|src="/cm/app/js/legacy/|g' "$file"
            sed -i 's|href="/cm/css/|href="/cm/app/css/legacy/|g' "$file"
            sed -i 's|href="/cm/themes/|href="/cm/vendor/css/legacy/themes/|g' "$file"
            sed -i 's|/cm/controllers/|/cm/app/js/legacy/controllers/|g' "$file"
            sed -i 's|/cm/models/|/cm/app/js/legacy/models/|g' "$file"
            sed -i 's|/cm/services/|/cm/app/js/legacy/services/|g' "$file"
            sed -i 's|/cm/views/|/cm/app/js/legacy/views/|g' "$file"
            sed -i 's|/cm/plugins/|/cm/app/js/legacy/plugins/|g' "$file"
            sed -i 's|/cm/classes/|/cm/app/js/legacy/classes/|g' "$file"
            sed -i "s|url(/cm/|url(/cm/app/|g" "$file"
            sed -i "s|@import \"/cm/|@import \"/cm/app/|g" "$file"

            # Count changes
            CHANGES=$(diff -u "${file}.pre-phase1" "$file" 2>/dev/null | grep -c "^+" || true)
            if [[ $CHANGES -gt 0 ]]; then
                echo "   ✓ $file ($CHANGES changes)"
                TOTAL_FILES=$((TOTAL_FILES + 1))
                TOTAL_CHANGES=$((TOTAL_CHANGES + CHANGES))
            fi
        fi
    done < <(find war -type f \( $pattern \) 2>/dev/null)
}

################################################################################
# Main migration
################################################################################

echo "Starting path reference updates..."
echo ""

# JSP files
apply_replacements "-name '*.jsp'" "JSP files"
echo ""

# HTML files (widget includes, etc.)
apply_replacements "-name '*.html'" "HTML files"
echo ""

# CSS files
apply_replacements "-name '*.css'" "CSS files"
echo ""

# JavaScript files (may contain hardcoded path references)
apply_replacements "-name '*.js'" "JavaScript files"
echo ""

# TypeScript files (if any have path references)
apply_replacements "-name '*.ts'" "TypeScript files"
echo ""

# Summary
echo "=================================="
echo "Migration Complete"
echo "=================================="
echo ""
echo "📊 Statistics:"
echo "   Total files modified: $TOTAL_FILES"
echo "   Total line changes: $TOTAL_CHANGES"
echo ""

# Verification step
echo "🔍 Verifying replacements..."
echo ""

REMAINING=$(grep -r "/cm/jslib/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | wc -l || echo "0")
if [[ "$REMAINING" -eq 0 ]]; then
    echo "   ✅ No old /cm/jslib/ paths remaining"
else
    echo "   ⚠️  Found $REMAINING instances of old /cm/jslib/ paths:"
    grep -r "/cm/jslib/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | sed 's/^/      - /'
fi
echo ""

REMAINING=$(grep -r "/cm/css/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | wc -l || echo "0")
if [[ "$REMAINING" -eq 0 ]]; then
    echo "   ✅ No old /cm/css/ paths remaining"
else
    echo "   ⚠️  Found $REMAINING instances of old /cm/css/ paths (check if they're in comments or strings)"
fi
echo ""

# Backup information
BACKUP_COUNT=$(find war -name "*.pre-phase1" 2>/dev/null | wc -l)
echo "💾 Backup files created: $BACKUP_COUNT (*.pre-phase1 extension)"
echo "   To revert a file: cp file.ext.pre-phase1 file.ext"
echo "   To clean up: find war -name '*.pre-phase1' -delete"
echo ""

echo "✅ Phase 1 reference migration complete!"
echo ""
echo "Next steps:"
echo "1. Run: ./mvn-env.sh -f WebUI/pom.xml clean compile"
echo "2. Check that JSPs resolve without errors"
echo "3. Load one page in browser and verify CSS/JS load"
echo "4. If all looks good, proceed to Phase 2: file structural moves"
echo ""
