#!/bin/bash

# Agent Temp Directory Cleaner Script
# Cleans old agent temp directories and optionally specific ones

REPO_ROOT=$(git rev-parse --show-toplevel)
TMP_BASE="$REPO_ROOT/.tmp"

# Function to clean old directories (older than 24 hours)
clean_old() {
    echo "Cleaning agent temp directories older than 24 hours..."
    find "$TMP_BASE" -name "agent-*" -type d -mtime +0 -exec rm -rf {} + 2>/dev/null || true
    echo "Old temp directories cleaned."
}

# Function to clean a specific session directory
clean_specific() {
    local session_id="$1"
    if [ -z "$session_id" ]; then
        echo "Error: --clean-specific requires a session ID"
        exit 1
    fi
    local tmpdir="$TMP_BASE/agent-$session_id"
    if [ -d "$tmpdir" ]; then
        echo "Cleaning specific temp directory: $tmpdir"
        rm -rf "$tmpdir"
        echo "Specific temp directory cleaned."
    else
        echo "Temp directory $tmpdir does not exist."
    fi
}

# Function to show help
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --clean-old          Clean all agent temp directories older than 24 hours (default)"
    echo "  --clean-specific ID  Clean the specific agent temp directory for session ID"
    echo "  --help               Show this help message"
    echo ""
    echo "If no options are provided, --clean-old is assumed."
}

# Main logic
if [ $# -eq 0 ]; then
    clean_old
else
    case "$1" in
        --clean-old)
            clean_old
            ;;
        --clean-specific)
            clean_specific "$2"
            ;;
        --help|-h)
            show_help
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
fi
