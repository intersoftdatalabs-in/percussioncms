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

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.data.PSMfaRecoveryCode;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.system.utils.PSBaseBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JPA repository for {@code PSX_ACCOUNT_SECURITY}, {@code PSX_MFA_*}, and {@code PSX_AUTH_*} tables.
 *
 * <p>Uses explicit {@link TransactionTemplate} instances so programmatic lookups from servlets,
 * filters, or adaptors execute inside predictable transaction boundaries even outside AOP proxies.
 */
@PSBaseBean("sys_authSecurityDao")
public class PSAuthSecurityDao implements IPSAuthSecurityDao {

  private static final Logger log = LogManager.getLogger(PSAuthSecurityDao.class);

  private static final Map<String, String> SCHEMA_CHECK_QUERIES = buildSchemaCheckQueries();

  @PersistenceContext private EntityManager entityManager;

  private TransactionTemplate writeTransactionTemplate;
  private TransactionTemplate readTransactionTemplate;
  private TransactionTemplate schemaProbeTransactionTemplate;

  private static Map<String, String> buildSchemaCheckQueries() {
    Map<String, String> queries = new LinkedHashMap<>();
    queries.put("PSX_ACCOUNT_SECURITY", "SELECT COUNT(e) FROM PSAccountSecurity e");
    queries.put("PSX_MFA_TOTP", "SELECT COUNT(e) FROM PSMfaTotp e");
    queries.put("PSX_MFA_PASSKEY", "SELECT COUNT(e) FROM PSMfaPasskey e");
    queries.put("PSX_MFA_RECOVERY_CODE", "SELECT COUNT(e) FROM PSMfaRecoveryCode e");
    queries.put("PSX_AUTH_TOKEN", "SELECT COUNT(e) FROM PSAuthToken e");
    queries.put("PSX_AUTH_LOGIN_TX", "SELECT COUNT(e) FROM PSAuthLoginTx e");
    return Map.copyOf(queries);
  }

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "transactionManager");

    TransactionTemplate write = new TransactionTemplate(transactionManager);
    write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    this.writeTransactionTemplate = write;

    TransactionTemplate read = new TransactionTemplate(transactionManager);
    read.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    read.setReadOnly(true);
    this.readTransactionTemplate = read;

    TransactionTemplate probe = new TransactionTemplate(transactionManager);
    probe.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    probe.setReadOnly(true);
    this.schemaProbeTransactionTemplate = probe;
  }

  /** Package-visible for unit tests. */
  void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Package-visible for unit tests. */
  void setTransactionTemplate(TransactionTemplate transactionTemplate) {
    this.writeTransactionTemplate = transactionTemplate;
    this.readTransactionTemplate = transactionTemplate;
    this.schemaProbeTransactionTemplate = transactionTemplate;
  }

  @Override
  public Optional<PSAccountSecurity> findAccountSecurity(String userName) {
    if (StringUtils.isBlank(userName)) {
      return Optional.empty();
    }
    requireReady();
    return Optional.ofNullable(
        readTransactionTemplate.execute(
            status -> entityManager.find(PSAccountSecurity.class, userName.trim())));
  }

  @Override
  public List<PSAccountSecurity> findAllAccountSecurity() {
    requireReady();
    List<PSAccountSecurity> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT a FROM PSAccountSecurity a ORDER BY a.userName",
                        PSAccountSecurity.class)
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public List<PSAccountSecurity> findByNormalizedEmail(String emailNormalized) {
    if (StringUtils.isBlank(emailNormalized)) {
      return List.of();
    }
    requireReady();
    String norm = emailNormalized.trim().toLowerCase(Locale.ROOT);
    List<PSAccountSecurity> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT a FROM PSAccountSecurity a WHERE a.emailNormalized = :email",
                        PSAccountSecurity.class)
                    .setParameter("email", norm)
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public List<PSAccountSecurity> findVerifiedByNormalizedEmail(String emailNormalized) {
    if (StringUtils.isBlank(emailNormalized)) {
      return List.of();
    }
    requireReady();
    String norm = emailNormalized.trim().toLowerCase(Locale.ROOT);
    List<PSAccountSecurity> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT a FROM PSAccountSecurity a WHERE a.emailNormalized = :email"
                            + " AND a.emailVerified = 'Y' ORDER BY a.userName",
                        PSAccountSecurity.class)
                    .setParameter("email", norm)
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public PSAccountSecurity saveAccountSecurity(PSAccountSecurity entity) {
    Objects.requireNonNull(entity, "entity");
    if (StringUtils.isBlank(entity.getUserName())) {
      throw new IllegalArgumentException("userName is required");
    }
    entity.setUpdatedAt(new Date());
    requireReady();
    return writeTransactionTemplate.execute(
        status -> {
          PSAccountSecurity merged = entityManager.merge(entity);
          entityManager.flush();
          return merged;
        });
  }

  @Override
  public List<PSMfaTotp> findActiveTotpByUser(String userName) {
    if (StringUtils.isBlank(userName)) {
      return List.of();
    }
    requireReady();
    List<PSMfaTotp> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT t FROM PSMfaTotp t WHERE t.userName = :userName"
                            + " AND t.status = :status ORDER BY t.createdAt",
                        PSMfaTotp.class)
                    .setParameter("userName", userName.trim())
                    .setParameter("status", PSMfaTotp.STATUS_ACTIVE)
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public List<PSMfaPasskey> findActivePasskeysByUser(String userName) {
    if (StringUtils.isBlank(userName)) {
      return List.of();
    }
    requireReady();
    List<PSMfaPasskey> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT p FROM PSMfaPasskey p WHERE p.userName = :userName"
                            + " AND p.status = :status ORDER BY p.createdAt",
                        PSMfaPasskey.class)
                    .setParameter("userName", userName.trim())
                    .setParameter("status", PSMfaPasskey.STATUS_ACTIVE)
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public long countUnusedRecoveryCodes(String userName) {
    if (StringUtils.isBlank(userName)) {
      return 0L;
    }
    requireReady();
    Long count =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT COUNT(c) FROM PSMfaRecoveryCode c WHERE c.userName = :userName"
                            + " AND c.usedAt IS NULL",
                        Long.class)
                    .setParameter("userName", userName.trim())
                    .getSingleResult());
    return count != null ? count : 0L;
  }

  @Override
  public Map<String, Integer> countActiveTotpByUsers() {
    return countByUserQuery(
        "SELECT t.userName, COUNT(t) FROM PSMfaTotp t WHERE t.status = :status GROUP BY t.userName",
        PSMfaTotp.STATUS_ACTIVE);
  }

  @Override
  public Map<String, Integer> countActivePasskeysByUsers() {
    return countByUserQuery(
        "SELECT p.userName, COUNT(p) FROM PSMfaPasskey p WHERE p.status = :status"
            + " GROUP BY p.userName",
        PSMfaPasskey.STATUS_ACTIVE);
  }

  @Override
  public Map<String, Integer> countUnusedRecoveryCodesByUsers() {
    requireReady();
    List<Object[]> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT c.userName, COUNT(c) FROM PSMfaRecoveryCode c"
                            + " WHERE c.usedAt IS NULL GROUP BY c.userName",
                        Object[].class)
                    .getResultList());
    return toUserCountMap(rows);
  }

  private Map<String, Integer> countByUserQuery(String jpql, String statusParam) {
    requireReady();
    List<Object[]> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(jpql, Object[].class)
                    .setParameter("status", statusParam)
                    .getResultList());
    return toUserCountMap(rows);
  }

  private static Map<String, Integer> toUserCountMap(List<Object[]> rows) {
    if (rows == null || rows.isEmpty()) {
      return Map.of();
    }
    Map<String, Integer> result = new LinkedHashMap<>();
    for (Object[] row : rows) {
      if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
        result.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
      }
    }
    return Map.copyOf(result);
  }

  @Override
  public PSAuthToken saveToken(PSAuthToken token) {
    Objects.requireNonNull(token, "token");
    if (StringUtils.isBlank(token.getTokenId())) {
      throw new IllegalArgumentException("tokenId is required");
    }
    requireReady();
    return writeTransactionTemplate.execute(
        status -> {
          PSAuthToken merged = entityManager.merge(token);
          entityManager.flush();
          return merged;
        });
  }

  @Override
  public Optional<PSAuthToken> findTokenByHash(String tokenHash) {
    if (StringUtils.isBlank(tokenHash)) {
      return Optional.empty();
    }
    requireReady();
    List<PSAuthToken> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT t FROM PSAuthToken t WHERE t.tokenHash = :tokenHash",
                        PSAuthToken.class)
                    .setParameter("tokenHash", tokenHash.trim())
                    .setMaxResults(1)
                    .getResultList());
    if (rows == null || rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.get(0));
  }

  @Override
  public int consumeAuthTokenAtomically(String tokenHash, String tokenType, Instant consumedAt) {
    if (StringUtils.isBlank(tokenHash) || StringUtils.isBlank(tokenType) || consumedAt == null) {
      return 0;
    }
    requireReady();
    Date nowDate = Date.from(consumedAt);
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSAuthToken t SET t.consumedAt = :consumedAt"
                            + " WHERE t.tokenHash = :tokenHash AND t.tokenType = :tokenType"
                            + " AND t.consumedAt IS NULL AND t.expiresAt > :consumedAt")
                    .setParameter("consumedAt", nowDate)
                    .setParameter("tokenHash", tokenHash.trim())
                    .setParameter("tokenType", tokenType.trim())
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int invalidateUnconsumedTokens(String userName, String tokenType, Instant consumedAt) {
    if (StringUtils.isBlank(userName) || StringUtils.isBlank(tokenType) || consumedAt == null) {
      return 0;
    }
    requireReady();
    Date nowDate = Date.from(consumedAt);
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSAuthToken t SET t.consumedAt = :consumedAt"
                            + " WHERE t.userName = :userName AND t.tokenType = :tokenType"
                            + " AND t.consumedAt IS NULL")
                    .setParameter("consumedAt", nowDate)
                    .setParameter("userName", userName.trim())
                    .setParameter("tokenType", tokenType.trim())
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int purgeExpiredTokens(Instant cutoff) {
    Objects.requireNonNull(cutoff, "cutoff");
    requireReady();
    Integer deleted =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery("DELETE FROM PSAuthToken t WHERE t.expiresAt < :cutoff")
                    .setParameter("cutoff", Date.from(cutoff))
                    .executeUpdate());
    return deleted != null ? deleted : 0;
  }

  @Override
  public PSAuthLoginTx saveLoginTx(PSAuthLoginTx tx) {
    Objects.requireNonNull(tx, "tx");
    if (StringUtils.isBlank(tx.getTxId())) {
      throw new IllegalArgumentException("txId is required");
    }
    requireReady();
    return writeTransactionTemplate.execute(
        status -> {
          PSAuthLoginTx merged = entityManager.merge(tx);
          entityManager.flush();
          return merged;
        });
  }

  @Override
  public Optional<PSAuthLoginTx> findLoginTx(String txId) {
    if (StringUtils.isBlank(txId)) {
      return Optional.empty();
    }
    requireReady();
    return Optional.ofNullable(
        readTransactionTemplate.execute(
            status -> entityManager.find(PSAuthLoginTx.class, txId.trim())));
  }

  @Override
  public int consumeLoginTxAtomically(String txId, String expectedType, Date now) {
    if (StringUtils.isBlank(txId) || StringUtils.isBlank(expectedType) || now == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSAuthLoginTx t SET t.state = :verifiedState"
                            + " WHERE t.txId = :txId AND t.txType = :txType"
                            + " AND t.state = :pendingState AND t.expiresAt > :now")
                    .setParameter("verifiedState", PSAuthLoginTx.STATE_VERIFIED)
                    .setParameter("txId", txId.trim())
                    .setParameter("txType", expectedType.trim())
                    .setParameter("pendingState", PSAuthLoginTx.STATE_PENDING)
                    .setParameter("now", now)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int incrementLoginTxAttemptsAtomically(String txId, int maxAttempts, Date now) {
    if (StringUtils.isBlank(txId) || maxAttempts <= 0 || now == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSAuthLoginTx t SET t.attemptCount = t.attemptCount + 1"
                            + " WHERE t.txId = :txId AND t.state = :pendingState"
                            + " AND t.expiresAt > :now AND t.attemptCount < :maxAttempts")
                    .setParameter("txId", txId.trim())
                    .setParameter("pendingState", PSAuthLoginTx.STATE_PENDING)
                    .setParameter("now", now)
                    .setParameter("maxAttempts", maxAttempts)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int cancelLoginTx(String txId) {
    if (StringUtils.isBlank(txId)) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSAuthLoginTx t SET t.state = :cancelledState"
                            + " WHERE t.txId = :txId AND t.state = :pendingState")
                    .setParameter("cancelledState", PSAuthLoginTx.STATE_CANCELLED)
                    .setParameter("txId", txId.trim())
                    .setParameter("pendingState", PSAuthLoginTx.STATE_PENDING)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int purgeExpiredLoginTx(Instant cutoff) {
    Objects.requireNonNull(cutoff, "cutoff");
    requireReady();
    Integer deleted =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery("DELETE FROM PSAuthLoginTx t WHERE t.expiresAt < :cutoff")
                    .setParameter("cutoff", Date.from(cutoff))
                    .executeUpdate());
    return deleted != null ? deleted : 0;
  }

  @Override
  public PSMfaTotp saveTotp(PSMfaTotp totp) {
    Objects.requireNonNull(totp, "totp");
    if (StringUtils.isBlank(totp.getTotpId()) || StringUtils.isBlank(totp.getUserName())) {
      throw new IllegalArgumentException("totpId and userName are required");
    }
    requireReady();
    return writeTransactionTemplate.execute(
        status -> {
          PSMfaTotp merged = entityManager.merge(totp);
          entityManager.flush();
          return merged;
        });
  }

  @Override
  public Optional<PSMfaTotp> findTotpById(String totpId) {
    if (StringUtils.isBlank(totpId)) {
      return Optional.empty();
    }
    requireReady();
    return Optional.ofNullable(
        readTransactionTemplate.execute(
            status -> entityManager.find(PSMfaTotp.class, totpId.trim())));
  }

  @Override
  public List<PSMfaTotp> findTotpByUserAndStatus(String userName, String status) {
    if (StringUtils.isBlank(userName) || StringUtils.isBlank(status)) {
      return List.of();
    }
    requireReady();
    List<PSMfaTotp> rows =
        readTransactionTemplate.execute(
            tx ->
                entityManager
                    .createQuery(
                        "SELECT t FROM PSMfaTotp t WHERE t.userName = :userName"
                            + " AND t.status = :status ORDER BY t.createdAt",
                        PSMfaTotp.class)
                    .setParameter("userName", userName.trim())
                    .setParameter("status", status.trim())
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public int advanceTotpLastUsedStepAtomically(String totpId, long newStep, Date usedAt) {
    if (StringUtils.isBlank(totpId) || newStep < 0 || usedAt == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaTotp t SET t.lastUsedStep = :newStep, t.lastUsedAt = :usedAt"
                            + " WHERE t.totpId = :totpId AND t.status = :activeStatus"
                            + " AND (t.lastUsedStep IS NULL OR t.lastUsedStep < :newStep)")
                    .setParameter("newStep", newStep)
                    .setParameter("usedAt", usedAt)
                    .setParameter("totpId", totpId.trim())
                    .setParameter("activeStatus", PSMfaTotp.STATUS_ACTIVE)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int revokeTotpByUser(String userName, String currentStatus) {
    if (StringUtils.isBlank(userName) || StringUtils.isBlank(currentStatus)) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaTotp t SET t.status = :revokedStatus"
                            + " WHERE t.userName = :userName AND t.status = :currentStatus")
                    .setParameter("revokedStatus", PSMfaTotp.STATUS_REVOKED)
                    .setParameter("userName", userName.trim())
                    .setParameter("currentStatus", currentStatus.trim())
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public void saveRecoveryCodes(List<PSMfaRecoveryCode> codes) {
    if (codes == null || codes.isEmpty()) {
      return;
    }
    requireReady();
    writeTransactionTemplate.executeWithoutResult(
        status -> {
          for (PSMfaRecoveryCode code : codes) {
            entityManager.merge(code);
          }
          entityManager.flush();
        });
  }

  @Override
  public List<PSMfaRecoveryCode> findUnusedRecoveryCodesByUser(String userName) {
    if (StringUtils.isBlank(userName)) {
      return List.of();
    }
    requireReady();
    List<PSMfaRecoveryCode> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT c FROM PSMfaRecoveryCode c WHERE c.userName = :userName"
                            + " AND c.usedAt IS NULL ORDER BY c.createdAt",
                        PSMfaRecoveryCode.class)
                    .setParameter("userName", userName.trim())
                    .getResultList());
    return rows != null ? rows : List.of();
  }

  @Override
  public int consumeRecoveryCodeAtomically(String codeId, Date usedAt) {
    if (StringUtils.isBlank(codeId) || usedAt == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaRecoveryCode c SET c.usedAt = :usedAt"
                            + " WHERE c.codeId = :codeId AND c.usedAt IS NULL")
                    .setParameter("usedAt", usedAt)
                    .setParameter("codeId", codeId.trim())
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int invalidateUnusedRecoveryCodesByUser(String userName, Date usedAt) {
    if (StringUtils.isBlank(userName) || usedAt == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaRecoveryCode c SET c.usedAt = :usedAt"
                            + " WHERE c.userName = :userName AND c.usedAt IS NULL")
                    .setParameter("usedAt", usedAt)
                    .setParameter("userName", userName.trim())
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public long findSessionVersion(String userName) {
    if (StringUtils.isBlank(userName)) {
      return 1L;
    }
    requireReady();
    PSAccountSecurity sec =
        readTransactionTemplate.execute(
            status -> entityManager.find(PSAccountSecurity.class, userName.trim()));
    return sec != null && sec.getSessionVersion() > 0 ? sec.getSessionVersion() : 1L;
  }

  @Override
  public long incrementSessionVersion(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    requireReady();
    String cleanUser = userName.trim();
    Long result =
        writeTransactionTemplate.execute(
            status -> {
              PSAccountSecurity sec = entityManager.find(PSAccountSecurity.class, cleanUser);
              if (sec == null) {
                sec = new PSAccountSecurity();
                sec.setUserName(cleanUser);
                sec.setSessionVersion(2L);
              } else {
                sec.setSessionVersion(Math.max(1L, sec.getSessionVersion()) + 1L);
              }
              sec.setUpdatedAt(new Date());
              PSAccountSecurity merged = entityManager.merge(sec);
              entityManager.flush();
              return merged.getSessionVersion();
            });
    return result != null ? result : 2L;
  }

  @Override
  public PSMfaPasskey savePasskey(PSMfaPasskey passkey) {
    Objects.requireNonNull(passkey, "passkey");
    if (StringUtils.isBlank(passkey.getPasskeyId())) {
      throw new IllegalArgumentException("passkeyId is required");
    }
    requireReady();
    return writeTransactionTemplate.execute(
        status -> {
          PSMfaPasskey merged = entityManager.merge(passkey);
          entityManager.flush();
          return merged;
        });
  }

  @Override
  public Optional<PSMfaPasskey> findPasskeyById(String passkeyId) {
    if (StringUtils.isBlank(passkeyId)) {
      return Optional.empty();
    }
    requireReady();
    return Optional.ofNullable(
        readTransactionTemplate.execute(
            status -> entityManager.find(PSMfaPasskey.class, passkeyId.trim())));
  }

  @Override
  public Optional<PSMfaPasskey> findActivePasskeyByCredentialId(String credentialId) {
    if (StringUtils.isBlank(credentialId)) {
      return Optional.empty();
    }
    requireReady();
    List<PSMfaPasskey> rows =
        readTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "SELECT p FROM PSMfaPasskey p WHERE p.credentialId = :credentialId"
                            + " AND p.status = :status",
                        PSMfaPasskey.class)
                    .setParameter("credentialId", credentialId.trim())
                    .setParameter("status", PSMfaPasskey.STATUS_ACTIVE)
                    .setMaxResults(1)
                    .getResultList());
    if (rows == null || rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.get(0));
  }

  @Override
  public int renamePasskey(String passkeyId, String userName, String newLabel) {
    if (StringUtils.isBlank(passkeyId)
        || StringUtils.isBlank(userName)
        || StringUtils.isBlank(newLabel)) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaPasskey p SET p.label = :label"
                            + " WHERE p.passkeyId = :passkeyId AND p.userName = :userName"
                            + " AND p.status = :status")
                    .setParameter("label", newLabel.trim())
                    .setParameter("passkeyId", passkeyId.trim())
                    .setParameter("userName", userName.trim())
                    .setParameter("status", PSMfaPasskey.STATUS_ACTIVE)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int revokePasskey(String passkeyId, String userName) {
    if (StringUtils.isBlank(passkeyId) || StringUtils.isBlank(userName)) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaPasskey p SET p.status = :revokedStatus"
                            + " WHERE p.passkeyId = :passkeyId AND p.userName = :userName"
                            + " AND p.status = :activeStatus")
                    .setParameter("revokedStatus", PSMfaPasskey.STATUS_REVOKED)
                    .setParameter("passkeyId", passkeyId.trim())
                    .setParameter("userName", userName.trim())
                    .setParameter("activeStatus", PSMfaPasskey.STATUS_ACTIVE)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public int updatePasskeySignCountAtomically(
      String passkeyId,
      long expectedSignCount,
      long newSignCount,
      boolean backupEligible,
      boolean backupState,
      Date lastUsedAt) {
    if (StringUtils.isBlank(passkeyId)
        || expectedSignCount < 0
        || newSignCount < 0
        || lastUsedAt == null) {
      return 0;
    }
    requireReady();
    Integer updated =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .createQuery(
                        "UPDATE PSMfaPasskey p SET p.signCount = :newSignCount,"
                            + " p.backupEligible = :be, p.backupState = :bs, p.lastUsedAt = :lastUsedAt"
                            + " WHERE p.passkeyId = :passkeyId AND p.status = :status"
                            + " AND p.signCount = :expectedSignCount")
                    .setParameter("newSignCount", newSignCount)
                    .setParameter("be", backupEligible ? "Y" : "N")
                    .setParameter("bs", backupState ? "Y" : "N")
                    .setParameter("lastUsedAt", lastUsedAt)
                    .setParameter("passkeyId", passkeyId.trim())
                    .setParameter("status", PSMfaPasskey.STATUS_ACTIVE)
                    .setParameter("expectedSignCount", expectedSignCount)
                    .executeUpdate());
    return updated != null ? updated : 0;
  }

  @Override
  public Map<String, Boolean> checkSchemaTablesPresent() {
    requireReady();
    Map<String, Boolean> statusMap = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : SCHEMA_CHECK_QUERIES.entrySet()) {
      String table = entry.getKey();
      String jpql = entry.getValue();
      try {
        schemaProbeTransactionTemplate.execute(
            status -> {
              TypedQuery<Long> q = entityManager.createQuery(jpql, Long.class);
              return q.getSingleResult();
            });
        statusMap.put(table, Boolean.TRUE);
      } catch (RuntimeException ex) {
        log.warn("Schema check failed for {}: {}", table, ex.toString());
        statusMap.put(table, Boolean.FALSE);
      }
    }
    return Map.copyOf(statusMap);
  }

  private void requireReady() {
    Objects.requireNonNull(entityManager, "entityManager");
    Objects.requireNonNull(writeTransactionTemplate, "writeTransactionTemplate");
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    Objects.requireNonNull(schemaProbeTransactionTemplate, "schemaProbeTransactionTemplate");
  }
}
