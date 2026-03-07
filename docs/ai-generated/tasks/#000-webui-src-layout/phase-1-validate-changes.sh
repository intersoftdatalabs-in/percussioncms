#!/bin/bash
################################################################################
# Phase 1 Validation: Preview all changes that will be made
#
# This script shows what files will be affected by Phase 1 reference updates
# WITHOUT making any changes. Use this to review scope before committing.
#
# Usage: bash phase-1-validate-changes.sh
################################################################################

set -e

WEBUI_DIR="${1:-.}"
cd "$WEBUI_DIR" || exit 1

echo "=================================="
echo "Phase 1 Validation: Preview Changes"
echo "=================================="
echo ""

# Count and list files that match patterns
echo "📋 Files affected by Phase 1 reference migration:"
echo ""

echo "1. JSP Files:"
find war -name "*.jsp" 2>/dev/null | wc -l | xargs echo "   Total:"
echo "   Sample files:"
find war -name "*.jsp" 2>/dev/null | head -5 | sed 's/^/   - /'
echo ""

echo "2. HTML Files (widgets, includes):"
find war -name "*.html" 2>/dev/null | wc -l | xargs echo "   Total:"
echo "   Sample files:"
find war -name "*.html" 2>/dev/null | head -5 | sed 's/^/   - /'
echo ""

echo "3. CSS Files:"
find war -name "*.css" 2>/dev/null | wc -l | xargs echo "   Total:"
echo "   Sample files:"
find war -name "*.css" 2>/dev/null | head -5 | sed 's/^/   - /'
echo ""

echo "4. JavaScript Files (may contain hardcoded paths):"
find war -name "*.js" 2>/dev/null | wc -l | xargs echo "   Total:"
echo ""

# Find files with old path patterns
echo "📍 Files containing OLD paths (will be updated):"
echo ""

echo "Files with '/cm/jslib/' pattern:"
grep -r "/cm/jslib/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | wc -l | xargs echo "   Count:"
grep -r "/cm/jslib/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | head -3 | sed 's/^/   - /'
echo ""

echo "Files with '/cm/css/' pattern:"
grep -r "/cm/css/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | wc -l | xargs echo "   Count:"
grep -r "/cm/css/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | head -3 | sed 's/^/   - /'
echo ""

echo "Files with '/cm/themes/' pattern:"
grep -r "/cm/themes/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | wc -l | xargs echo "   Count:"
echo ""

echo "Files with '/cm/controllers/' pattern:"
grep -r "/cm/controllers/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | wc -l | xargs echo "   Count:"
echo ""

echo "Files with '/cm/jslibMin/' pattern:"
grep -r "/cm/jslibMin/" war --include="*.jsp" --include="*.html" --include="*.css" --include="*.js" --include="*.ts" 2>/dev/null | cut -d: -f1 | sort -u | wc -l | xargs echo "   Count:"
echo ""

echo "✅ Validation complete. Review the counts above."
echo ""
echo "Next step: Run phase-1-test-single.sh to test on one JSP file"
