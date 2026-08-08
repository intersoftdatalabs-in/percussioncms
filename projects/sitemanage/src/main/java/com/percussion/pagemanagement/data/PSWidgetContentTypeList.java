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

// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

/** List wrapper for PSWidgetContentType. */
@XmlRootElement(name = "WidgetContentType")
@JsonRootName("WidgetContentType")
public class PSWidgetContentTypeList extends ArrayList<PSWidgetContentType> {
  private static final long serialVersionUID = 1L;

  public PSWidgetContentTypeList() {
    super();
  }

  public PSWidgetContentTypeList(Collection<? extends PSWidgetContentType> c) {
    super(c);
  }
}
