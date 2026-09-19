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

import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";
import { mapIdParam } from "./deepLinkMap";

/**
 * Server-driven item publishing actions menu (issue #4581, slice 6 of #4531).
 *
 * <p>Consumes the existing sitemanage {@code GET …/publish/publishingActions/{id}}
 * endpoint (shipped slices 3–5 use the publish/stage REST; this slice adds no
 * new REST). The server returns one row per workflow action with an
 * {@code enabled} flag; the shell renders those rows and navigates to the
 * already-shipped panels. CMS paths always use {@code /}.</p>
 */

/** Wire row from {@code PSPublishingAction} ({@code name}, {@code enabled}). */
export interface PublishingAction {
  name: string;
  enabled: boolean;
}

/** Panel target on the site workspace for a server action name. */
export type PublishingActionTarget =
  | "publish-now"
  | "schedule"
  | "takedown"
  | "stage"
  | "unstage";

/** Server {@code PSPublishingAction} names (data, rendered as received). */
export const PUBLISHING_ACTION_NAMES = {
  PUBLISH: "Publish",
  SCHEDULE: "Schedule...",
  TAKEDOWN: "Remove from Site",
  STAGE: "Stage",
  REMOVE_FROM_STAGING: "Remove from Staging",
} as const;

/** Panel testid that owns each target (peers under {@code components/}). */
export const PUBLISHING_ACTION_PANEL_TESTID: Record<
  PublishingActionTarget,
  string
> = {
  "publish-now": "item-publish-now",
  schedule: "item-schedule-dates",
  takedown: "item-takedown",
  stage: "item-stage",
  unstage: "item-stage",
};

/**
 * Map a server action name to its site-workspace panel target.
 * Unknown names return {@code null} so callers skip the row.
 */
export function actionTargetForName(
  name: string,
): PublishingActionTarget | null {
  const normalized = (name ?? "").trim().toLowerCase();
  if (normalized === PUBLISHING_ACTION_NAMES.PUBLISH.toLowerCase()) {
    return "publish-now";
  }
  if (
    normalized === PUBLISHING_ACTION_NAMES.SCHEDULE.toLowerCase() ||
    normalized === "schedule"
  ) {
    return "schedule";
  }
  if (normalized === PUBLISHING_ACTION_NAMES.TAKEDOWN.toLowerCase()) {
    return "takedown";
  }
  if (normalized === PUBLISHING_ACTION_NAMES.STAGE.toLowerCase()) {
    return "stage";
  }
  if (
    normalized === PUBLISHING_ACTION_NAMES.REMOVE_FROM_STAGING.toLowerCase()
  ) {
    return "unstage";
  }
  return null;
}

function toPublishingAction(row: unknown): PublishingAction | null {
  if (row == null || typeof row !== "object") {
    return null;
  }
  const obj = row as Record<string, unknown>;
  const name = obj.name;
  if (typeof name !== "string" || !name.trim()) {
    return null;
  }
  return { name: name.trim(), enabled: obj.enabled === true };
}

function asArray(value: unknown): unknown[] | null {
  if (Array.isArray(value)) {
    return value;
  }
  return null;
}

/**
 * Parse the {@code publishingActions} response body. The endpoint serializes a
 * {@code PSPublishingActionList} (a bare JSON array); accept common envelope
 * keys defensively. Rows with unknown names are kept — the menu skips them at
 * render via {@link actionTargetForName}.
 */
export function parsePublishingActions(body: unknown): PublishingAction[] {
  if (body == null) {
    return [];
  }
  const direct = asArray(body);
  if (direct) {
    return direct
      .map(toPublishingAction)
      .filter((r): r is PublishingAction => r !== null);
  }
  if (typeof body === "object") {
    const obj = body as Record<string, unknown>;
    for (const key of [
      "PSPublishingActionList",
      "PublishingActionList",
      "ArrayList",
      "actions",
    ]) {
      const nested = asArray(obj[key]);
      if (nested) {
        return nested
          .map(toPublishingAction)
          .filter((r): r is PublishingAction => r !== null);
      }
    }
    const single = toPublishingAction(body);
    if (single) {
      return [single];
    }
  }
  return [];
}

/** Normalize the menu item id the same way peer panels do. */
export function safeActionsItemId(
  raw: string | null | undefined,
): string {
  return mapIdParam(raw);
}

/**
 * Map fetch failures: HTTP 403 → forbidden, 404 → not found, else generic.
 * Mirrors the peer {@code *ErrorMessage} helpers (e.g. stage).
 */
export function publishingActionsErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 404) {
      return formatApiError(err, message(MSG.PUBLISH_NOW_NOT_FOUND));
    }
  }
  const text = formatApiError(err, message(MSG.PUBLISH_ERROR));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (/\b404\b|\bNOT FOUND\b/i.test(text)) {
    return message(MSG.PUBLISH_NOW_NOT_FOUND);
  }
  return text;
}
