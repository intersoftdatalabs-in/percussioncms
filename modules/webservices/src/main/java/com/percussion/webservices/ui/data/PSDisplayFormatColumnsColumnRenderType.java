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
package com.percussion.webservices.ui.data;

/** Simple enum-like type for column render type. */
public class PSDisplayFormatColumnsColumnRenderType {
  public static final String _number = "number";
  public static final String _image = "image";
  public static final String _date = "date";
  public static final String _text = "text";

  private final String value;

  public PSDisplayFormatColumnsColumnRenderType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static PSDisplayFormatColumnsColumnRenderType fromString(String v) {
    if (v == null) return new PSDisplayFormatColumnsColumnRenderType(_text);
    String s = v.toLowerCase();
    switch (s) {
      case _number:
        return new PSDisplayFormatColumnsColumnRenderType(_number);
      case _image:
        return new PSDisplayFormatColumnsColumnRenderType(_image);
      case _date:
        return new PSDisplayFormatColumnsColumnRenderType(_date);
      default:
        return new PSDisplayFormatColumnsColumnRenderType(_text);
    }
  }
}
