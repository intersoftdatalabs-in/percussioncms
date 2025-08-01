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
package com.percussion.user.service.impl;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSPasswordHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils; // Modernized: Use lang3
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional; // Java 11 Optional

/**
 * Default password encryption bean for user passwords.
 * Provides encryption and legacy encryption for backward compatibility.
 *
 * @author DavidBenua
 */
@Component("defaultPasswordEncryptionBean")
@Lazy
public class PSDefaultPasswordEncryptionBean implements IPSPasswordFilter
{
    @Override
    public String encrypt(String password)
    {
        // Use Optional and Java 11 features for clarity and null safety
        return Optional.ofNullable(password)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(pw -> {
                    try {
                        return PSPasswordHandler.getHashedPassword(pw);
                    } catch (PSEncryptionException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public String getAlgorithm() {
        return PSPasswordHandler.ALGORITHM;
    }

    @Override
    public void init(@SuppressWarnings("unused") IPSExtensionDef def, @SuppressWarnings("unused") File codeRoot)
    {
        // No initialization required.
    }

    /**
     * Encrypts the password using the legacy hashing routine.
     * Allows security providers to re-encrypt passwords on login after a security update.
     *
     * @param password the password to encrypt
     * @return the legacy-encrypted password
     */
    @SuppressFBWarnings("WEAK_MESSAGE_DIGEST_SHA1")
    @Override
    @Deprecated
    public String legacyEncrypt(String password) {
        // Use Optional for null/blank handling
        return Optional.ofNullable(password)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(DigestUtils::shaHex)
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public String getLegacyAlgorithm() {
        return "SHA-1";
    }
}
