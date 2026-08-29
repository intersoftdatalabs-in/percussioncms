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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Content type icon strategy (CD-11).
 *
 * <p>{@code source} is {@code none}, {@code specified} (file path/name), or {@code fromFileField}
 * (file field name). {@code none} clears {@code value}. Non-{@code none} requires a non-blank
 * value. This envelope is path/name only — it does not upload icon binaries.
 *
 * <p>Jackson root wrap is {@code ContentTypeIcon} ({@code WRAP_ROOT_VALUE} / {@code
 * UNWRAP_ROOT_VALUE}).
 */
@XmlRootElement(name = "ContentTypeIcon")
@JsonRootName("ContentTypeIcon")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content type icon strategy (none, specified file, or from file field)")
public class ContentTypeIcon {

  /** No icon. Value is cleared. */
  public static final String SOURCE_NONE = "none";

  /** Icon is a specified file path or name. */
  public static final String SOURCE_SPECIFIED = "specified";

  /** Icon is derived from a file field (extension). */
  public static final String SOURCE_FROM_FILE_FIELD = "fromFileField";

  @Schema(
      required = true,
      description = "Icon source: none, specified, or fromFileField",
      allowableValues = {SOURCE_NONE, SOURCE_SPECIFIED, SOURCE_FROM_FILE_FIELD})
  private String source;

  @Schema(
      description =
          "Specified file path/name, or file field name. Omitted/empty when source is none.")
  private String value;

  public ContentTypeIcon() {}

  public ContentTypeIcon(String source, String value) {
    this.source = source;
    this.value = value;
  }

  /**
   * Returns whether {@code source} is one of the REST icon sources (case-insensitive).
   *
   * @param source candidate source, may be {@code null}
   * @return {@code true} when {@code none}, {@code specified}, or {@code fromFileField}
   */
  public static boolean isKnownSource(String source) {
    if (source == null) {
      return false;
    }
    String trimmed = source.trim();
    return SOURCE_NONE.equalsIgnoreCase(trimmed)
        || SOURCE_SPECIFIED.equalsIgnoreCase(trimmed)
        || SOURCE_FROM_FILE_FIELD.equalsIgnoreCase(trimmed);
  }

  /**
   * Returns whether {@code source} is {@code none} (case-insensitive).
   *
   * @param source candidate source, may be {@code null}
   * @return {@code true} when source is none
   */
  public static boolean isNone(String source) {
    return source != null && SOURCE_NONE.equalsIgnoreCase(source.trim());
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
