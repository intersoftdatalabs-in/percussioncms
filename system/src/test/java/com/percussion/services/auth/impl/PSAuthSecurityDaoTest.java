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
package com.percussion.services.auth.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.data.PSMfaRecoveryCode;
import com.percussion.services.auth.data.PSMfaTotp;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PSAuthSecurityDaoTest {

  private static PSAuthSecurityDao createDao(EntityManager em) {
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    when(tm.getTransaction(any(TransactionDefinition.class)))
        .thenAnswer(inv -> new SimpleTransactionStatus());
    PSAuthSecurityDao dao = new PSAuthSecurityDao();
    dao.setEntityManager(em);
    dao.setTransactionManager(tm);
    return dao;
  }

  @Test
  void saveAccountSecurityMergesFlushesAndSetsUpdatedAt() {
    EntityManager em = mock(EntityManager.class);
    PSAuthSecurityDao dao = createDao(em);

    PSAccountSecurity entity = new PSAccountSecurity();
    entity.setUserName("alice");
    entity.setEmailNormalized("alice@example.com");
    entity.setEmailVerified(true);
    when(em.merge(entity)).thenReturn(entity);

    PSAccountSecurity saved = dao.saveAccountSecurity(entity);

    assertNotNull(saved.getUpdatedAt());
    assertTrue(saved.isEmailVerified());
    assertEquals(PSAccountSecurity.POLICY_INTERACTIVE_MFA_REQUIRED, saved.getAccountPolicy());
    assertEquals(1L, saved.getSessionVersion());
    verify(em).merge(entity);
    verify(em).flush();
  }

  @Test
  void findByNormalizedEmailUsesRootLowercaseAndReturnsMatches() {
    EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    TypedQuery<PSAccountSecurity> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(PSAccountSecurity.class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);

    PSAccountSecurity alice = new PSAccountSecurity();
    alice.setUserName("alice");
    alice.setEmailNormalized("alice@example.com");
    when(query.getResultList()).thenReturn(List.of(alice));

    PSAuthSecurityDao dao = createDao(em);
    List<PSAccountSecurity> matches = dao.findByNormalizedEmail("  Alice@EXAMPLE.COM ");

    assertEquals(1, matches.size());
    assertEquals("alice", matches.get(0).getUserName());
    verify(query).setParameter("email", "alice@example.com");
  }

  @Test
  void groupedFactorCountsMapUsersToCounts() {
    EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    TypedQuery<Object[]> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.getResultList())
        .thenReturn(List.of(new Object[] {"alice", 2L}, new Object[] {"bob", 1L}));

    PSAuthSecurityDao dao = createDao(em);
    Map<String, Integer> totpCounts = dao.countActiveTotpByUsers();
    Map<String, Integer> passkeyCounts = dao.countActivePasskeysByUsers();
    Map<String, Integer> recoveryCounts = dao.countUnusedRecoveryCodesByUsers();

    assertEquals(2, totpCounts.get("alice"));
    assertEquals(1, totpCounts.get("bob"));
    assertEquals(2, passkeyCounts.get("alice"));
    assertEquals(1, recoveryCounts.get("bob"));
  }

  @Test
  void tokenAndLoginTxOperationsAndPurgeWork() {
    EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    TypedQuery<PSAuthToken> tokenQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(PSAuthToken.class))).thenReturn(tokenQuery);
    when(tokenQuery.setParameter(anyString(), any())).thenReturn(tokenQuery);
    when(tokenQuery.setMaxResults(1)).thenReturn(tokenQuery);

    PSAuthToken token = new PSAuthToken();
    token.setTokenId("t1");
    token.setTokenHash("hash123");
    token.setTokenType(PSAuthToken.TYPE_EMAIL_VERIFY);
    token.setUserName("alice");
    token.setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
    when(em.merge(token)).thenReturn(token);
    when(tokenQuery.getResultList()).thenReturn(List.of(token));

    Query deleteQuery = mock(Query.class);
    when(em.createQuery(anyString())).thenReturn(deleteQuery);
    when(deleteQuery.setParameter(anyString(), any())).thenReturn(deleteQuery);
    when(deleteQuery.executeUpdate()).thenReturn(3);

    PSAuthLoginTx tx = new PSAuthLoginTx();
    tx.setTxId("tx1");
    tx.setTxType(PSAuthLoginTx.TYPE_MFA_CHALLENGE);
    tx.setUserName("alice");
    tx.setChallengeB64("secret-challenge");
    tx.setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
    when(em.merge(tx)).thenReturn(tx);
    when(em.find(PSAuthLoginTx.class, "tx1")).thenReturn(tx);

    PSAuthSecurityDao dao = createDao(em);
    dao.saveToken(token);
    Optional<PSAuthToken> found = dao.findTokenByHash("hash123");
    assertTrue(found.isPresent());
    assertEquals("t1", found.get().getTokenId());
    assertEquals(3, dao.purgeExpiredTokens(Instant.now()));

    dao.saveLoginTx(tx);
    assertEquals("tx1", dao.findLoginTx("tx1").orElseThrow().getTxId());
    assertEquals(3, dao.purgeExpiredLoginTx(Instant.now()));
  }

  @Test
  void checkSchemaTablesPresentReportsIndividualFailuresWithoutAbortingOthers() {
    EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    TypedQuery<Long> okQuery = mock(TypedQuery.class);
    when(okQuery.getSingleResult()).thenReturn(0L);
    @SuppressWarnings("unchecked")
    TypedQuery<Long> failQuery = mock(TypedQuery.class);
    when(failQuery.getSingleResult()).thenThrow(new IllegalStateException("missing table"));

    when(em.createQuery(anyString(), eq(Long.class)))
        .thenAnswer(
            inv -> {
              String jpql = inv.getArgument(0, String.class);
              return jpql.contains("PSMfaPasskey") ? failQuery : okQuery;
            });

    PSAuthSecurityDao dao = createDao(em);
    Map<String, Boolean> status = dao.checkSchemaTablesPresent();

    assertEquals(6, status.size());
    assertTrue(status.get("PSX_ACCOUNT_SECURITY"));
    assertTrue(status.get("PSX_MFA_TOTP"));
    assertFalse(status.get("PSX_MFA_PASSKEY"));
    assertTrue(status.get("PSX_MFA_RECOVERY_CODE"));
    assertTrue(status.get("PSX_AUTH_TOKEN"));
    assertTrue(status.get("PSX_AUTH_LOGIN_TX"));
  }

  @Test
  void sensitiveEntitiesRedactSecretsInToString() {
    PSMfaTotp totp = new PSMfaTotp();
    totp.setTotpId("id1");
    totp.setUserName("alice");
    totp.setSecretEnc("SUPER_SECRET_ENC_KEY");
    assertFalse(totp.toString().contains("SUPER_SECRET_ENC_KEY"));

    PSMfaRecoveryCode rc = new PSMfaRecoveryCode();
    rc.setCodeId("c1");
    rc.setCodeHash("SUPER_SECRET_HASH");
    assertFalse(rc.toString().contains("SUPER_SECRET_HASH"));

    PSAuthToken tok = new PSAuthToken();
    tok.setTokenId("tok1");
    tok.setTokenHash("RAW_TOKEN_HASH");
    assertFalse(tok.toString().contains("RAW_TOKEN_HASH"));

    PSAuthLoginTx tx = new PSAuthLoginTx();
    tx.setTxId("tx1");
    tx.setChallengeB64("SECRET_CHALLENGE_B64");
    assertFalse(tx.toString().contains("SECRET_CHALLENGE_B64"));

    PSMfaPasskey pk = new PSMfaPasskey();
    pk.setBackupEligible(true);
    pk.setBackupState(false);
    assertTrue(pk.isBackupEligible());
    assertFalse(pk.isBackupState());
  }

  @Test
  void findVerifiedByNormalizedEmailFiltersVerifiedRowsAndOrdersByUser() {
    EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    TypedQuery<PSAccountSecurity> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(PSAccountSecurity.class))).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);

    PSAccountSecurity alice = new PSAccountSecurity();
    alice.setUserName("alice");
    alice.setEmailNormalized("shared@example.com");
    alice.setEmailVerified(true);
    when(query.getResultList()).thenReturn(List.of(alice));

    PSAuthSecurityDao dao = createDao(em);
    assertTrue(dao.findVerifiedByNormalizedEmail("  ").isEmpty());
    List<PSAccountSecurity> matches = dao.findVerifiedByNormalizedEmail(" Shared@EXAMPLE.COM ");

    assertEquals(1, matches.size());
    assertEquals("alice", matches.get(0).getUserName());
    verify(query).setParameter("email", "shared@example.com");
  }

  @Test
  void consumeAuthTokenAtomicallyAndInvalidateUnconsumedExecuteUpdates() {
    EntityManager em = mock(EntityManager.class);
    Query updateQuery = mock(Query.class);
    when(em.createQuery(anyString())).thenReturn(updateQuery);
    when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
    when(updateQuery.executeUpdate()).thenReturn(1).thenReturn(2);

    PSAuthSecurityDao dao = createDao(em);
    Instant now = Instant.parse("2026-11-18T12:00:00Z");

    assertEquals(0, dao.consumeAuthTokenAtomically(" ", PSAuthToken.TYPE_EMAIL_VERIFY, now));
    assertEquals(1, dao.consumeAuthTokenAtomically("digest123", PSAuthToken.TYPE_EMAIL_VERIFY, now));
    assertEquals(2, dao.invalidateUnconsumedTokens("alice", PSAuthToken.TYPE_EMAIL_VERIFY, now));

    verify(updateQuery).setParameter("tokenHash", "digest123");
    verify(updateQuery, org.mockito.Mockito.times(2))
        .setParameter("tokenType", PSAuthToken.TYPE_EMAIL_VERIFY);
    verify(updateQuery).setParameter("userName", "alice");
  }

  @Test
  void totpRecoveryCodeLoginTxAndSessionVersionOperationsWork() {
    EntityManager em = mock(EntityManager.class);
    Query updateQuery = mock(Query.class);
    when(em.createQuery(anyString())).thenReturn(updateQuery);
    when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
    when(updateQuery.executeUpdate()).thenReturn(1);

    PSAccountSecurity existing = new PSAccountSecurity();
    existing.setUserName("alice");
    existing.setSessionVersion(3L);
    when(em.find(PSAccountSecurity.class, "alice")).thenReturn(existing);
    when(em.merge(any(PSAccountSecurity.class))).thenAnswer(inv -> inv.getArgument(0));

    PSAuthSecurityDao dao = createDao(em);
    Date now = new Date();

    assertEquals(1, dao.advanceTotpLastUsedStepAtomically("totp-1", 42L, now));
    assertEquals(1, dao.revokeTotpByUser("alice", PSMfaTotp.STATUS_PENDING));
    assertEquals(1, dao.consumeRecoveryCodeAtomically("rc-1", now));
    assertEquals(1, dao.invalidateUnusedRecoveryCodesByUser("alice", now));
    assertEquals(1, dao.consumeLoginTxAtomically("tx-1", PSAuthLoginTx.TYPE_MFA_CHALLENGE, now));
    assertEquals(1, dao.incrementLoginTxAttemptsAtomically("tx-1", 5, now));
    assertEquals(1, dao.cancelLoginTx("tx-1"));

    assertEquals(3L, dao.findSessionVersion("alice"));
    assertEquals(1L, dao.findSessionVersion("unknown"));
    assertEquals(4L, dao.incrementSessionVersion("alice"));
    assertEquals(2L, dao.incrementSessionVersion("bob"));
  }

  @Test
  void passkeyPersistenceLookupRenameRevokeAndSignCountUpdateWork() {
    EntityManager em = mock(EntityManager.class);
    Query updateQuery = mock(Query.class);
    @SuppressWarnings("unchecked")
    TypedQuery<PSMfaPasskey> typedQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString())).thenReturn(updateQuery);
    when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
    when(updateQuery.executeUpdate()).thenReturn(1);
    when(em.createQuery(anyString(), eq(PSMfaPasskey.class))).thenReturn(typedQuery);
    when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
    when(typedQuery.setMaxResults(1)).thenReturn(typedQuery);
    when(em.merge(any(PSMfaPasskey.class))).thenAnswer(inv -> inv.getArgument(0));

    PSMfaPasskey pk = new PSMfaPasskey();
    pk.setPasskeyId("pk-1");
    pk.setUserName("alice");
    pk.setCredentialId("cred-1");
    pk.setUserHandle("uh-1");
    pk.setCosePublicKeyB64("pQECAyYgASFYIG==");
    pk.setLabel("MacBook");
    when(em.find(PSMfaPasskey.class, "pk-1")).thenReturn(pk);
    when(typedQuery.getResultList()).thenReturn(List.of(pk));

    PSAuthSecurityDao dao = createDao(em);
    assertEquals("pk-1", dao.savePasskey(pk).getPasskeyId());
    assertTrue(dao.findPasskeyById("pk-1").isPresent());
    assertTrue(dao.findActivePasskeyByCredentialId("cred-1").isPresent());
    assertEquals(1, dao.renamePasskey("pk-1", "alice", "YubiKey"));
    assertEquals(
        1, dao.updatePasskeySignCountAtomically("pk-1", 0L, 1L, true, true, new Date()));
    assertEquals(1, dao.revokePasskey("pk-1", "alice"));
    assertEquals(0, dao.renamePasskey(" ", "alice", "New"));
  }
}
