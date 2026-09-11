/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.email.data.PSEmailRequest;
import jakarta.mail.Authenticator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.commons.mail2.jakarta.DefaultAuthenticator;
import org.apache.commons.mail2.jakarta.MultiPartEmail;
import org.junit.jupiter.api.Test;

/** DTS mail helper must compile against Commons Email 2 Jakarta + {@code jakarta.mail.*}. */
public class PSEmailHelperCommonsEmail2JakartaTest {

  @Test
  public void defaultAuthenticatorIsJakartaMail() {
    assertInstanceOf(Authenticator.class, new DefaultAuthenticator("u", "p"));
  }

  @Test
  public void tlsAndSslUseJakartaEmail2Setters() {
    MultiPartEmail email = new MultiPartEmail();
    email.setHostName("smtp.example.test");
    email.setStartTLSEnabled(true);
    email.setSSLOnConnect(true);
    email.setSslSmtpPort("465");
    assertEquals("smtp.example.test", email.getHostName());
    assertTrue(email.isStartTLSEnabled());
    assertTrue(email.isSSLOnConnect());
    assertEquals("465", email.getSslSmtpPort());
  }

  @Test
  public void missingHostDoesNotInitializeClient() {
    Properties props = new Properties();
    PSEmailHelper helper = new PSEmailHelper(props);
    PSEmailRequest request = new PSEmailRequest();
    request.setToList("to@example.test");
    request.setSubject("s");
    request.setBody("b");
    assertThrows(PSEmailServiceNotInitializedException.class, () -> helper.sendMail(request));
  }

  @Test
  public void helperSourceUsesCommonsEmail2JakartaApis() throws Exception {
    Path src =
        Path.of("src", "main", "java", "com", "percussion", "delivery", "utils", "PSEmailHelper.java");
    Path fromModule = Path.of("").toAbsolutePath().normalize().resolve(src);
    Path resolved =
        Files.isRegularFile(fromModule)
            ? fromModule
            : Path.of("")
                .toAbsolutePath()
                .normalize()
                .resolve("deliverytiersuite")
                .resolve("delivery-tier-suite")
                .resolve("common")
                .resolve(src);
    assertTrue(Files.isRegularFile(resolved), () -> "missing " + resolved);
    String text = Files.readString(resolved, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertFalse(text.contains("import org.apache.commons.mail."));
    assertFalse(text.contains(".setTLS("));
    assertFalse(text.contains(".setSSL("));
    assertTrue(text.contains("setStartTLSEnabled"));
    assertTrue(text.contains("setSSLOnConnect"));
  }
}
