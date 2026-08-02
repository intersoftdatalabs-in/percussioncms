/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { fallbackLabelFromKey, message } from "../i18n/message";

/**
 * Login-screen TMX message wrapper.
 *
 * <p>Reads {@code window.I18N.message} fresh on every call so that a freshly
 * loaded TMX bundle (from {@link ensureTmxLoaded}) is picked up on the next
 * render without stale closures. Falls back to the English text after
 * {@code @} via {@link fallbackLabelFromKey} when the bundle is missing or
 * echoes the catalog key.</p>
 */
export function t(key: string, args?: unknown[]): string {
  return message(key, args);
}

export const LOGIN_KEYS = {
  TITLE: "perc.ui.login.modern@Sign in",
  USERNAME: "perc.ui.login.modern@User name",
  PASSWORD: "perc.ui.login.modern@Password",
  LOCALE: "perc.ui.login.modern@Locale",
  SUBMIT: "perc.ui.login.modern@Login",
} as const;

export type LoginKey = (typeof LOGIN_KEYS)[keyof typeof LOGIN_KEYS];

export { fallbackLabelFromKey };