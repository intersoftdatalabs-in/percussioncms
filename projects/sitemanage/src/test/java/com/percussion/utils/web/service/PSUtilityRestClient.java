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

package com.percussion.utils.web.service;

import com.percussion.share.data.PSMapWrapper;
import com.percussion.share.test.PSObjectRestClient;

/**
 * REST client for utility service.
 * Refactored for Java 11 and Google Java Style.
 */
public class PSUtilityRestClient extends PSObjectRestClient {

    private final String path = "/Rhythmyx/services/utils/utility/";

    public PSUtilityRestClient(String baseUrl) {
        super(baseUrl);
    }

    /**
     * Encrypts a string using the utility REST service.
     *
     * @param mapWrapper the map wrapper containing the string and key
     * @return the encrypted string in a map wrapper
     */
    public PSMapWrapper encryptString(PSMapWrapper mapWrapper) {
        return postObjectToPath(concatPath(path, "encryptstring"), mapWrapper, PSMapWrapper.class);
    }

    /**
     * Decrypts a string using the utility REST service.
     *
     * @param mapWrapper the map wrapper containing the encrypted string and key
     * @return the decrypted string in a map wrapper
     */
    public PSMapWrapper decryptString(PSMapWrapper mapWrapper) {
        return postObjectToPath(concatPath(path, "decryptstring"), mapWrapper, PSMapWrapper.class);
    }
}
