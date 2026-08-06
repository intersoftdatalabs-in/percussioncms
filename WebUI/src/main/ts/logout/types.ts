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

/**
 * Server-provided bootstrap for the React logout page (XSS-safe JSON text node).
 */
export interface LogoutBootstrap {
  /**
   * UI locale for TMX + document lang (BCP-47 lowercase hyphen).
   * Prefer the user's pre-logout session locale; system language is last resort.
   */
  locale: string;
  /**
   * Path-absolute or relative href for the "sign in again" control.
   * Prefer the product login front door ({@code login} / {@code /rxlogin.jsp})
   * with {@code j_locale} so login reopens in the same language.
   */
  loginHref: string;
}
