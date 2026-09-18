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

import type { Plugin } from "@opencode-ai/plugin"
import { homedir } from "node:os"
import { join, posix } from "node:path"

const REPO_SLUG = "intersoft-workspace-percussioncms"
const COMPONENT = "night-issue-prs"

function defaultWorktreePath(): string {
  return join(homedir(), ".opencode", "worktrees", REPO_SLUG, COMPONENT)
}

function defaultReportPath(): string {
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
  if (!("NIGHT_CODING_TOOL_VERSION" in env))
    env.NIGHT_CODING_TOOL_VERSION = ""
}

function buildNightBashPolicy(): Record<string, "allow" | "ask" | "deny"> {
  const bash: Record<string, "allow" | "ask" | "deny"> = {}

  bash["*"] = "ask"

  bash["gh *"] = "allow"
  bash["git status *"] = "allow"
  bash["git diff *"] = "allow"
  bash["git log *"] = "allow"
  bash["git fetch *"] = "allow"
  bash["git branch *"] = "allow"
  bash["git checkout *"] = "allow"
  bash["git worktree *"] = "allow"
  bash["git add *"] = "allow"
  bash["git commit *"] = "allow"
  bash["git rebase *"] = "allow"
  bash["git push *"] = "allow"
  bash["git remote *"] = "allow"
  bash["git rev-parse *"] = "allow"
  bash["git show *"] = "allow"
  bash["git stash *"] = "allow"

  bash["./mvnw *"] = "allow"
  bash["../mvnw *"] = "allow"
  bash["../../mvnw *"] = "allow"
  bash["../../../mvnw *"] = "allow"
  bash["mvnw *"] = "allow"
  bash["mvnw.cmd *"] = "allow"

  bash["docker *"] = "allow"
  bash["python3 scripts/*"] = "allow"
  bash["python3 ../scripts/*"] = "allow"
  bash["python3 ../../scripts/*"] = "allow"
  bash["node *"] = "allow"
  bash["npm run *"] = "allow"
  bash["npm install *"] = "allow"
  bash["npx *"] = "allow"

  bash["mkdir -p *"] = "allow"
  bash["mkdir *"] = "allow"
  bash["ls *"] = "allow"
  bash["cat *"] = "allow"
  bash["rg *"] = "allow"
  bash["test -d *"] = "allow"
  bash["test -f *"] = "allow"
  bash["test -e *"] = "allow"
  bash["echo *"] = "allow"
  bash["pwd"] = "allow"
  bash["cd *"] = "allow"
  bash["cp *"] = "allow"
  bash["mv *"] = "allow"

  bash["*--skipTests*"] = "deny"
  bash["*-Dmaven.test.skip*"] = "deny"
  bash["*-Dmaven.test.skip.exec*"] = "deny"
  bash["*push*--force*main*"] = "deny"
  bash["*push*--force*night-issue-prs-main*"] = "deny"

  return bash
}

function mergeBashPolicy(
  existing: unknown,
  night: Record<string, "allow" | "ask" | "deny">,
): Record<string, "allow" | "ask" | "deny"> {
  if (typeof existing === "object" && existing !== null && !Array.isArray(existing)) {
    const merged: Record<string, "allow" | "ask" | "deny"> = { ...night }
    for (const [k, v] of Object.entries(existing)) {
      if (v === "allow" || v === "ask" || v === "deny") {
        merged[k] = v
      }
    }
    return merged
  }
  if (existing === "allow" || existing === "ask" || existing === "deny") {
    const merged = { ...night }
    merged["*"] = existing
    return merged
  }
  return night
}

export default (async () => {
  const isNight = detectNightSession()

  return {
    config: async (cfg) => {
      if (!isNight) return

      cfg.permission = cfg.permission ?? {}
      cfg.permission.bash = mergeBashPolicy(
        (cfg.permission as { bash?: unknown }).bash,
        buildNightBashPolicy(),
      )
    },

    "shell.env": async (env) => {
      if (!isNight) return
      applyNightDefaults(env as Record<string, string | undefined>)
    },
  }
}) satisfies Plugin
