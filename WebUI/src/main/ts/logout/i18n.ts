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

import { message } from "../i18n/message";

/** Logout-screen TMX message wrapper (same contract as login {@code t()}). */
export function t(key: string, args?: unknown[]): string {
  return message(key, args);
}

export const LOGOUT_KEYS = {
  TITLE: "perc.ui.logout.modern@Signed out",
  MESSAGE: "perc.ui.logout.modern@You have been logged out.",
  SIGN_IN: "perc.ui.logout.modern@Sign in again",
} as const;

export type LogoutKey = (typeof LOGOUT_KEYS)[keyof typeof LOGOUT_KEYS];
