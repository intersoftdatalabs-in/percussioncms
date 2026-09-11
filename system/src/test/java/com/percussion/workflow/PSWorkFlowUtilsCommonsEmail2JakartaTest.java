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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Authenticator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.mail2.core.EmailConstants;
import org.apache.commons.mail2.jakarta.DefaultAuthenticator;
import org.apache.commons.mail2.jakarta.MultiPartEmail;
import org.junit.jupiter.api.Test;

/**
 * perc-system SMTP (workflow reports) must use Commons Email 2 Jakarta, which extends {@link
 * Authenticator}, not {@code javax.mail.Authenticator} from commons-email 1.6.
 */
public class PSWorkFlowUtilsCommonsEmail2JakartaTest {

  @Test
  public void defaultAuthenticatorIsJakartaMail() {
    DefaultAuthenticator auth = new DefaultAuthenticator("smtp-user", "smtp-pass");
    assertInstanceOf(Authenticator.class, auth);
  }

  @Test
  public void multiPartEmailAcceptsJakartaAuthenticatorWithoutSend() {
    MultiPartEmail email = new MultiPartEmail();
    email.setCharset(EmailConstants.UTF_8);
    email.setHostName("smtp.example.test");
    email.setSmtpPort(587);
    email.setStartTLSEnabled(true);
    email.setSSLOnConnect(false);
    email.setAuthenticator(new DefaultAuthenticator("smtp-user", "smtp-pass"));
    assertEquals("smtp.example.test", email.getHostName());
    assertEquals("587", email.getSmtpPort());
    assertTrue(email.isStartTLSEnabled());
    assertFalse(email.isSSLOnConnect());
  }

  @Test
  public void workflowUtilsSourceUsesCommonsEmail2Jakarta() throws Exception {
    Path src = locateWorkFlowUtilsSource();
    assertTrue(Files.isRegularFile(src), () -> "missing " + src.toAbsolutePath());
    String text = Files.readString(src, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertFalse(text.contains("import org.apache.commons.mail."));
    assertTrue(text.contains("import org.apache.commons.mail2.jakarta.DefaultAuthenticator;"));
    assertTrue(text.contains("import org.apache.commons.mail2.jakarta.MultiPartEmail;"));
    assertTrue(text.contains("import org.apache.commons.mail2.core.EmailException;"));
  }

  private static Path locateWorkFlowUtilsSource() {
    Path rel =
        Path.of("src", "main", "java", "com", "percussion", "workflow", "PSWorkFlowUtils.java");
    Path fromModule = Path.of("").toAbsolutePath().normalize().resolve(rel);
    if (Files.isRegularFile(fromModule)) {
      return fromModule;
    }
    Path fromRepo =
        Path.of("").toAbsolutePath().normalize().resolve("system").resolve(rel);
    if (Files.isRegularFile(fromRepo)) {
      return fromRepo;
    }
    throw new AssertionError(
        "PSWorkFlowUtils.java not found from cwd " + Path.of("").toAbsolutePath());
  }
}
