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

package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;

/**
 * JSON body for path/folder children: {@code {"PathItem":[...]}} (same shape as {@code
 * PSSiteSummaryList} / {@code {"SiteSummary":[...]}}).
 *
 * <p>Returned from REST via {@code Response.ok(list).type(APPLICATION_JSON)} so Jackson writes the
 * list. Returning {@code List&lt;PSPathItem&gt;} as the resource method type previously selected a
 * JAXB collection writer that failed with IllegalAnnotationExceptions for non-empty Sites (#2989).
 * No JAXB {@code @XmlRootElement} on this ArrayList subclass.
 */
@ArraySchema(schema = @Schema(implementation = PSPathItem.class))
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.DEFAULT)
@JsonRootName("PathItem")
public class PSPathItemList extends ArrayList<PSPathItem> {
  private static final long serialVersionUID = 1L;

  public PSPathItemList() {
    super();
  }

  public PSPathItemList(Collection<? extends PSPathItem> c) {
    super(c);
  }
}
