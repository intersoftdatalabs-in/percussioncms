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
package com.percussion.activity.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.Serializable;
import java.util.Optional;

/**
 * A request object used for getting the content activity data from the rest service. 
 */
@JsonRootName(value = "ContentActivityRequest")
public class PSContentActivityRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String path;
    private String durationType;
    private String duration;

    /**
     * @return the path may be <code>null</code> or empty.
     */
    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the duration type may be <code>null</code> or empty.
     */
    public Optional<String> getDurationType() {
        return Optional.ofNullable(durationType);
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    /**
     * @return the duration may be <code>null</code> or empty.
     */
    public Optional<String> getDuration() {
        return Optional.ofNullable(duration);
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
