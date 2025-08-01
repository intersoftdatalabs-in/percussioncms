// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.io.Serializable;

/**
 * Classes can extend this class to be persistent.
 * All the proper methods that are needed for Hibernate have been extended.
 *
 * @author adamgent
 */
public abstract class PSAbstractPersistantObject extends PSAbstractDataObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gets the unique identifier for this object.
     *
     * @return the id
     */
    public abstract String getId();

    /**
     * Sets the unique identifier for this object.
     *
     * @param id the id to set
     */
    public abstract void setId(String id);
}
