/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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

package com.percussion.cx;

/**
 * Represents a clipboard data item for JavaScript clipboard operations. Used to transfer data
 * between JavaScript and Java clipboard systems.
 */
public class JSClipDataItem {

  /** The kind of clip data, always "text" for this implementation. */
  public String kind = "text";

  /** The MIME type of the data (e.g., "text/plain"). */
  public String type;

  /** The actual data content. */
  public String data;

  /**
   * Creates a new JSClipDataItem with the specified type and data.
   *
   * @param type the MIME type of the data
   * @param data the data content
   */
  public JSClipDataItem(String type, String data) {
    this.type = type;
    this.data = data;
  }

  /**
   * Gets the kind of clip data.
   *
   * @return always returns "text"
   */
  public String getKind() {
    return "text";
  }

  /**
   * Gets the data as a string.
   *
   * @return the data content as a string
   */
  public String getAsString() {
    return this.data;
  }

  /**
   * Gets the MIME type of the data.
   *
   * @return the MIME type
   */
  public String getType() {
    return this.type;
  }

  @Override
  public String toString() {
    return "ClipDataItem{"
        + "kind='"
        + kind
        + '\''
        + ", type='"
        + type
        + '\''
        + ", data='"
        + data
        + '\''
        + '}';
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JSClipDataItem other = (JSClipDataItem) obj;
    if (type == null) {
      if (other.type != null) return false;
    } else if (!type.equals(other.type)) return false;
    return true;
  }
}
