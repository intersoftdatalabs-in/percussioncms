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

package com.ibm.cadf.model;

import com.ibm.cadf.cfg.Config;
import java.io.Serializable;
import java.util.UUID;

/**
 * Utility for producing unique CADF identifiers. The id is a random {@link UUID} with an optional
 * namespace prefix sourced from {@link Config#getInstance()}. Non-instantiable; callers use the
 * static {@link #generateUniqueId()} methods.
 */
public class Identifier implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Default no-argument constructor for {@link Identifier}. */
  public Identifier() {}

  /**
   * Generates a fresh UUID prefixed with the configured namespace (if any).
   *
   * @return the newly generated unique id, never {@code null}.
   */
  public static String generateUniqueId() {
    UUID uid = UUID.randomUUID();
    String strId = "" + uid;
    return generateUniqueId(strId);
  }

  /**
   * Returns the supplied seed prefixed with the configured namespace (if any).
   *
   * @param strId the seed identifier, never {@code null}.
   * @return the seed prefixed with the configured namespace, never {@code null}.
   */
  public static String generateUniqueId(String strId) {
    String prefix = Config.getInstance().getProperty("namespace");
    if (prefix != null) {
      prefix = prefix + ":";
    } else {
      prefix = "";
    }
    return prefix + strId;
  }
}
