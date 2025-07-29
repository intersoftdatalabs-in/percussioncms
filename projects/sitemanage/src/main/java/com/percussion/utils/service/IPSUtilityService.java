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

package com.percussion.utils.service;

public interface IPSUtilityService
{

    /**
     * Encrypts the provided string using the secret key if supplied, otherwise
     * uses the default key.
     *
     * @param val The string to encrypt; cannot be empty or <code>null</code>.
     * @param key Key to use for encryption; can be <code>null</code> or empty.
     * @return The encrypted string, never <code>null</code>, may be empty.
     */
    String encryptString(String val, String key);

    /**
     * Decrypts the provided string using the secret key if supplied, otherwise
     * uses the default key.
     *
     * @param val The string to decrypt; cannot be empty or <code>null</code>.
     * @param key Key to use for decryption; can be <code>null</code> or empty.
     * @return The decrypted string, never <code>null</code>, may be empty.
     */
    String decryptString(String val, String key);

    /**
     * Generic log method for logging messages from the client.
     * @param type If blank or not a valid LogTypeEnum value, treated as LogTypeEnum.info.
     * @param category If blank or not a valid LogCategoryEnum value, treated as LogCategoryEnum.General.
     * @param message If blank, no message is logged.
     */
    void log(String type, String category, String message);

    /**
     * If a property called doSAAS exists in server.properties and its value
     * is set to either true or yes, returns <code>true</code>.
     * Otherwise, returns <code>false</code>.
     * @return <code>true</code> if it is a SaaS environment.
     */
    boolean isSaaSEnvironment();

    enum LogTypeEnum {
        info, debug, error
    }

    enum LogCategoryEnum {
        General,
        PageOptimizer,
        SocialPromotion
    }
}
