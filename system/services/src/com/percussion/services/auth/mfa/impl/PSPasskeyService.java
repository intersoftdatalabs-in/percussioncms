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
package com.percussion.services.auth.mfa.impl;

import com.percussion.server.PSServer;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.mfa.IPSPasskeyService;
import com.percussion.services.auth.mfa.PSPasskeyOptions;
import com.percussion.services.auth.mfa.PSPasskeyRegisterOutcome;
import com.percussion.services.auth.mfa.PSPasskeySummary;
import com.percussion.services.auth.mfa.impl.PSWebAuthnConfigResolver.PSWebAuthnRpConfig;
import com.percussion.system.utils.PSBaseBean;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import com.webauthn4j.verifier.exception.VerificationException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** WebAuthn / FIDO2 Passkey ceremony and credential service backed by WebAuthn4J 0.32. */
@PSBaseBean("sys_passkeyService")
public class PSPasskeyService implements IPSPasskeyService {

  private static final int CHALLENGE_BYTES = 32;
  private static final int USER_HANDLE_BYTES = 32;
  private static final long CEREMONY_TIMEOUT_MS = 120_000L;
  private static final Duration CEREMONY_TTL = Duration.ofMinutes(5);
  private static final int MAX_LABEL_LEN = 128;

  private static final List<PublicKeyCredentialParameters> SUPPORTED_PUB_KEY_PARAMS =
      List.of(
          new PublicKeyCredentialParameters(
              PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
          new PublicKeyCredentialParameters(
              PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256),
          new PublicKeyCredentialParameters(
              PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.EdDSA));

  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final IPSAuthSecurityDao authSecurityDao;
  private final Supplier<Properties> serverPropsSupplier;
  private final PSWebAuthnConfigResolver configResolver;
  private final ObjectConverter objectConverter;
  private final WebAuthnManager webAuthnManager;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public PSPasskeyService(IPSAuthSecurityDao authSecurityDao) {
    this(
        authSecurityDao,
        PSPasskeyService::defaultServerProperties,
        new PSWebAuthnConfigResolver(),
        new ObjectConverter(),
        Clock.systemUTC());
  }

  PSPasskeyService(
      IPSAuthSecurityDao authSecurityDao,
      Supplier<Properties> serverPropsSupplier,
      PSWebAuthnConfigResolver configResolver,
      ObjectConverter objectConverter,
      Clock clock) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.configResolver = Objects.requireNonNull(configResolver, "configResolver");
    this.objectConverter = Objects.requireNonNull(objectConverter, "objectConverter");
    this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(this.objectConverter);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public PSPasskeyOptions startRegistration(
      String userName, String displayName, String label, String requestOrigin) {
    String cleanUser = requireUserName(userName);
    PSWebAuthnRpConfig cfg = configResolver.resolve(serverPropsSupplier.get(), requestOrigin);
    List<PSMfaPasskey> existing = authSecurityDao.findActivePasskeysByUser(cleanUser);
    String userHandle = resolveOrCreateUserHandle(existing);
    String challengeB64Url = generateChallenge();
    String cleanLabel = normalizeLabel(label);

    String txId =
        saveCeremonyTx(
            PSAuthLoginTx.TYPE_WEBAUTHN_REG,
            cleanUser,
            challengeB64Url,
            cfg,
            userHandle,
            cleanLabel);

    String optionsJson =
        buildCreationOptionsJson(cfg, cleanUser, displayName, userHandle, challengeB64Url, existing);
    return new PSPasskeyOptions(txId, optionsJson);
  }

  @Override
  public PSPasskeyRegisterOutcome finishRegistration(
      String userName,
      String challengeTxId,
      String label,
      String registrationResponseJson,
      String requestOrigin) {
    String cleanUser = requireUserName(userName);
    if (StringUtils.isBlank(registrationResponseJson)) {
      throw new IllegalArgumentException("Passkey registration response is required.");
    }

    PSAuthLoginTx tx = consumeCeremonyTx(challengeTxId, PSAuthLoginTx.TYPE_WEBAUTHN_REG, cleanUser);
    Map<String, Object> payload = parsePayload(tx.getPayloadJson());
    PSWebAuthnRpConfig cfg = configResolver.resolve(serverPropsSupplier.get(), requestOrigin);
    ServerProperty serverProperty = buildServerProperty(cfg, payload, tx.getChallengeB64());

    RegistrationParameters params =
        new RegistrationParameters(
            serverProperty, SUPPORTED_PUB_KEY_PARAMS, cfg.registrationUvRequired(), true);
    RegistrationData data = parseAndVerifyRegistration(registrationResponseJson, params);

    AttestedCredentialData attested = data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
    String credentialIdB64Url = Base64UrlUtil.encodeToString(attested.getCredentialId());
    if (authSecurityDao.findActivePasskeyByCredentialId(credentialIdB64Url).isPresent()) {
      throw new IllegalArgumentException("This passkey is already registered.");
    }

    String cleanLabel =
        StringUtils.isNotBlank(label)
            ? normalizeLabel(label)
            : normalizeLabel(asString(payload.get("label")));
    String userHandle = StringUtils.defaultIfBlank(asString(payload.get("userHandle")), generateChallenge());

    PSMfaPasskey entity = buildPasskeyEntity(cleanUser, cleanLabel, userHandle, credentialIdB64Url, data);
    authSecurityDao.savePasskey(entity);
    return new PSPasskeyRegisterOutcome(
        true, toSummary(entity), List.of(), "Passkey registered.");
  }

  @Override
  public PSPasskeyOptions startAuthentication(String userName, String requestOrigin) {
    String cleanUser = requireUserName(userName);
    List<PSMfaPasskey> active = authSecurityDao.findActivePasskeysByUser(cleanUser);
    if (active.isEmpty()) {
      throw new IllegalStateException("No active passkeys are registered for this account.");
    }
    PSWebAuthnRpConfig cfg = configResolver.resolve(serverPropsSupplier.get(), requestOrigin);
    String challengeB64Url = generateChallenge();

    String txId =
        saveCeremonyTx(PSAuthLoginTx.TYPE_WEBAUTHN_AUTH, cleanUser, challengeB64Url, cfg, "", "");
    String optionsJson = buildRequestOptionsJson(cfg, challengeB64Url, active);
    return new PSPasskeyOptions(txId, optionsJson);
  }

  @Override
  public PSMfaPasskey verifyAuthentication(
      String expectedUserName,
      String challengeTxId,
      String authenticationResponseJson,
      String requestOrigin) {
    String cleanUser = requireUserName(expectedUserName);
    if (StringUtils.isBlank(authenticationResponseJson)) {
      throw new IllegalArgumentException("Passkey assertion response is required.");
    }

    PSAuthLoginTx tx = consumeCeremonyTx(challengeTxId, PSAuthLoginTx.TYPE_WEBAUTHN_AUTH, cleanUser);
    Map<String, Object> payload = parsePayload(tx.getPayloadJson());
    PSWebAuthnRpConfig cfg = configResolver.resolve(serverPropsSupplier.get(), requestOrigin);
    ServerProperty serverProperty = buildServerProperty(cfg, payload, tx.getChallengeB64());

    AuthenticationData parsed = parseAuthenticationData(authenticationResponseJson);
    String credentialIdB64Url = Base64UrlUtil.encodeToString(parsed.getCredentialId());
    PSMfaPasskey stored =
        authSecurityDao
            .findActivePasskeyByCredentialId(credentialIdB64Url)
            .filter(p -> cleanUser.equalsIgnoreCase(p.getUserName()))
            .orElseThrow(() -> new IllegalArgumentException("Unrecognized passkey credential."));

    CredentialRecord credentialRecord = toCredentialRecord(stored);
    List<byte[]> allowCredentials = List.of(Base64UrlUtil.decode(stored.getCredentialId()));
    AuthenticationParameters authParams =
        new AuthenticationParameters(serverProperty, credentialRecord, allowCredentials, true, true);

    AuthenticationData verified = verifyAuthenticationData(authenticationResponseJson, authParams);
    long previousCount = stored.getSignCount();
    long newCount = verified.getAuthenticatorData().getSignCount();
    if (previousCount > 0L && newCount <= previousCount) {
      throw new IllegalArgumentException("Passkey authenticator counter regression detected.");
    }

    boolean be = verified.getAuthenticatorData().isFlagBE();
    boolean bs = verified.getAuthenticatorData().isFlagBS();
    Date usedAt = Date.from(clock.instant());
    int updated =
        authSecurityDao.updatePasskeySignCountAtomically(
            stored.getPasskeyId(), previousCount, newCount, be, bs, usedAt);
    if (updated != 1) {
      throw new IllegalArgumentException("Passkey verification conflicted with a concurrent update.");
    }
    stored.setSignCount(newCount);
    stored.setBackupEligible(be);
    stored.setBackupState(bs);
    stored.setLastUsedAt(usedAt);
    return stored;
  }

  @Override
  public List<PSPasskeySummary> listPasskeys(String userName) {
    if (StringUtils.isBlank(userName)) {
      return List.of();
    }
    return authSecurityDao.findActivePasskeysByUser(userName.trim()).stream()
        .map(PSPasskeyService::toSummary)
        .toList();
  }

  @Override
  public void renamePasskey(String userName, String passkeyId, String newLabel) {
    String cleanUser = requireUserName(userName);
    if (StringUtils.isBlank(passkeyId)) {
      throw new IllegalArgumentException("passkeyId is required");
    }
    String cleanLabel = normalizeLabel(newLabel);
    int updated = authSecurityDao.renamePasskey(passkeyId.trim(), cleanUser, cleanLabel);
    if (updated != 1) {
      throw new IllegalArgumentException("Passkey not found.");
    }
  }

  @Override
  public void deletePasskey(String userName, String passkeyId) {
    String cleanUser = requireUserName(userName);
    if (StringUtils.isBlank(passkeyId)) {
      throw new IllegalArgumentException("passkeyId is required");
    }
    int updated = authSecurityDao.revokePasskey(passkeyId.trim(), cleanUser);
    if (updated != 1) {
      throw new IllegalArgumentException("Passkey not found.");
    }
  }

  private RegistrationData parseAndVerifyRegistration(
      String json, RegistrationParameters params) {
    try {
      return webAuthnManager.verifyRegistrationResponseJSON(json, params);
    } catch (DataConversionException | VerificationException ex) {
      throw new IllegalArgumentException("Passkey registration verification failed: " + ex.getMessage(), ex);
    }
  }

  private AuthenticationData parseAuthenticationData(String json) {
    try {
      return webAuthnManager.parseAuthenticationResponseJSON(json);
    } catch (DataConversionException ex) {
      throw new IllegalArgumentException("Invalid passkey assertion payload: " + ex.getMessage(), ex);
    }
  }

  private AuthenticationData verifyAuthenticationData(String json, AuthenticationParameters params) {
    try {
      return webAuthnManager.verifyAuthenticationResponseJSON(json, params);
    } catch (DataConversionException | VerificationException ex) {
      throw new IllegalArgumentException("Passkey verification failed: " + ex.getMessage(), ex);
    }
  }

  private PSMfaPasskey buildPasskeyEntity(
      String userName,
      String label,
      String userHandle,
      String credentialIdB64Url,
      RegistrationData data) {
    AttestedCredentialData attested = data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
    COSEKey coseKey = attested.getCOSEKey();
    byte[] coseBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);

    PSMfaPasskey entity = new PSMfaPasskey();
    entity.setPasskeyId(UUID.randomUUID().toString());
    entity.setUserName(userName);
    entity.setCredentialId(credentialIdB64Url);
    entity.setUserHandle(userHandle);
    entity.setCosePublicKeyB64(Base64.getEncoder().encodeToString(coseBytes));
    AAGUID aaguid = attested.getAaguid();
    entity.setAaguid(aaguid != null ? aaguid.toString() : null);
    entity.setSignCount(data.getAttestationObject().getAuthenticatorData().getSignCount());
    entity.setTransports(joinTransports(data.getTransports()));
    entity.setBackupEligible(data.getAttestationObject().getAuthenticatorData().isFlagBE());
    entity.setBackupState(data.getAttestationObject().getAuthenticatorData().isFlagBS());
    entity.setLabel(label);
    entity.setStatus(PSMfaPasskey.STATUS_ACTIVE);
    entity.setCreatedAt(Date.from(clock.instant()));
    return entity;
  }

  private CredentialRecord toCredentialRecord(PSMfaPasskey stored) {
    byte[] credIdBytes = Base64UrlUtil.decode(stored.getCredentialId());
    byte[] coseBytes = Base64.getDecoder().decode(stored.getCosePublicKeyB64());
    COSEKey coseKey = objectConverter.getCborConverter().readValue(coseBytes, COSEKey.class);
    AAGUID aaguid =
        StringUtils.isNotBlank(stored.getAaguid()) ? new AAGUID(stored.getAaguid()) : AAGUID.ZERO;
    AttestedCredentialData attested = new AttestedCredentialData(aaguid, credIdBytes, coseKey);
    Set<AuthenticatorTransport> transports = parseTransports(stored.getTransports());
    return new CredentialRecordImpl(
        new NoneAttestationStatement(),
        Boolean.TRUE,
        stored.isBackupEligible(),
        stored.isBackupState(),
        stored.getSignCount(),
        attested,
        null,
        null,
        null,
        transports);
  }

  private String saveCeremonyTx(
      String txType,
      String userName,
      String challengeB64Url,
      PSWebAuthnRpConfig cfg,
      String userHandle,
      String label) {
    Instant now = clock.instant();
    PSAuthLoginTx tx = new PSAuthLoginTx();
    tx.setTxId(UUID.randomUUID().toString());
    tx.setTxType(txType);
    tx.setUserName(userName);
    tx.setChallengeB64(challengeB64Url);
    tx.setState(PSAuthLoginTx.STATE_PENDING);
    tx.setCreatedAt(Date.from(now));
    tx.setExpiresAt(Date.from(now.plus(CEREMONY_TTL)));

    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("rpId", cfg.rpId());
    if (StringUtils.isNotBlank(userHandle)) {
      meta.put("userHandle", userHandle);
    }
    if (StringUtils.isNotBlank(label)) {
      meta.put("label", label);
    }
    tx.setPayloadJson(JSON.writeValueAsString(meta));
    authSecurityDao.saveLoginTx(tx);
    return tx.getTxId();
  }

  private PSAuthLoginTx consumeCeremonyTx(String txId, String expectedType, String expectedUser) {
    if (StringUtils.isBlank(txId)) {
      throw new IllegalArgumentException("Passkey challenge session missing.");
    }
    String cleanTxId = txId.trim();
    Optional<PSAuthLoginTx> opt = authSecurityDao.findLoginTx(cleanTxId);
    if (opt.isEmpty()) {
      throw new IllegalArgumentException("Passkey challenge expired or invalid.");
    }
    PSAuthLoginTx tx = opt.get();
    if (!expectedUser.equalsIgnoreCase(tx.getUserName())) {
      throw new IllegalArgumentException("Passkey challenge does not belong to this user.");
    }
    int consumed =
        authSecurityDao.consumeLoginTxAtomically(cleanTxId, expectedType, Date.from(clock.instant()));
    if (consumed != 1) {
      throw new IllegalArgumentException("Passkey challenge was already used or expired.");
    }
    return tx;
  }

  private ServerProperty buildServerProperty(
      PSWebAuthnRpConfig cfg, Map<String, Object> payload, String challengeB64Url) {
    String boundRpId = StringUtils.defaultIfBlank(asString(payload.get("rpId")), cfg.rpId());
    if (!boundRpId.equalsIgnoreCase(cfg.rpId())) {
      throw new IllegalArgumentException("WebAuthn Relying Party ID mismatch.");
    }
    byte[] challengeBytes = Base64UrlUtil.decode(challengeB64Url);
    return ServerProperty.builder()
        .origins(cfg.allowedOrigins())
        .rpId(cfg.rpId())
        .challenge(new DefaultChallenge(challengeBytes))
        .build();
  }

  private String buildCreationOptionsJson(
      PSWebAuthnRpConfig cfg,
      String userName,
      String displayName,
      String userHandle,
      String challengeB64Url,
      List<PSMfaPasskey> existing) {
    Map<String, Object> opts = new LinkedHashMap<>();
    opts.put("rp", Map.of("id", cfg.rpId(), "name", cfg.rpName()));
    String effectiveDisplay = StringUtils.defaultIfBlank(StringUtils.trimToNull(displayName), userName);
    opts.put("user", Map.of("id", userHandle, "name", userName, "displayName", effectiveDisplay));
    opts.put("challenge", challengeB64Url);
    opts.put(
        "pubKeyCredParams",
        List.of(
            Map.of("type", "public-key", "alg", -7),
            Map.of("type", "public-key", "alg", -257),
            Map.of("type", "public-key", "alg", -8)));
    opts.put("timeout", CEREMONY_TIMEOUT_MS);
    opts.put("excludeCredentials", toCredentialDescriptors(existing));
    opts.put(
        "authenticatorSelection",
        Map.of(
            "residentKey", "preferred",
            "requireResidentKey", false,
            "userVerification", cfg.registrationUvRequired() ? "required" : "preferred"));
    opts.put("attestation", "none");
    return JSON.writeValueAsString(opts);
  }

  private String buildRequestOptionsJson(
      PSWebAuthnRpConfig cfg, String challengeB64Url, List<PSMfaPasskey> active) {
    Map<String, Object> opts = new LinkedHashMap<>();
    opts.put("challenge", challengeB64Url);
    opts.put("timeout", CEREMONY_TIMEOUT_MS);
    opts.put("rpId", cfg.rpId());
    opts.put("allowCredentials", toCredentialDescriptors(active));
    opts.put("userVerification", "required");
    return JSON.writeValueAsString(opts);
  }

  private static List<Map<String, Object>> toCredentialDescriptors(List<PSMfaPasskey> keys) {
    return keys.stream()
        .map(
            pk -> {
              Map<String, Object> desc = new LinkedHashMap<>();
              desc.put("type", "public-key");
              desc.put("id", pk.getCredentialId());
              List<String> transports = splitTransports(pk.getTransports());
              if (!transports.isEmpty()) {
                desc.put("transports", transports);
              }
              return desc;
            })
        .toList();
  }

  private String resolveOrCreateUserHandle(List<PSMfaPasskey> existing) {
    return existing.stream()
        .map(PSMfaPasskey::getUserHandle)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElseGet(() -> randomBase64Url(USER_HANDLE_BYTES));
  }

  private String generateChallenge() {
    return randomBase64Url(CHALLENGE_BYTES);
  }

  private String randomBase64Url(int byteLen) {
    byte[] buf = new byte[byteLen];
    secureRandom.nextBytes(buf);
    return Base64UrlUtil.encodeToString(buf);
  }

  public static PSPasskeySummary toSummary(PSMfaPasskey pk) {
    return new PSPasskeySummary(
        pk.getPasskeyId(),
        pk.getLabel(),
        pk.getAaguid(),
        splitTransports(pk.getTransports()),
        pk.isBackupEligible(),
        pk.isBackupState(),
        pk.getCreatedAt() != null ? pk.getCreatedAt().toInstant() : null,
        pk.getLastUsedAt() != null ? pk.getLastUsedAt().toInstant() : null);
  }

  private static String joinTransports(Set<AuthenticatorTransport> transports) {
    if (transports == null || transports.isEmpty()) {
      return "";
    }
    return String.join(
        ",", transports.stream().map(AuthenticatorTransport::getValue).sorted().toList());
  }

  private static List<String> splitTransports(String csv) {
    if (StringUtils.isBlank(csv)) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotEmpty)
        .toList();
  }

  private static Set<AuthenticatorTransport> parseTransports(String csv) {
    Set<AuthenticatorTransport> out = new LinkedHashSet<>();
    for (String item : splitTransports(csv)) {
      try {
        out.add(AuthenticatorTransport.create(item));
      } catch (IllegalArgumentException ignored) {
        // Skip unknown transport strings safely
      }
    }
    return out;
  }

  private static String normalizeLabel(String rawLabel) {
    String clean = StringUtils.trimToEmpty(rawLabel);
    if (clean.isEmpty()) {
      return "Passkey";
    }
    return clean.length() > MAX_LABEL_LEN ? clean.substring(0, MAX_LABEL_LEN) : clean;
  }

  private static String requireUserName(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    return userName.trim();
  }

  private static Map<String, Object> parsePayload(String payloadJson) {
    if (StringUtils.isBlank(payloadJson)) {
      return Map.of();
    }
    try {
      return JSON.readValue(payloadJson, MAP_TYPE);
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private static String asString(Object val) {
    return val instanceof String s ? s : "";
  }

  private static Properties defaultServerProperties() {
    try {
      Properties p = PSServer.getServerProps();
      return p != null ? p : new Properties();
    } catch (Exception ex) {
      return new Properties();
    }
  }
}
