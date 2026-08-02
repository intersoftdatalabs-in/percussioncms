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
package com.percussion.distribution.gcm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lockstep source of truth for GCM FFI natives staged into {@code <installdir>/bin}.
 *
 * <p>Must stay aligned with:
 *
 * <ul>
 *   <li>{@code modules/perc-distribution-tree/pom.xml} — {@code mkd.gcm.version}, {@code
 *       mkd-gcm-natives} dependency, {@code stage-gcm-natives} unpack
 *   <li>{@code installDistributionFiles.xml} — {@code <available>} fail checks and {@code
 *       <include>} patterns under the GCM staging block
 *   <li>Upstream {@code dev.monkeyking:mkd-gcm-natives} layout under {@code
 *       dev/monkeyking/gcm/native/&lt;platform&gt;/}
 * </ul>
 *
 * <p>macOS is intentionally absent until the upstream natives jar ships Darwin libraries.
 */
final class BundledGcmNatives {

  /** Property name in module {@code pom.xml}. */
  static final String POM_VERSION_PROPERTY = "mkd.gcm.version";

  /** Maven coordinates for the natives artifact. */
  static final String GROUP_ID = "dev.monkeyking";

  static final String ARTIFACT_ID = "mkd-gcm-natives";

  /**
   * Platform directory (under {@code dev/monkeyking/gcm/native/}) → shared-library filename that
   * must land in {@code distribution/bin/}.
   */
  static final Map<String, String> PLATFORM_TO_FILENAME;

  /** Filenames that ANT must {@code <include>} when flattening into {@code bin/}. */
  static final Set<String> INCLUDE_FILENAMES;

  /** Relative paths used by ANT {@code <available file="..."/>} fail gates. */
  static final Set<String> AVAILABLE_RELATIVE_PATHS;

  static {
    Map<String, String> platforms = new LinkedHashMap<>();
    platforms.put("windows-x86_64", "mkd_gcm_ffi.dll");
    platforms.put("linux-x86_64", "libmkd_gcm_ffi.so");
    PLATFORM_TO_FILENAME = Collections.unmodifiableMap(platforms);

    Set<String> includes = new LinkedHashSet<>();
    Set<String> available = new LinkedHashSet<>();
    for (Map.Entry<String, String> e : PLATFORM_TO_FILENAME.entrySet()) {
      includes.add(e.getValue());
      available.add(
          "_gcm-native-stage/dev/monkeyking/gcm/native/" + e.getKey() + "/" + e.getValue());
    }
    INCLUDE_FILENAMES = Collections.unmodifiableSet(includes);
    AVAILABLE_RELATIVE_PATHS = Collections.unmodifiableSet(available);
  }

  private BundledGcmNatives() {}
}
