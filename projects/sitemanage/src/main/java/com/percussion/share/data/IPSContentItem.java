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
package com.percussion.share.data;

import java.util.Map;

/**
 * A generic, low-level representation of an item in the system backed
 * by a Rhythmyx content item.
 *
 * <p>Provides access to all fields as a map, where the key is the field name and the value is the field value.
 *
 * @author adamgent
 */
public interface IPSContentItem extends IPSItemSummary {

    /**
     * Returns a map of all fields for this content item.
     * The key is the field name, and the value is the field value.
     *
     * @return a non-null map of field names to values
     */
    Map<String, Object> getFields();

    /**
     * Sets the fields for this content item.
     *
     * @param fields a non-null map of field names to values
     */
    void setFields(Map<String, Object> fields);
}
