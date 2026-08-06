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

import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Facade for resolving locale formats for the UI. Prefers DB rows when the CMS object manager is
 * available; always falls back to {@link PSLocaleFormatDefaults} for missing rows/fields.
 */
public final class PSLocaleFormatCatalog {

  private PSLocaleFormatCatalog() {}

  /**
   * Resolve the effective format for a language tag (never null). Safe to call from JSP without a
   * live Hibernate session — uses product defaults only if the object manager is unavailable.
   */
  public static PSLocaleFormat resolve(String language) {
    return PSLocaleFormatResolver.resolve(language, loadCatalogMap());
  }

  /**
   * Load stored format rows: DB when available, else product defaults map.
   *
   * @return mutable copy keyed by normalized language string
   */
  public static Map<String, PSLocaleFormat> loadCatalogMap() {
    Map<String, PSLocaleFormat> map = new LinkedHashMap<>(PSLocaleFormatDefaults.shipped());
    try {
      IPSCmsObjectMgr mgr = PSCmsObjectMgrLocator.getObjectManager();
      if (mgr != null) {
        List<PSLocaleFormat> rows = mgr.findAllLocaleFormats().collect(Collectors.toList());
        for (PSLocaleFormat row : rows) {
          if (row != null && row.getLanguageString() != null) {
            map.put(PSLocaleFormatResolver.normalize(row.getLanguageString()), row);
          }
        }
      }
    } catch (RuntimeException e) {
      // Unit tests / early boot / missing Spring beans: product defaults only.
    }
    return map;
  }
}
