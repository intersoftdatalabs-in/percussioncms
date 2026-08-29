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

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selects an {@link IPSVirtualSiteSource} for a registered {@link VirtualSiteSourceType}.
 *
 * <p>Used by {@link PSVirtualSiteBuildService} (CLI and CMS REST {@code POST
 * /sites/{nameOrId}/virtual/build}) so git-filesystem, csv-filesystem, sql-database, http-json,
 * object-storage, rss-atom, and icalendar share one assemble pipeline. REST GET/PUT persist {@code
 * http-json}, {@code object-storage}, and {@code rss-atom}. {@code icalendar} is SPI/CLI assemble
 * in this slice (REST persist later).
 */
public final class PSVirtualSiteSourceFactory {

  private PSVirtualSiteSourceFactory() {}

  /**
   * Create the adapter for {@code type}.
   *
   * @param type registered source kind, not null
   * @return new stateless source instance
   */
  public static IPSVirtualSiteSource create(VirtualSiteSourceType type) {
    Objects.requireNonNull(type, "type");
    return switch (type) {
      case GIT_FILESYSTEM -> new PSGitFilesystemVirtualSiteSource();
      case CSV_FILESYSTEM -> new PSCsvFilesystemVirtualSiteSource();
      case SQL_DATABASE -> new PSSqlDatabaseVirtualSiteSource();
      case HTTP_JSON -> new PSHttpJsonVirtualSiteSource();
      case OBJECT_STORAGE -> new PSObjectStorageVirtualSiteSource();
      case RSS_ATOM -> new PSRssAtomVirtualSiteSource();
      case ICALENDAR -> new PSIcalendarVirtualSiteSource();
    };
  }

  /**
   * Create the adapter for a property / CLI wire name.
   *
   * @param wireName e.g. {@code git-filesystem}, {@code csv-filesystem}, {@code sql-database},
   *     {@code http-json}, {@code object-storage}, {@code rss-atom}, or {@code icalendar}
   * @return new source
   * @throws VirtualSiteException when the name is blank or unknown
   */
  public static IPSVirtualSiteSource createFromWireName(String wireName)
      throws VirtualSiteException {
    VirtualSiteSourceType type = VirtualSiteSourceType.fromWireName(wireName);
    if (type == null) {
      throw new VirtualSiteException(
          "Unsupported virtual source kind '"
              + (wireName == null ? "" : wireName)
              + "'. Allowed: "
              + Stream.of(VirtualSiteSourceType.values())
                  .map(VirtualSiteSourceType::wireName)
                  .collect(Collectors.joining(", ")));
    }
    return create(type);
  }
}
