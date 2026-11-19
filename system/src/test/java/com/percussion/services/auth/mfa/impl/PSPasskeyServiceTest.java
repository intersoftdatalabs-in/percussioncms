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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.mfa.PSPasskeyOptions;
import com.percussion.services.auth.mfa.PSPasskeyRegisterOutcome;
import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.AuthenticatorDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.util.Base64UrlUtil;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class PSPasskeyServiceTest {

  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final String ORIGIN = "https://cms.example.com";
  private static final String RP_ID = "cms.example.com";

  private record TestFixture(
      IPSAuthSecurityDao dao,
      Map<String, PSMfaPasskey> passkeysById,
      Map<String, PSAuthLoginTx> txById,
      PSPasskeyService service,
      ObjectConverter objectConverter,
      Clock clock) {}

  private TestFixture createFixture() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    Map<String, PSMfaPasskey> passkeys = new ConcurrentHashMap<>();
    Map<String, PSAuthLoginTx> txStore = new ConcurrentHashMap<>();
    Clock clock = Clock.fixed(Instant.parse("2026-11-19T01:00:00Z"), ZoneOffset.UTC);

    when(dao.saveLoginTx(any()))
        .thenAnswer(
            inv -> {
              PSAuthLoginTx tx = inv.getArgument(0);
              txStore.put(tx.getTxId(), tx);
              return tx;
            });
    when(dao.findLoginTx(anyString()))
        .thenAnswer(inv -> Optional.ofNullable(txStore.get(inv.getArgument(0))));
    when(dao.consumeLoginTxAtomically(anyString(), anyString(), any()))
        .thenAnswer(
            inv -> {
              String id = inv.getArgument(0);
              String type = inv.getArgument(1);
              PSAuthLoginTx tx = txStore.get(id);
              if (tx != null && type.equals(tx.getTxType()) && PSAuthLoginTx.STATE_PENDING.equals(tx.getState())) {
                tx.setState(PSAuthLoginTx.STATE_VERIFIED);
                return 1;
              }
              return 0;
            });
    when(dao.savePasskey(any()))
        .thenAnswer(
            inv -> {
              PSMfaPasskey pk = inv.getArgument(0);
              passkeys.put(pk.getPasskeyId(), pk);
              return pk;
            });
    when(dao.findActivePasskeysByUser(anyString()))
        .thenAnswer(
            inv -> {
              String user = inv.getArgument(0);
              return passkeys.values().stream()
                  .filter(
                      p -> user.equalsIgnoreCase(p.getUserName()) && PSMfaPasskey.STATUS_ACTIVE.equals(p.getStatus()))
                  .toList();
            });
    when(dao.findActivePasskeyByCredentialId(anyString()))
        .thenAnswer(
            inv -> {
              String cid = inv.getArgument(0);
              return passkeys.values().stream()
                  .filter(
                      p -> cid.equals(p.getCredentialId()) && PSMfaPasskey.STATUS_ACTIVE.equals(p.getStatus()))
                  .findFirst();
            });
    when(dao.renamePasskey(anyString(), anyString(), anyString()))
        .thenAnswer(
            inv -> {
              PSMfaPasskey pk = passkeys.get(inv.getArgument(0));
              if (pk != null
                  && ((String) inv.getArgument(1)).equalsIgnoreCase(pk.getUserName())
                  && PSMfaPasskey.STATUS_ACTIVE.equals(pk.getStatus())) {
                pk.setLabel(inv.getArgument(2));
                return 1;
              }
              return 0;
            });
    when(dao.revokePasskey(anyString(), anyString()))
        .thenAnswer(
            inv -> {
              PSMfaPasskey pk = passkeys.get(inv.getArgument(0));
              if (pk != null
                  && ((String) inv.getArgument(1)).equalsIgnoreCase(pk.getUserName())
                  && PSMfaPasskey.STATUS_ACTIVE.equals(pk.getStatus())) {
                pk.setStatus(PSMfaPasskey.STATUS_REVOKED);
                return 1;
              }
              return 0;
            });
    when(dao.updatePasskeySignCountAtomically(
            anyString(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), any()))
        .thenAnswer(
            inv -> {
              PSMfaPasskey pk = passkeys.get(inv.getArgument(0));
              long expected = inv.getArgument(1);
              long next = inv.getArgument(2);
              boolean be = inv.getArgument(3);
              boolean bs = inv.getArgument(4);
              Date usedAt = inv.getArgument(5);
              if (pk != null && pk.getSignCount() == expected) {
                pk.setSignCount(next);
                pk.setBackupEligible(be);
                pk.setBackupState(bs);
                pk.setLastUsedAt(usedAt);
                return 1;
              }
              return 0;
            });

    Properties props = new Properties();
    props.setProperty(PSWebAuthnConfigResolver.PROP_RP_ID, RP_ID);
    props.setProperty(PSWebAuthnConfigResolver.PROP_RP_NAME, "Percussion CMS");
    props.setProperty(PSWebAuthnConfigResolver.PROP_ALLOWED_ORIGINS, ORIGIN);

    ObjectConverter oc = new ObjectConverter();
    PSPasskeyService svc =
        new PSPasskeyService(dao, () -> props, new PSWebAuthnConfigResolver(), oc, clock);
    return new TestFixture(dao, passkeys, txStore, svc, oc, clock);
  }

  private static KeyPair generateEcKeyPair() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(new ECGenParameterSpec("secp256r1"));
    return kpg.generateKeyPair();
  }

  private String buildRegistrationResponse(
      ObjectConverter oc,
      String rpId,
      String origin,
      String challenge,
      byte[] credId,
      KeyPair kp,
      long signCount,
      byte flags)
      throws Exception {
    byte[] clientDataBytes =
        JSON.writeValueAsString(
                Map.of("type", "webauthn.create", "challenge", challenge, "origin", origin, "crossOrigin", false))
            .getBytes(StandardCharsets.UTF_8);
    byte[] rpIdHash = MessageDigest.getInstance("SHA-256").digest(rpId.getBytes(StandardCharsets.UTF_8));
    EC2COSEKey coseKey = EC2COSEKey.create(kp, COSEAlgorithmIdentifier.ES256);
    AttestedCredentialData attested = new AttestedCredentialData(AAGUID.ZERO, credId, coseKey);
    AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authData =
        new AuthenticatorData<>(rpIdHash, flags, signCount, attested);
    AttestationObject attObj = new AttestationObject(authData, new NoneAttestationStatement());
    byte[] attObjBytes = new AttestationObjectConverter(oc).convertToBytes(attObj);

    String credIdB64Url = Base64UrlUtil.encodeToString(credId);
    return JSON.writeValueAsString(
        Map.of(
            "id", credIdB64Url,
            "rawId", credIdB64Url,
            "type", "public-key",
            "response",
                Map.of(
                    "clientDataJSON", Base64UrlUtil.encodeToString(clientDataBytes),
                    "attestationObject", Base64UrlUtil.encodeToString(attObjBytes),
                    "transports", List.of("internal")),
            "clientExtensionResults", Map.of()));
  }

  private String buildAuthenticationResponse(
      ObjectConverter oc,
      String rpId,
      String origin,
      String challenge,
      byte[] credId,
      KeyPair kp,
      long signCount,
      byte flags,
      boolean corruptSignature)
      throws Exception {
    byte[] clientDataBytes =
        JSON.writeValueAsString(
                Map.of("type", "webauthn.get", "challenge", challenge, "origin", origin, "crossOrigin", false))
            .getBytes(StandardCharsets.UTF_8);
    byte[] clientDataHash = MessageDigest.getInstance("SHA-256").digest(clientDataBytes);
    byte[] rpIdHash = MessageDigest.getInstance("SHA-256").digest(rpId.getBytes(StandardCharsets.UTF_8));
    AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> authData =
        new AuthenticatorData<>(rpIdHash, flags, signCount);
    byte[] authDataBytes = new AuthenticatorDataConverter(oc).convert(authData);

    Signature signer = Signature.getInstance("SHA256withECDSA");
    signer.initSign((ECPrivateKey) kp.getPrivate());
    signer.update(authDataBytes);
    signer.update(clientDataHash);
    byte[] sig = signer.sign();
    if (corruptSignature) {
      sig[sig.length - 1] ^= 0x55;
    }

    String credIdB64Url = Base64UrlUtil.encodeToString(credId);
    return JSON.writeValueAsString(
        Map.of(
            "id", credIdB64Url,
            "rawId", credIdB64Url,
            "type", "public-key",
            "response",
                Map.of(
                    "clientDataJSON", Base64UrlUtil.encodeToString(clientDataBytes),
                    "authenticatorData", Base64UrlUtil.encodeToString(authDataBytes),
                    "signature", Base64UrlUtil.encodeToString(sig)),
            "clientExtensionResults", Map.of()));
  }

  @Test
  void registersVerifiesSyncedAndCounteredPasskeysAndRejectsSecurityViolations() throws Exception {
    TestFixture f = createFixture();
    KeyPair kp = generateEcKeyPair();
    byte[] credId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    PSPasskeyOptions regOpts = f.service().startRegistration("alice", "Alice", "Laptop", ORIGIN);
    String regChallenge = (String) JSON.readValue(regOpts.optionsJson(), MAP_TYPE).get("challenge");
    byte regFlags =
        (byte)
            (AuthenticatorData.BIT_UP
                | AuthenticatorData.BIT_UV
                | AuthenticatorData.BIT_BE
                | AuthenticatorData.BIT_BS
                | AuthenticatorData.BIT_AT);
    String regResp =
        buildRegistrationResponse(f.objectConverter(), RP_ID, ORIGIN, regChallenge, credId, kp, 0L, regFlags);

    PSPasskeyRegisterOutcome outcome =
        f.service().finishRegistration("alice", regOpts.challengeTxId(), "Laptop", regResp, ORIGIN);
    assertTrue(outcome.registered());
    assertTrue(outcome.passkey().backupEligible());
    assertTrue(outcome.passkey().backupState());

    // Challenge replay fails
    assertThrows(
        IllegalArgumentException.class,
        () -> f.service().finishRegistration("alice", regOpts.challengeTxId(), "Laptop", regResp, ORIGIN));

    // Synced passkey signCount 0 -> 0 succeeds
    byte authFlags =
        (byte)
            (AuthenticatorData.BIT_UP
                | AuthenticatorData.BIT_UV
                | AuthenticatorData.BIT_BE
                | AuthenticatorData.BIT_BS);
    PSPasskeyOptions auth0 = f.service().startAuthentication("alice", ORIGIN);
    String chal0 = (String) JSON.readValue(auth0.optionsJson(), MAP_TYPE).get("challenge");
    String assert0 =
        buildAuthenticationResponse(f.objectConverter(), RP_ID, ORIGIN, chal0, credId, kp, 0L, authFlags, false);
    assertEquals(0L, f.service().verifyAuthentication("alice", auth0.challengeTxId(), assert0, ORIGIN).getSignCount());

    // Counter increments to 5 succeed
    PSPasskeyOptions auth5 = f.service().startAuthentication("alice", ORIGIN);
    String chal5 = (String) JSON.readValue(auth5.optionsJson(), MAP_TYPE).get("challenge");
    String assert5 =
        buildAuthenticationResponse(f.objectConverter(), RP_ID, ORIGIN, chal5, credId, kp, 5L, authFlags, false);
    assertEquals(5L, f.service().verifyAuthentication("alice", auth5.challengeTxId(), assert5, ORIGIN).getSignCount());

    // Sign-count regression (5 -> 4) fails
    PSPasskeyOptions authRegress = f.service().startAuthentication("alice", ORIGIN);
    String chalReg = (String) JSON.readValue(authRegress.optionsJson(), MAP_TYPE).get("challenge");
    String assertReg =
        buildAuthenticationResponse(f.objectConverter(), RP_ID, ORIGIN, chalReg, credId, kp, 4L, authFlags, false);
    assertThrows(
        IllegalArgumentException.class,
        () -> f.service().verifyAuthentication("alice", authRegress.challengeTxId(), assertReg, ORIGIN));

    // Tampered signature fails
    PSPasskeyOptions authBadSig = f.service().startAuthentication("alice", ORIGIN);
    String chalBadSig = (String) JSON.readValue(authBadSig.optionsJson(), MAP_TYPE).get("challenge");
    String assertBadSig =
        buildAuthenticationResponse(f.objectConverter(), RP_ID, ORIGIN, chalBadSig, credId, kp, 6L, authFlags, true);
    assertThrows(
        IllegalArgumentException.class,
        () -> f.service().verifyAuthentication("alice", authBadSig.challengeTxId(), assertBadSig, ORIGIN));

    // Origin mismatch fails
    PSPasskeyOptions authOrigin = f.service().startAuthentication("alice", ORIGIN);
    String chalOrigin = (String) JSON.readValue(authOrigin.optionsJson(), MAP_TYPE).get("challenge");
    String assertOrigin =
        buildAuthenticationResponse(
            f.objectConverter(), RP_ID, "https://attacker.example", chalOrigin, credId, kp, 6L, authFlags, false);
    assertThrows(
        IllegalArgumentException.class,
        () -> f.service().verifyAuthentication("alice", authOrigin.challengeTxId(), assertOrigin, ORIGIN));

    // Missing UV (UV=false) fails
    PSPasskeyOptions authNoUv = f.service().startAuthentication("alice", ORIGIN);
    String chalNoUv = (String) JSON.readValue(authNoUv.optionsJson(), MAP_TYPE).get("challenge");
    String assertNoUv =
        buildAuthenticationResponse(
            f.objectConverter(), RP_ID, ORIGIN, chalNoUv, credId, kp, 6L, AuthenticatorData.BIT_UP, false);
    assertThrows(
        IllegalArgumentException.class,
        () -> f.service().verifyAuthentication("alice", authNoUv.challengeTxId(), assertNoUv, ORIGIN));

    // Rename & delete work cleanly
    String passkeyId = outcome.passkey().passkeyId();
    f.service().renamePasskey("alice", passkeyId, "YubiKey 5");
    assertEquals("YubiKey 5", f.service().listPasskeys("alice").get(0).label());
    f.service().deletePasskey("alice", passkeyId);
    assertTrue(f.service().listPasskeys("alice").isEmpty());
  }
}
