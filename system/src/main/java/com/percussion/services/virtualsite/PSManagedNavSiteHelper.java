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
package com.percussion.services.virtualsite;

import com.percussion.services.sitemgr.IPSSite;
import org.apache.commons.lang3.StringUtils;

/**
 * Traditional-site flag for CMS managed navigation ({@code navigation.managed}).
 *
 * <p>Virtual Sites do not use this flag — they have their own Git/Markdown nav. Traditional sites
 * default to managed navigation when the property is absent.
 */
public final class PSManagedNavSiteHelper {

  /** Site property. {@code false} skips NavTree/homepage seed. Absent ⇒ {@code true}. */
  public static final String PROP_MANAGED = "navigation.managed";

  private PSManagedNavSiteHelper() {}

  /**
   * Create-time DTO flag. {@code null} means include managed navigation (product default).
   *
   * @param createFlag value from the create body; may be null
   * @return true when a NavTree should be created
   */
  public static boolean wantsManagedNavigation(Boolean createFlag) {
    return createFlag == null || createFlag;
  }

  /**
   * Persisted site. Virtual Sites never want CMS managed nav. Traditional sites honor {@link
   * #PROP_MANAGED} (default true).
   *
   * @param site may be null (treated as traditional default true)
   * @return true when CMS managed navigation is in effect
   */
  public static boolean wantsManagedNavigation(IPSSite site) {
    if (PSVirtualSiteHelper.isVirtual(site)) {
      return false;
    }
    return parseFlag(PSVirtualSiteHelper.findProperty(site, PROP_MANAGED).orElse(null), true);
  }

  /**
   * Wire/DTO value for traditional sites only. Virtual Sites omit the flag ({@code null}).
   *
   * @param site may be null
   * @return {@code Boolean.TRUE}/{@code FALSE} for traditional; {@code null} when virtual
   */
  public static Boolean flagForNonVirtual(IPSSite site) {
    if (site == null || PSVirtualSiteHelper.isVirtual(site)) {
      return null;
    }
    return wantsManagedNavigation(site);
  }

  static boolean parseFlag(String raw, boolean defaultValue) {
    if (StringUtils.isBlank(raw)) {
      return defaultValue;
    }
    String v = raw.trim();
    if ("false".equalsIgnoreCase(v) || "0".equals(v) || "no".equalsIgnoreCase(v)) {
      return false;
    }
    if ("true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v)) {
      return true;
    }
    return defaultValue;
  }
}
