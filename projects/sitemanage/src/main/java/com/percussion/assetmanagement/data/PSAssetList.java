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

package com.percussion.assetmanagement.data;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

/** List wrapper for PSAsset objects. */
@XmlRootElement(name = "Asset")
@ArraySchema(schema = @Schema(implementation = PSAsset.class))
public class PSAssetList extends ArrayList<PSAsset> {
  private static final long serialVersionUID = 1L;


  public PSAssetList() {
    super();
  }

  public PSAssetList(Collection<? extends PSAsset> c) {
    super(c);
  }
}
