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
package com.percussion.delivery.listeners;

import java.util.Set;

/**
 * @author erikserating
 *
 */
public interface IPSServiceDataChangeListener {
    /**
     * Called when data is changed as a result of an update or
     * delete and is committed to the repository. An insert will not fire this method.
     * @param sites the sites whose data was changed. Never blank.
     * @param services affected by the data change. Never {@code null} or empty.
     */
    void dataChanged(Set<String> sites, String[] services);

    /**
     * Called when a data change is requested but before the data is
     * actually committed to the repository.
     * @param sites the sites whose data change is requested. Never blank.
     * @param services affected by the data change. Never {@code null} or empty.
     */
    void dataChangeRequested(Set<String> sites, String[] services);
}
