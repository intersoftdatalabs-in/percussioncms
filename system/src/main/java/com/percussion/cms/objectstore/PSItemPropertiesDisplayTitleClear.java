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
package com.percussion.cms.objectstore;

import org.apache.commons.lang3.StringUtils;

/**
 * Lets one item-properties save store an empty {@code displaytitle} (#5297).
 *
 * <p>FastForward marks {@code displaytitle} required ({@code isValidSysDisplayTitle}). That rule
 * still applies to content-editor saves. Explorer clear opens this scope on the calling thread
 * only for the save that posts an empty title. {@link #close()} always clears the scope.
 */
public final class PSItemPropertiesDisplayTitleClear implements AutoCloseable {

  /** Request parameter copied onto the content-editor modify for the clear save. */
  public static final String REQUEST_PARAM = "sys_allowBlankDisplayTitle";

  private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

  private PSItemPropertiesDisplayTitleClear() {
    ACTIVE.set(Boolean.TRUE);
  }

  /** Arms the current thread. Close the result in a {@code finally}. */
  public static PSItemPropertiesDisplayTitleClear open() {
    return new PSItemPropertiesDisplayTitleClear();
  }

  public static boolean isActive() {
    return Boolean.TRUE.equals(ACTIVE.get());
  }

  /**
   * True only for the {@code displaytitle} field when the clear parameter is {@code yes}. Other
   * fields, and a display title without the parameter, keep the required-title rule.
   */
  public static boolean skipRequiredCheck(String fieldName, String allowParam) {
    return "displaytitle".equalsIgnoreCase(StringUtils.trimToEmpty(fieldName))
        && "yes".equalsIgnoreCase(StringUtils.trimToEmpty(allowParam));
  }

  @Override
  public void close() {
    ACTIVE.remove();
  }
}
