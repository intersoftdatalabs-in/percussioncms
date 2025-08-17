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
package com.percussion.pagemanagement.assembler;

/**
 * Represents the rendering result of an element in a region, which can be either a widget or a
 * subregion.
 *
 * <p>The rendering results of an entire region are a list of these objects.
 *
 * @author adamgent
 */
public class PSRegionResult {

  private String result;
  private PSRegionResultType type = PSRegionResultType.WIDGET;
  private boolean publishMode;

  /**
   * The widget instance for this region result. May be {@code null} if {@link #getType()} is {@link
   * PSRegionResultType#SUBREGION}.
   */
  private PSWidgetInstance widget;

  /** The cause of an error during rendering, if any. May be {@code null}. */
  private Throwable errorCause;

  /**
   * Gets the exception thrown during rendering, if any.
   *
   * @return the exception, or {@code null} if no exception was thrown.
   */
  public Throwable getErrorCause() {
    return errorCause;
  }

  /**
   * Sets the error cause and publish mode.
   *
   * @param errorCause the exception thrown during rendering.
   * @param publishMode whether this is in publish mode.
   */
  public void setErrorCause(Throwable errorCause, boolean publishMode) {
    this.errorCause = errorCause;
    this.publishMode = publishMode;
  }

  /**
   * Gets the widget instance for this region result.
   *
   * @return the widget instance, or {@code null} if this is a subregion.
   */
  public PSWidgetInstance getWidget() {
    return widget;
  }

  public void setWidget(PSWidgetInstance widget) {
    this.widget = widget;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public PSRegionResultType getType() {
    return type;
  }

  public void setType(PSRegionResultType type) {
    this.type = type;
  }

  /** Indicates whether this region result is a widget rendering or a subregion. */
  public enum PSRegionResultType {
    WIDGET,
    SUBREGION
  }

  /**
   * Overridden for ease of use in Velocity templates.
   *
   * @return the rendered result, or an error message if rendering failed.
   */
  @Override
  public String toString() {
    if (getErrorCause() != null) {
      if (!publishMode) {
        return "Error Displaying Contents. See logs for more details";
      } else {
        return "";
      }
    }
    return result;
  }
}
