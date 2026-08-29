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

package com.percussion.rest.locales;

import java.net.URI;
import java.util.List;

/**
 * Admin catalog for the singleton auto-translation set (CD-18).
 *
 * <p>Persisted via {@code IPSContentDesignWs.loadTranslationSettings} / {@code
 * saveTranslationSettings} (held design lock released on save). Empty list clears the set.
 */
public interface IAutoTranslationsAdaptor {

  /** Load current auto-translation rows. Admin only. */
  List<AutoTranslationRow> getAutoTranslations(URI baseUri);

  /**
   * Replace the auto-translation set. Admin only. Empty list deletes all rows.
   *
   * @return the persisted set (reload after save)
   */
  List<AutoTranslationRow> saveAutoTranslations(URI baseUri, List<AutoTranslationRow> rows);
}
