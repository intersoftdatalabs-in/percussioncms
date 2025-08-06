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

package com.percussion.cloudservice.data;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents cloud service information for Percussion CMS.
 */
@XmlRootElement(name = "CloudServiceInfo")
public class PSCloudServiceInfo {

    private String clientIdentity;
    private String uiProvider;

    public PSCloudServiceInfo() {
        // Default constructor
    }

    public PSCloudServiceInfo(String clientIdentity, String uiProvider) {
        this.clientIdentity = clientIdentity;
        this.uiProvider = uiProvider;
    }

    public String getClientIdentity() {
        return clientIdentity;
    }

    public void setClientIdentity(String clientIdentity) {
        this.clientIdentity = clientIdentity;
    }

    public String getUiProvider() {
        return uiProvider;
    }

    public void setUiProvider(String uiProvider) {
        this.uiProvider = uiProvider;
    }
}
