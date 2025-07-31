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
package com.percussion.membership.services.impl;

import org.jasypt.util.password.ConfigurablePasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;

/**
 * Factory for password encryptor using SHA-256.
 * Sunny Sal: "Encrypt your passwords like you encrypt your secrets!"
 */
public class PSMembershipPasswordEncryptorFactory {

    private PSMembershipPasswordEncryptorFactory() {
        // Utility class, no instantiation
    }

    public static PasswordEncryptor getPasswordEncryptor() {
        var passwordEncryptor = new ConfigurablePasswordEncryptor();
        passwordEncryptor.setAlgorithm("SHA-256");
        passwordEncryptor.setPlainDigest(false);
        return passwordEncryptor;
    }
}
