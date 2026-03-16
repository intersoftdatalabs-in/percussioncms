#!/usr/bin/env bash
# api-client.sh — Percussion CMS REST API client helper.
#
# This script is meant to be sourced, not executed directly. It provides
# shell functions for authenticating with and querying the Percussion CMS
# REST API.
#
# Usage:
#   source .github/skills/percussioncms-dev/scripts/api-client.sh
#
#   # Login (stores session cookie):
#   perc_login "Admin" "mypassword"
#
#   # Make API calls:
#   perc_api GET "/folders/by-path/MySite"
#   perc_api GET "/pages/by-path/MySite/index"
#   perc_api GET "/assets/by-path/Assets/uploads"
#   perc_api DELETE "/assets/by-path/Assets/uploads/old-file.jpg"
#   perc_api PUT "/folders/by-path/MySite/NewFolder" '{"name":"NewFolder"}'
#
#   # Convenience functions:
#   perc_list_sites
#   perc_list_folders "MySite"
#   perc_list_folders "MySite" "FolderA/FolderB"
#   perc_list_assets
#   perc_list_assets "uploads"
#   perc_list_pages "MySite"
#   perc_list_pages "MySite" "FolderA"
#
# Environment:
#   API_BASE       REST API base URL (default: http://localhost:9992/Rhythmyx/rest)
#   CMS_USER       Username for login (default: Admin)
#   CMS_PASSWORD   Password for login (no default — will prompt)
#

API_BASE="${API_BASE:-http://localhost:9992/Rhythmyx/rest}"
COOKIE_JAR="/tmp/perc-cookies.txt"

# ─── Authentication ───────────────────────────────────────────────────────────

# Login to Percussion CMS and store session cookie.
# Usage: perc_login [username] [password]
perc_login() {
  local user="${1:-${CMS_USER:-Admin}}"
  local pass="${2:-${CMS_PASSWORD:-}}"

  if [[ -z "${pass}" ]]; then
    echo -n "Password for ${user}: "
    read -rs pass
    echo ""
  fi

  echo "Logging in as ${user}..."

  # Percussion CMS uses form-based j_security_check or basic auth depending
  # on configuration. Try basic auth first, then fall back to form login.

  # Attempt 1: HTTP Basic Auth — just store the credentials in the cookie jar
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -c "${COOKIE_JAR}" \
    -u "${user}:${pass}" \
    "${API_BASE}/folders/by-path/Assets")

  if [[ "${http_code}" == "200" ]]; then
    echo "Login successful (Basic Auth)."
    # Store credentials for reuse
    export PERC_AUTH_HEADER="-u ${user}:${pass}"
    return 0
  fi

  # Attempt 2: Form-based login (j_security_check)
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -c "${COOKIE_JAR}" \
    -d "j_username=${user}&j_password=${pass}" \
    "${API_BASE%/rest}/j_security_check")

  if [[ "${http_code}" == "200" || "${http_code}" == "302" || "${http_code}" == "303" ]]; then
    echo "Login successful (Form Auth)."
    export PERC_AUTH_HEADER=""
    return 0
  fi

  echo "ERROR: Login failed (HTTP ${http_code}). Check username/password." >&2
  return 1
}

# ─── Core API Call ────────────────────────────────────────────────────────────

# Make a REST API call.
# Usage: perc_api METHOD PATH [BODY]
#   METHOD  HTTP method (GET, PUT, POST, DELETE)
#   PATH    API path relative to API_BASE (e.g., /folders/by-path/MySite)
#   BODY    Optional JSON body for PUT/POST
perc_api() {
  local method="${1:?Usage: perc_api METHOD PATH [BODY]}"
  local path="${2:?Usage: perc_api METHOD PATH [BODY]}"
  local body="${3:-}"

  # Normalize path: ensure it starts with /
  [[ "${path}" == /* ]] || path="/${path}"

  local curl_args=(
    -s
    -X "${method}"
    -b "${COOKIE_JAR}"
    -c "${COOKIE_JAR}"
    -H "Content-Type: application/json"
    -H "Accept: application/json"
  )

  # Add auth header if stored from basic auth login
  if [[ -n "${PERC_AUTH_HEADER:-}" ]]; then
    # shellcheck disable=SC2206
    curl_args+=(${PERC_AUTH_HEADER})
  fi

  # Add body if provided
  if [[ -n "${body}" ]]; then
    curl_args+=(-d "${body}")
  fi

  curl "${curl_args[@]}" "${API_BASE}${path}"
}

# Pretty-print a perc_api response
perc_api_pretty() {
  perc_api "$@" | python3 -m json.tool 2>/dev/null || perc_api "$@"
}

# ─── Convenience Functions ────────────────────────────────────────────────────

# List all sites.
# Usage: perc_list_sites
perc_list_sites() {
  echo "Listing sites..."
  perc_api_pretty GET "/folders/by-path/Sites"
}

# List folders under a site or path.
# Usage: perc_list_folders SITE_NAME [PATH]
perc_list_folders() {
  local site="${1:?Usage: perc_list_folders SITE_NAME [PATH]}"
  local path="${2:-}"

  if [[ -n "${path}" ]]; then
    echo "Listing folders in ${site}/${path}..."
    perc_api_pretty GET "/folders/by-path/${site}/${path}"
  else
    echo "Listing folders in ${site}..."
    perc_api_pretty GET "/folders/by-path/${site}"
  fi
}

# List assets in the asset library.
# Usage: perc_list_assets [SUBFOLDER_PATH]
perc_list_assets() {
  local path="${1:-}"

  if [[ -n "${path}" ]]; then
    echo "Listing assets in Assets/${path}..."
    perc_api_pretty GET "/folders/by-path/Assets/${path}"
  else
    echo "Listing asset library root..."
    perc_api_pretty GET "/folders/by-path/Assets"
  fi
}

# List pages in a site or folder.
# Usage: perc_list_pages SITE_NAME [PATH]
perc_list_pages() {
  local site="${1:?Usage: perc_list_pages SITE_NAME [PATH]}"
  local path="${2:-}"

  # Pages are listed as part of the folder response
  if [[ -n "${path}" ]]; then
    echo "Listing pages in ${site}/${path}..."
    perc_api_pretty GET "/folders/by-path/${site}/${path}"
  else
    echo "Listing pages in ${site}..."
    perc_api_pretty GET "/folders/by-path/${site}"
  fi
  echo ""
  echo "TIP: Pages are in the 'pages' array of the response."
  echo "     Follow page 'href' links to get full page details."
}

# ─── Self-test ────────────────────────────────────────────────────────────────

# Quick connectivity check.
# Usage: perc_check
perc_check() {
  echo "Checking CMS connectivity at ${API_BASE}..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -b "${COOKIE_JAR}" \
    ${PERC_AUTH_HEADER:-} \
    "${API_BASE}/folders/by-path/Assets")

  if [[ "${http_code}" == "200" ]]; then
    echo "CMS is reachable and authenticated."
    return 0
  elif [[ "${http_code}" == "401" || "${http_code}" == "403" ]]; then
    echo "CMS is reachable but authentication failed. Run perc_login first."
    return 1
  else
    echo "CMS returned HTTP ${http_code}. Is it running?"
    return 1
  fi
}

echo "Percussion CMS API client loaded."
echo "  API_BASE:   ${API_BASE}"
echo "  Cookie Jar: ${COOKIE_JAR}"
echo ""
echo "Functions available:"
echo "  perc_login [user] [pass]        — Authenticate"
echo "  perc_api METHOD PATH [BODY]     — Raw API call"
echo "  perc_api_pretty METHOD PATH     — Pretty-printed API call"
echo "  perc_list_sites                 — List all sites"
echo "  perc_list_folders SITE [PATH]   — List folders"
echo "  perc_list_assets [PATH]         — List assets"
echo "  perc_list_pages SITE [PATH]     — List pages"
echo "  perc_check                      — Connectivity check"
