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
package com.percussion.share.dao.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.data.IPSContentItem;
import com.percussion.share.data.PSDataItemSummary;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic low-level representation of an item in the system backed by a Rhythmyx content item.
 */
public class PSContentItem extends PSDataItemSummary implements IPSContentItem {

  /** Never null. */
  private HashMap<String, Object> fields = new HashMap<>();

  /** {@inheritDoc} */
  @Override
  public Map<String, Object> getFields() {
    return fields;
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public void setFields(Map<String, Object> fields) {
    notNull(fields, "fields");
    if (fields instanceof HashMap) {
      this.fields = (HashMap<String, Object>) fields;
    } else {
      this.fields = new HashMap<>(fields);
    }
  }

  /** Not safe to serialize. */
  private static final long serialVersionUID = -3451673795623212592L;
}
