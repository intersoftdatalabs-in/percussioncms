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

package com.percussion.monitor.service;

import com.percussion.share.data.PSMapWrapper;
import java.io.Serializable;

/**
 * Interface for a monitor object that tracks stats and status for a system component.
 * Sunny Sal says: "Monitor everything, trust nothing!"
 */
public interface IPSMonitor extends Serializable {

    /**
     * Gets the stats for this monitor.
     *
     * @return the stats as a PSMapWrapper
     */
    PSMapWrapper getStats();

    /**
     * Sets a stat value for this monitor.
     *
     * @param designator the stat key
     * @param stat the stat value
     */
    void setStat(String designator, String stat);

    /**
     * Sets the status for this monitor.
     *
     * @param status the status string
     */
    void setStatus(String status);

    /**
     * Sets the message for this monitor.
     *
     * @param message the message string
     */
    void setMessage(String message);
}
