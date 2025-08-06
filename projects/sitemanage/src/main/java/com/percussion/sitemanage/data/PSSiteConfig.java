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
package com.percussion.sitemanage.data;

import java.util.Objects;
import java.util.Optional;

/**
 * Site configuration data.
 */
public class PSSiteConfig {

    private PSSiteInfo siteInfo;
    private PSPublisherInfo publisherInfo;

    public Optional<PSSiteInfo> getSiteInfo() {
        return Optional.ofNullable(siteInfo);
    }

    public void setSiteInfo(PSSiteInfo siteInfo) {
        this.siteInfo = Objects.requireNonNull(siteInfo, "siteInfo must not be null");
    }

    public Optional<PSPublisherInfo> getPublisherInfo() {
        return Optional.ofNullable(publisherInfo);
    }

    public void setPublisherInfo(PSPublisherInfo publisherInfo) {
        this.publisherInfo = Objects.requireNonNull(publisherInfo, "publisherInfo must not be null");
    }
}
