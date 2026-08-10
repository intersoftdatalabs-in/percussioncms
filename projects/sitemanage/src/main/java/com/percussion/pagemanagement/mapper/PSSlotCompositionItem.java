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
package com.percussion.pagemanagement.mapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered content item inside a {@link PSSlotCompositionNode}, typically a CM1 widget instance
 * upgraded onto a unified slot composition (instance layout/style overrides).
 */
public final class PSSlotCompositionItem {

  private final String widgetInstanceId;
  private final String definitionId;
  private final Map<String, Object> layoutOverrides;
  private final Map<String, Object> styleOverrides;

  /**
   * @param widgetInstanceId may be {@code null} when not yet assigned
   * @param definitionId may be {@code null} when unknown
   * @param layoutOverrides never {@code null} (copied)
   * @param styleOverrides never {@code null} (copied)
   */
  public PSSlotCompositionItem(
      String widgetInstanceId,
      String definitionId,
      Map<String, Object> layoutOverrides,
      Map<String, Object> styleOverrides) {
    this.widgetInstanceId = widgetInstanceId;
    this.definitionId = definitionId;
    this.layoutOverrides =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(layoutOverrides, "layoutOverrides")));
    this.styleOverrides =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(styleOverrides, "styleOverrides")));
  }

  public String getWidgetInstanceId() {
    return widgetInstanceId;
  }

  public String getDefinitionId() {
    return definitionId;
  }

  public Map<String, Object> getLayoutOverrides() {
    return layoutOverrides;
  }

  public Map<String, Object> getStyleOverrides() {
    return styleOverrides;
  }
}
