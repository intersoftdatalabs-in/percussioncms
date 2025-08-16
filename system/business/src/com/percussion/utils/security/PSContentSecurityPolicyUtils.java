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
package com.percussion.utils.security;

import com.percussion.delivery.data.PSDeliveryInfo;
import java.util.List;

/**
 * Java 11 refactored: Utility for editing Content Security Policy strings for delivery servers.
 * <p>
 * Uses Google Java Style and modern Java 11 features.
 * All methods are static and thread-safe.
 */
public class PSContentSecurityPolicyUtils {
    /**
     * Edits the Content Security Policy string to include delivery server URLs in the frame-src directive.
     * @param psDeliveryInfoList list of delivery info objects
     * @param contentSecurityString the original CSP string
     * @return the updated CSP string
     */
    public static String editContentSecurityPolicy(List<PSDeliveryInfo> psDeliveryInfoList, String contentSecurityString) {
        var serverString = new StringBuilder();
        for (var psDeliveryInfo : psDeliveryInfoList) {
            serverString.append(psDeliveryInfo.getUrl()).append(" ");
            serverString.append(psDeliveryInfo.getUrl()).append("/* ");
        }
        if (contentSecurityString.contains("frame-src")) {
            contentSecurityString = contentSecurityString.replace("frame-src", "frame-src " + serverString);
        } else {
            if (contentSecurityString.endsWith(";")) {
                contentSecurityString = contentSecurityString + " frame-src 'self' " + serverString + ";";
            } else {
                contentSecurityString = contentSecurityString + "; frame-src 'self' " + serverString + ";";
            }
        }
        return contentSecurityString;
    }
}
