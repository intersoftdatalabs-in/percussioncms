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
package com.percussion.ui.data;

import com.percussion.pathmanagement.data.PSPathItem;

import java.util.List;

/**
 * Criteria for display properties in UI lists.
 */
public class PSDisplayPropertiesCriteria {
    private List<PSPathItem> items;
    private PSSimpleDisplayFormat format;
    private boolean displayFormatRequired = true;

    public PSDisplayPropertiesCriteria(List<PSPathItem> items, PSSimpleDisplayFormat format) {
        this.items = items;
        this.format = format;
    }

    public List<PSPathItem> getItems() {
        return items;
    }

    public PSSimpleDisplayFormat getFormat() {
        return format;
    }

    public boolean isDisplayFormatRequired() {
        return displayFormatRequired;
    }

    public void setDisplayFormatRequired(boolean displayFormatRequired) {
        this.displayFormatRequired = displayFormatRequired;
    }
}
