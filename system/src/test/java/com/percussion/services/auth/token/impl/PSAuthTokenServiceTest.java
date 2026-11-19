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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.auth.token.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class PSAuthTokenServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-18T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Test
  void issueTokenStoresOnlySha256HashInvalidatesPriorAndRedactsRawToken() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    when(dao.saveToken(any())).thenAnswer(inv -> inv.getArgument(0));

    PSAuthTokenService service = new PSAuthTokenService(dao, Properties::new, FIXED_CLOCK);
    PSIssuedAuthToken issued =
        service.issueToken(
            PSAuthToken.TYPE_EMAIL_VERIFY,
            "alice",
            "alice@example.com",
            "alice",
            "127.0.0.1",
            Duration.ofMinutes(30));

    verify(dao)
        .invalidateUnconsumedTokens("alice", PSAuthToken.TYPE_EMAIL_VERIFY, FIXED_NOW);

    ArgumentCaptor<PSAuthToken> captor = ArgumentCaptor.forClass(PSAuthToken.class);
    verify(dao).saveToken(captor.capture());
    PSAuthToken stored = captor.getValue();

    assertEquals(PSAuthTokenService.sha256Hex(issued.rawToken()), stored.getTokenHash());
    assertNotEquals(issued.rawToken(), stored.getTokenHash());
    assertEquals("alice", stored.getUserName());
    assertEquals("alice@example.com", stored.getTargetEmail());
    assertEquals(FIXED_NOW.plus(Duration.ofMinutes(30)), issued.expiresAt());
    assertFalse(issued.toString().contains(issued.rawToken()));
  }

  @Test
  void consumeTokenRequiresAtomicUpdateRowCountOfOne() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    String raw = "sample-secret-token";
    String hash = PSAuthTokenService.sha256Hex(raw);

    PSAuthToken row = new PSAuthToken();
    row.setTokenId("tok-1");
    row.setTokenHash(hash);
    row.setTokenType(PSAuthToken.TYPE_EMAIL_VERIFY);
    row.setUserName("alice");
    row.setTargetEmail("new@example.com");
    row.setCreatedBy("alice");

    when(dao.consumeAuthTokenAtomically(eq(hash), eq(PSAuthToken.TYPE_EMAIL_VERIFY), eq(FIXED_NOW)))
        .thenReturn(1)
        .thenReturn(0);
    when(dao.findTokenByHash(hash)).thenReturn(Optional.of(row));

    PSAuthTokenService service = new PSAuthTokenService(dao, Properties::new, FIXED_CLOCK);
    Optional<PSConsumedAuthToken> first =
        service.consumeToken(raw, PSAuthToken.TYPE_EMAIL_VERIFY);
    Optional<PSConsumedAuthToken> replay =
        service.consumeToken(raw, PSAuthToken.TYPE_EMAIL_VERIFY);

    assertTrue(first.isPresent());
    assertEquals("alice", first.get().userName());
    assertEquals("new@example.com", first.get().targetEmail());
    assertTrue(replay.isEmpty());
    assertTrue(service.consumeToken("   ", PSAuthToken.TYPE_EMAIL_VERIFY).isEmpty());
  }

  @Test
  void resolveTtlClampsConfiguredValuesAndUsesDefaults() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    Properties props = new Properties();
    props.setProperty(PSAuthTokenService.PROP_EMAIL_VERIFY_TTL_MINUTES, "1");
    props.setProperty(PSAuthTokenService.PROP_PASSWORD_RESET_TTL_MINUTES, "9999");

    PSAuthTokenService service = new PSAuthTokenService(dao, () -> props, FIXED_CLOCK);
    assertEquals(Duration.ofMinutes(5), service.resolveTtl(PSAuthToken.TYPE_EMAIL_VERIFY));
    assertEquals(Duration.ofMinutes(1440), service.resolveTtl(PSAuthToken.TYPE_PASSWORD_RESET));
    assertEquals(Duration.ofMinutes(60), service.resolveTtl(PSAuthToken.TYPE_ADMIN_BREAK_GLASS));
  }
}
