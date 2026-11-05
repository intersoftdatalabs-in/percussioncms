/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Plugin } from "@opencode/plugin"
import { homedir } from "node:os"
import { join, posix } from "node:path"

const REPO_SLUG = "intersoft-workspace-percussioncms"
const COMPONENT = "night-issue-prs"

/**
 * V2 plugin id. Stable across reloads; plugin storage is scoped by this value.
 */
const PLUGIN_ID = "intersoft.percussioncms.night"

function defaultWorktreePath(): string {
  return join(homedir(), ".opencode", "worktrees", REPO_SLUG, COMPONENT)
}

function defaultReportPath(): string {
  // Relative to the night worktree. posix.join keeps the report path stable
  // in prompts and reports regardless of the host OS.
  return posix.join("scratch", "night-report.md")
}

function detectNightSession(): boolean {
  return Boolean(
    process.env.NIGHT_WORKTREE_PATH ||
      process.env.NIGHT_BASE_BRANCH ||
      process.env.NIGHT_REPORT_PATH,
  )
}

function applyNightDefaults(env: Record<string, string | undefined>): void {
  if (!env.NIGHT_WORKTREE_PATH) env.NIGHT_WORKTREE_PATH = defaultWorktreePath()
  if (!env.NIGHT_BASE_BRANCH) env.NIGHT_BASE_BRANCH = "main"
  if (!env.NIGHT_REPORT_PATH) env.NIGHT_REPORT_PATH = defaultReportPath()
  if (!env.NIGHT_OPERATOR) env.NIGHT_OPERATOR = "opencode"
  if (!env.NIGHT_CODING_TOOL) env.NIGHT_CODING_TOOL = "OpenCode"
  if (!("NIGHT_MODEL_ID" in env)) env.NIGHT_MODEL_ID = ""
  if (!("NIGHT_CODING_TOOL_VERSION" in env)) env.NIGHT_CODING_TOOL_VERSION = ""
}

/**
 * Hard-banned shell commands for the unattended workflow.
 *
 * Keep this list aligned with `.opencode/agents/night-worker.md` → **Hard
 * bans** and `.grok/workflows/README.md`. `--auto` approves an action when no
 * rule denies it, so the permission hook only needs the deny half of the V1
 * policy; the launcher always runs with `--auto`.
 *
 * Shell resources are scanner-produced command strings (compound commands can
 * produce several), so ref checks are token-based rather than substring
 * matches: `--force-with-lease` on a feature branch such as `fix/main-menu`
 * must stay allowed, while `main` / `night-issue-prs-main` stay protected.
 */
const PROTECTED_REFS = new Set(["main", "night-issue-prs-main"])
const PROTECTED_REF_PATTERN = /^(?:refs\/heads\/)?(main|night-issue-prs-main)$/
const FORCE_FLAG_PATTERN = /^(?:--force(?:-with-lease)?|-f)$/
const SKIP_TESTS_PATTERN = /^(?:-DskipTests|-Dmaven\.test\.skip(?:\.exec)?|--skipTests)(?:=|$)/

function isProtectedRef(token: string): boolean {
  const ref = token.includes(":") ? token.slice(token.lastIndexOf(":") + 1) : token
  return PROTECTED_REFS.has(ref) || PROTECTED_REF_PATTERN.test(ref)
}

function isGitPush(tokens: readonly string[]): boolean {
  if (tokens.length === 0) return false
  const executable = tokens[0]
  if (executable !== "git" && !executable.endsWith("/git")) return false
  return tokens.includes("push")
}

function isForcePush(tokens: readonly string[]): boolean {
  return tokens.some(
    (token) => FORCE_FLAG_PATTERN.test(token) || token.startsWith("+"),
  )
}

export function deniedShellReason(resources: readonly string[]): string | undefined {
  for (const resource of resources) {
    const tokens = resource.trim().split(/\s+/)

    if (tokens.some((token) => SKIP_TESTS_PATTERN.test(token))) {
      return "night-issue-prs hard ban: Maven build gates must not skip tests (--skipTests / -DskipTests / -Dmaven.test.skip)."
    }
    if (isGitPush(tokens) && tokens.some(isProtectedRef)) {
      return isForcePush(tokens)
        ? "night-issue-prs hard ban: never force-push main or night-issue-prs-main (use --force-with-lease on the feature branch)."
        : "night-issue-prs hard ban: never push to main or night-issue-prs-main (open a PR instead)."
    }
  }
  return undefined
}

export default Plugin.define({
  id: PLUGIN_ID,
  async setup(ctx) {
    if (!detectNightSession()) return

    await ctx.shell.hook("create.before", (event) => {
      applyNightDefaults(event.env)
    })

    await ctx.permission.hook("evaluate", (event) => {
      if (event.action !== "shell") return
      // An explicit configured deny is already final; nothing to escalate.
      if (event.effect === "deny") return

      const reason = deniedShellReason(event.resources)
      if (reason) {
        event.effect = "deny"
        event.message = reason
      }
    })
  },
})
