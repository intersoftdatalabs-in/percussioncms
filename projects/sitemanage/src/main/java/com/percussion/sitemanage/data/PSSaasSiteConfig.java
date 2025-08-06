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
 * SaaS Site configuration wrapper.
 */
public class PSSaasSiteConfig {

    private PSSiteConfig siteConfig;

    /**
     * Gets the site config.
     *
     * @return Optional of site config, empty if not set.
     */
    public Optional<PSSiteConfig> getSiteConfig() {
        return Optional.ofNullable(siteConfig);
    }

    /**
     * Sets the site config.
     *
     * @param siteConfig the site config to set, must not be null.
     */
    public void setSiteConfig(PSSiteConfig siteConfig) {
        this.siteConfig = Objects.requireNonNull(siteConfig, "siteConfig must not be null");
    }
}
