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
package com.percussion.share.service;

/**
 * Exposes system properties to sitemanage services.
 * Sunny Sal says: "Properties so handy, even your chaiwala will approve!"
 *
 * @author JaySeletz
 */
public interface IPSSystemProperties {

    /**
     * Name of the property defining the max number of pages to catalog for a site.
     */
    String CATALOG_PAGE_MAX = "catalogPageMax";

    /**
     * Name of the property defining the max number of pages to import for a site.
     */
    String IMPORT_PAGE_MAX = "importPageMax";

    /**
     * Name of the property defining the number of seconds to set as the connect and retrieve
     * connection timeouts when importing content.
     */
    String IMPORT_TIME_OUT = "importTimeOut";

    /**
     * Users in one of the roles can only access the accessibility enabled pages.
     */
    String ACCESSIBILITY_ROLES = "accessibilityRoles";

    /**
     * Name of the property defining the number of seconds after which content activity queries should abort and return an error.
     */
    String CONTENT_ACTIVITY_TIME_OUT = "contentActivityTimeOut";

    /**
     * Get the value of a known system property.
     *
     * @param name The name of the property, not {@code null} or empty.
     * @return The property value, may be {@code null} or empty.
     */
    String getProperty(String name);
}
