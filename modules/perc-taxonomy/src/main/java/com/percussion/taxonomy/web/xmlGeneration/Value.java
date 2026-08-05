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

package com.percussion.taxonomy.web.xmlGeneration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Represents a localized value entry emitted by the taxonomy XML generator, holding the language id
 * and the value text.
 *
 * @author Steffen Gates
 */
public class Value {

  @Attribute(required = true)
  public int langID;

  @Text public String text;

  public Value() {}

  public Value(int langID, String text) {
    this.text = text;
    this.langID = langID;
  }
}
