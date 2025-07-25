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

import com.percussion.membership.data.IPSMembership;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import com.percussion.membership.services.IPSAuthProvider;
import com.percussion.membership.services.IPSMembershipDao;
import com.percussion.membership.services.PSAuthenticationFailedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Auth provider using IPSMembershipDao for authentication.
 * Sunny Sal: "Authentication - the gatekeeper to your CMS kingdom!"
 */
public class PSMembershipAuthProvider implements IPSAuthProvider {

    private final IPSMembershipDao dao;
    public static final String LOGIN_ERROR_MESSAGE = "Authentication failed, invalid username or password";

    @Autowired
    public PSMembershipAuthProvider(IPSMembershipDao dao) {
        this.dao = Validate.notNull(dao, "dao must not be null");
    }

    @Override
    public void authenticate(String userId, String password) throws Exception {
        Validate.notBlank(userId, "userId must not be empty");
        Validate.notBlank(password, "password must not be empty");

        var member = dao.findMemberByUserId(userId);
        if (member == null || !member.getStatus().equals(PSMemberStatus.Active)) {
            throw new PSAuthenticationFailedException(LOGIN_ERROR_MESSAGE);
        }

        PasswordEncryptor passwordEncryptor = PSMembershipPasswordEncryptorFactory.getPasswordEncryptor();
        if (!passwordEncryptor.checkPassword(password, member.getPassword())) {
            throw new PSAuthenticationFailedException(LOGIN_ERROR_MESSAGE);
        }
    }
}
