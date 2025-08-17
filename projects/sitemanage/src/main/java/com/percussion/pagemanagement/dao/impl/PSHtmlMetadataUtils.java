// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pagemanagement.dao.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.pagemanagement.data.IPSHtmlMetadata;
import java.util.Map;

/** Utilities for {@link IPSHtmlMetadata}. */
public class PSHtmlMetadataUtils {

  /**
   * Copies metadata to a map of fields.
   *
   * @param meta never {@code null}.
   * @param f never {@code null}.
   */
  public static void toMap(IPSHtmlMetadata meta, Map<String, Object> f) {
    notNull(meta);
    notNull(f);
    f.put("code_insert_after_body_start", meta.getAfterBodyStartContent());
    f.put("code_insert_before_body_close", meta.getBeforeBodyCloseContent());
    f.put("additional_head_content", meta.getAdditionalHeadContent());
    f.put("protected_region", meta.getProtectedRegion());
    f.put("protected_region_text", meta.getProtectedRegionText());
  }

  /**
   * Copies metadata from a map of fields.
   *
   * @param meta never {@code null}.
   * @param f never {@code null}.
   */
  public static void fromMap(IPSHtmlMetadata meta, Map<String, Object> f) {
    notNull(meta);
    notNull(f);
    var addHeadContent = (String) f.get("additional_head_content");
    var afterBodyStart = (String) f.get("code_insert_after_body_start");
    var beforeBodyClose = (String) f.get("code_insert_before_body_close");
    var protectedRegion = (String) f.get("protected_region");
    var protectedRegionText = (String) f.get("protected_region_text");
    meta.setAdditionalHeadContent(addHeadContent);
    meta.setAfterBodyStartContent(afterBodyStart);
    meta.setBeforeBodyCloseContent(beforeBodyClose);
    meta.setProtectedRegion(protectedRegion);
    meta.setProtectedRegionText(protectedRegionText);
  }

  /**
   * Copies all metadata fields from one object to another.
   *
   * @param from never {@code null}.
   * @param to never {@code null}.
   */
  public static void copy(IPSHtmlMetadata from, IPSHtmlMetadata to) {
    notNull(from);
    notNull(to);
    to.setAdditionalHeadContent(from.getAdditionalHeadContent());
    to.setAfterBodyStartContent(from.getAfterBodyStartContent());
    to.setBeforeBodyCloseContent(from.getBeforeBodyCloseContent());
    to.setProtectedRegion(from.getProtectedRegion());
    to.setProtectedRegionText(from.getProtectedRegionText());
    to.setDocType(from.getDocType());
  }
}
