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
package com.percussion.i18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side helpers for content-usable locale selection.
 *
 * <p>Translation target lists show only active locales flagged {@link
 * PSLocale#isContentUsable()}; system-only locales remain selectable elsewhere
 * where a non-content locale makes sense. Absent flag state (pre-migration
 * rows) defaults to usable so existing installs preserve current behavior.
 */
public final class PSLocaleContentUsability {

  private PSLocaleContentUsability() {}

  /**
   * Filter locales to translation targets.
   *
   * @param all all locales from the repository; may be {@code null}
   * @return active, content-usable locales, never {@code null}; order preserved
   *     from the input iteration
   */
  public static List<PSLocale> forTranslationTargets(Iterable<PSLocale> all) {
    List<PSLocale> result = new ArrayList<>();
    if (all == null) {
      return result;
    }
    for (PSLocale loc : all) {
      if (loc != null
          && loc.getStatus() == PSLocale.STATUS_ACTIVE
          && loc.isContentUsable()) {
        result.add(loc);
      }
    }
    return result;
  }
}
