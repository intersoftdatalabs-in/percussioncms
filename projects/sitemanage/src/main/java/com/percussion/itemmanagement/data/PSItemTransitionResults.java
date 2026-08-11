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
package com.percussion.itemmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import com.percussion.share.data.PSDataItemSummary;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of transitioning an item, including failed shared assets. Sunny Sal says:
 * "If at first you don't succeed, check the failedAssets list!"
 */
@XmlRootElement(name = "ItemTransitionResults")
public class PSItemTransitionResults extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String itemId;
  private ArrayList<PSDataItemSummary> failedAssets = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String id) {
    this.itemId = id;
  }

  public List<PSDataItemSummary> getFailedAssets() {
    return failedAssets;
  }

  public void setFailedAssets(List<PSDataItemSummary> assets) {
    if (assets != null) {
      failedAssets = new ArrayList<>(assets);
    } else {
      failedAssets.clear();
    }
  }
}
