#!/bin/bash
################################################################################
# Phase 1 Test: Apply reference updates to ONE JSP file
#
# This script tests the sed replacements on a single JSP file (common_js.jsp)
# to verify the patterns work before applying to all files.
#
# Usage: bash phase-1-test-single.sh [webui_dir]
# Default webui_dir: current directory
################################################################################

set -e

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

# Select test file - common_js.jsp has the most references
TEST_FILE="war/app/includes/common_js.jsp"

if [[ ! -f "$TEST_FILE" ]]; then
    echo "❌ Test file not found: $TEST_FILE"
    exit 1
fi

echo "=================================="
echo "Phase 1 Test: Single JSP File"
echo "=================================="
echo ""
echo "Test file: $TEST_FILE"
echo ""

# Create backup
BACKUP_FILE="${TEST_FILE}.backup"
cp "$TEST_FILE" "$BACKUP_FILE"
echo "✅ Backup created: $BACKUP_FILE"
echo ""

# Show original content
echo "📋 Original content (first 20 lines with /cm/ paths):"
echo "---"
grep -n "/cm/" "$TEST_FILE" | head -20
echo "---"
echo ""

# Create temp file with all replacements
TEMP_FILE="${TEST_FILE}.tmp"
cp "$TEST_FILE" "$TEMP_FILE"

echo "🔄 Applying replacements to test file..."
echo ""

# JSP script references: /cm/jslib/* -> /cm/app/js/legacy/* or /cm/vendor/js/legacy/*
# This is the main pattern in JSP files
sed -i 's|src="/cm/jslib/|src="/cm/app/js/legacy/|g' "$TEMP_FILE"
echo "✓ JSP src paths: /cm/jslib/ → /cm/app/js/legacy/"

# JSP CSS references: /cm/css/* -> /cm/app/css/legacy/*
sed -i 's|href="/cm/css/|href="/cm/app/css/legacy/|g' "$TEMP_FILE"
echo "✓ CSS href paths: /cm/css/ → /cm/app/css/legacy/"

# CSS theme references: /cm/themes/* -> /cm/vendor/css/legacy/themes/*
sed -i 's|href="/cm/themes/|href="/cm/vendor/css/legacy/themes/|g' "$TEMP_FILE"
echo "✓ Theme paths: /cm/themes/ → /cm/vendor/css/legacy/themes/"

# Controller references: /cm/controllers/* -> /cm/app/js/legacy/controllers/*
sed -i 's|/cm/controllers/|/cm/app/js/legacy/controllers/|g' "$TEMP_FILE"
echo "✓ Controller paths: /cm/controllers/ → /cm/app/js/legacy/controllers/"

# Models: /cm/models/* -> /cm/app/js/legacy/models/*
sed -i 's|/cm/models/|/cm/app/js/legacy/models/|g' "$TEMP_FILE"
echo "✓ Model paths: /cm/models/ → /cm/app/js/legacy/models/"

# Services: /cm/services/* -> /cm/app/js/legacy/services/*
sed -i 's|/cm/services/|/cm/app/js/legacy/services/|g' "$TEMP_FILE"
echo "✓ Service paths: /cm/services/ → /cm/app/js/legacy/services/"

# Views: /cm/views/* -> /cm/app/js/legacy/views/*
sed -i 's|/cm/views/|/cm/app/js/legacy/views/|g' "$TEMP_FILE"
echo "✓ View paths: /cm/views/ → /cm/app/js/legacy/views/"

# Plugins: /cm/plugins/* -> /cm/app/js/legacy/plugins/*
sed -i 's|/cm/plugins/|/cm/app/js/legacy/plugins/|g' "$TEMP_FILE"
echo "✓ Plugin paths: /cm/plugins/ → /cm/app/js/legacy/plugins/"

# Classes: /cm/classes/* -> /cm/app/js/legacy/classes/*
sed -i 's|/cm/classes/|/cm/app/js/legacy/classes/|g' "$TEMP_FILE"
echo "✓ Class paths: /cm/classes/ → /cm/app/js/legacy/classes/"

# jslibMin (generated): keep at /cm/jslibMin/
echo "✓ Generated bundles: /cm/jslibMin/ (unchanged - generated at build time)"

# Relative path patterns in CSS (url() format)
sed -i "s|url(/cm/|url(/cm/app/|g" "$TEMP_FILE"
echo "✓ CSS url() paths: url(/cm/*) → url(/cm/app/*)"

echo ""
echo "✅ Replacements applied to temp file"
echo ""

# Show diff
echo "📊 Changes preview (diff between original and modified):"
echo "---"
diff -u "$TEST_FILE" "$TEMP_FILE" | head -50 || true
echo "---"
echo ""

# Count what changed
TOTAL_CHANGES=$(diff -u "$TEST_FILE" "$TEMP_FILE" 2>/dev/null | grep -c "^+" | xargs)
echo "📈 Total changed lines: $TOTAL_CHANGES"
echo ""

# Ask user to validate
echo "🔍 VALIDATION REQUIRED:"
echo ""
echo "Please inspect the changes above:"
echo ""
echo "Option 1: APPROVE - Accept and apply to test file"
echo "Option 2: REJECT - Restore backup and exit"
echo ""
read -p "Enter 1 (approve) or 2 (reject): " CHOICE

if [[ "$CHOICE" == "1" ]]; then
    # Apply to test file
    mv "$TEMP_FILE" "$TEST_FILE"
    echo ""
    echo "✅ Changes applied! Test file updated: $TEST_FILE"
    echo ""
    echo "Next steps:"
    echo "1. Test in browser: Load the page that uses $TEST_FILE"
    echo "2. Check browser console for any 404 errors"
    echo "3. Verify CSS and JS load correctly"
    echo "4. Once confirmed, run: bash phase-1-migrate-all-files.sh"
    echo ""
    echo "To revert: cp $BACKUP_FILE $TEST_FILE"
else
    # Restore backup
    rm "$TEMP_FILE"
    echo ""
    echo "❌ Changes rejected. Backup still available: $BACKUP_FILE"
    exit 1
fi
