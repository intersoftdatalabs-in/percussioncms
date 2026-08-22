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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Pure helpers for TemplateDef package install ID reservation (issue #3727 / parent #2813).
 *
 * <p>Free of Spring so unit tests run without a CMS context. On a <em>first assign</em> (new
 * mapping, unused UUID) the archive source UUID is kept so {@code perc.Baseline} system templates
 * install as {@code 0-4-602}..{@code 0-4-614}. Existing customer/snapshot rows are matched by name
 * before reservation and are never remapped here.
 */
public final class PSTemplateDefInstallUtils {

  /**
   * Expected assembly GUIDs for {@code perc.Baseline} system templates on a fresh 8.2 install
   * ({@code TemplateDef-N} → {@code 0-4-N}). Existing databases keep whatever UUID was assigned on
   * first install.
   */
  public static final Map<String, String> BASELINE_SYSTEM_TEMPLATE_GUIDS =
      Map.of(
          "perc.page", "0-4-602",
          "perc.pageDatabase", "0-4-604",
          "perc.pageDispatcher", "0-4-606",
          "perc.pageXml", "0-4-608",
          "perc.sys.resource", "0-4-610",
          "perc.widget", "0-4-612",
          "perc.widgetDispatcher", "0-4-614");

  private PSTemplateDefInstallUtils() {
    // utility
  }

  /**
   * Whether a new TemplateDef mapping should keep the archive source UUID instead of allocating
   * the next {@code variantid} from the GUID manager.
   *
   * @param sourceId archive dependency id (typically {@code 602} for {@code TemplateDef-602})
   * @param sourceUuidInUse {@code true} when that UUID already exists on the target or is reserved
   *     as another mapping's target
   * @return {@code true} to keep {@code sourceId} as the target id
   */
  public static boolean shouldKeepSourceUuid(String sourceId, boolean sourceUuidInUse) {
    return resolveKeptSourceId(sourceId, sourceUuidInUse) != null;
  }

  /**
   * Source UUID to assign as the mapping target, or {@code null} when the caller must allocate a
   * new sequential id.
   *
   * @param sourceId archive dependency id
   * @param sourceUuidInUse whether the UUID is already claimed
   * @return trimmed source id, or {@code null}
   */
  public static String resolveKeptSourceId(String sourceId, boolean sourceUuidInUse) {
    if (sourceId == null || sourceId.isBlank()) {
      return null;
    }
    String trimmed = sourceId.trim();
    try {
      templateUuid(trimmed);
    } catch (RuntimeException e) {
      return null;
    }
    if (sourceUuidInUse) {
      return null;
    }
    return trimmed;
  }

  /**
   * First-assign path: if the mapping is a new object with no target yet and the source UUID is
   * unused, set the target to the archive source id.
   *
   * @param mapping current id mapping, not {@code null}
   * @param sourceUuidInUse whether the source UUID is already claimed
   * @return {@code true} if the mapping target was set to the source UUID
   */
  public static boolean tryKeepSourceUuid(PSIdMapping mapping, boolean sourceUuidInUse) {
    Objects.requireNonNull(mapping, "mapping");
    if (!mapping.isNewObject() || mapping.getTargetId() != null) {
      return false;
    }
    String kept = resolveKeptSourceId(mapping.getSourceId(), sourceUuidInUse);
    if (kept == null) {
      return false;
    }
    String name = mapping.getSourceName();
    if (name == null || name.isBlank()) {
      name = kept;
    }
    mapping.setTarget(kept, name);
    return true;
  }

  /**
   * Whether another mapping in {@code idMap} already reserved {@code sourceId} as a TemplateDef
   * target (same UUID, including encoded {@link PSGuid#longValue()} forms).
   *
   * @param idMap current install id map, not {@code null}
   * @param sourceId UUID being considered, may be blank
   * @param objectType dependency type to match (typically {@code TemplateDef})
   * @return {@code true} if another mapping already claims that UUID
   */
  public static boolean isUuidReservedAsTarget(PSIdMap idMap, String sourceId, String objectType) {
    Objects.requireNonNull(idMap, "idMap");
    if (objectType == null || objectType.isBlank()) {
      throw new IllegalArgumentException("objectType may not be null or empty");
    }
    if (sourceId == null || sourceId.isBlank()) {
      return false;
    }
    Iterator<PSIdMapping> mappings = idMap.getMappings();
    while (mappings.hasNext()) {
      PSIdMapping mapping = mappings.next();
      if (mapping == null || mapping.getTargetId() == null) {
        continue;
      }
      if (!objectType.equals(mapping.getObjectType())) {
        continue;
      }
      if (sourceId.equals(mapping.getSourceId())) {
        continue;
      }
      if (sameTemplateUuid(sourceId, mapping.getTargetId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compare two TemplateDef ids as UUID values ({@code 602}, {@code 0-4-602}, or encoded longs).
   *
   * @param left first id
   * @param right second id
   * @return {@code true} when both parse and share the same template UUID
   */
  public static boolean sameTemplateUuid(String left, String right) {
    if (left == null || right == null || left.isBlank() || right.isBlank()) {
      return false;
    }
    try {
      return templateUuid(left) == templateUuid(right);
    } catch (RuntimeException e) {
      return left.trim().equals(right.trim());
    }
  }

  /**
   * Assembly GUID string form ({@code 0-4-N}) for a TemplateDef dependency id.
   *
   * @param sourceId archive id such as {@code 602} or {@code 0-4-602}
   * @return {@code host-type-uuid} string, never {@code null}
   */
  public static String assemblyGuidString(String sourceId) {
    if (sourceId == null || sourceId.isBlank()) {
      throw new IllegalArgumentException("sourceId may not be null or empty");
    }
    return new PSGuid(PSTypeEnum.TEMPLATE, sourceId.trim()).toString();
  }

  static int templateUuid(String id) {
    return new PSGuid(PSTypeEnum.TEMPLATE, id.trim()).getUUID();
  }
}
