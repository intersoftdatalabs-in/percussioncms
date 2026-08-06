/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.test.utils;

import com.percussion.delivery.email.data.PSEmailRequest;
import com.percussion.delivery.exceptions.PSEmailException;
import com.percussion.delivery.test.utils.spring.PSEmailTestFakeSender;
import com.percussion.delivery.utils.PSEmailServiceNotInitializedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author natechadwick
 */
@Configuration("classpath:test-beans.xml")
@ExtendWith(SpringExtension.class)
public class PSEmailHelperTest {

  private static String BCC_LIST = "a@a.com,b@b.com";
  private static String CC_LIST = "c@c.com,d@d.com";
  private static String TO_LIST = "e@e.com,f@f.com,j@j.com";
  private static String BODY = "Test Body";
  private static String SUBJECT = "Test Subject";

  private PSEmailTestFakeSender sender = new PSEmailTestFakeSender();

  @Test
  public void testCreate() throws PSEmailServiceNotInitializedException {
    PSEmailRequest r = new PSEmailRequest();

    r.setBCCList(BCC_LIST);
    r.setCCList(CC_LIST);
    r.setToList(TO_LIST);
    r.setBody(BODY);
    r.setSubject(SUBJECT);

    Assertions.assertEquals(BCC_LIST, r.getBCCList());
    Assertions.assertEquals(CC_LIST, r.getCCList());
    Assertions.assertEquals(TO_LIST, r.getToList());
    Assertions.assertEquals(BODY, r.getBody());
    Assertions.assertEquals(SUBJECT, r.getSubject());

    try {
      this.sender.sendMail(r);
    } catch (PSEmailException e) {
      Assertions.assertTrue(
          e.getMessage().contains("smtp.gmail.com"), "Google Send Should Have Failed");
    }
  }
}
