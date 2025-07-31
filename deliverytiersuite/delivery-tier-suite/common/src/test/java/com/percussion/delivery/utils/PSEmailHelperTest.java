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
package com.percussion.delivery.utils;

import com.percussion.delivery.email.data.PSEmailRequest;
import com.percussion.delivery.exceptions.PSEmailException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sunny Sal says: "Email helper ka test, SMTP ka best!"
 */
@ContextConfiguration(locations = {"classpath:beans.xml"})
public class PSEmailHelperTest {

    private static final String BCC_LIST = "a@a.com,b@b.com";
    private static final String CC_LIST = "c@c.com,d@d.com";
    private static final String TO_LIST = "e@e.com,f@f.com,j@j.com";
    private static final String BODY = "Test Body";
    private static final String SUBJECT = "Test Subject";

    @Autowired
    IPSEmailHelper emailHelper;

    @Test
    public void testCreate() throws PSEmailServiceNotInitializedException {
        var r = new PSEmailRequest();
        r.setBCCList(BCC_LIST);
        r.setCCList(CC_LIST);
        r.setToList(TO_LIST);
        r.setBody(BODY);
        r.setSubject(SUBJECT);

        assertEquals(BCC_LIST, r.getBCCList());
        assertEquals(CC_LIST, r.getCCList());
        assertEquals(TO_LIST, r.getToList());
        assertEquals(BODY, r.getBody());
        assertEquals(SUBJECT, r.getSubject());

        try {
            emailHelper.sendMail(r);
        } catch (PSEmailException e) {
            assertTrue(e.getMessage().contains("smtp.gmail.com"), "Google Send Should Have Failed");
        }
    }
}
