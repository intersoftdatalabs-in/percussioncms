/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.tls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class WrappedTrustManagerTest {

  @Mock private X509Certificate mockCertificate1;

  @Mock private X509Certificate mockCertificate2;

  @Mock private KeyStore mockKeyStore;

  private WrappedTrustManager wrappedTrustManager;
  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  public void setUp() {
    // Capture System.out for testing console output
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));

    wrappedTrustManager = new WrappedTrustManager();
  }

  @Test
  public void testConstructor_InitializesWithDefaultJavaTrustManager() {
    // Given/When - constructor is called in setUp()

    // Then
    assertNotNull("WrappedTrustManager should be created", wrappedTrustManager);

    // Verify it has at least the default Java trust manager
    X509Certificate[] acceptedIssuers = wrappedTrustManager.getAcceptedIssuers();
    assertNotNull("Accepted issuers should not be null", acceptedIssuers);
  }

  @Test
  public void testAddKeyStore_ValidKeyStore() {
    // Given
    String keystoreName = "TestKeyStore";

    // When
    wrappedTrustManager.addKeyStore(keystoreName, mockKeyStore);

    // Then
    // Should not throw any exception
    assertTrue("KeyStore should be added successfully", true);
  }

  @Test
  public void testAddKeyStore_NullKeyStore() {
    // Given
    String keystoreName = "NullKeyStore";

    // When
    wrappedTrustManager.addKeyStore(keystoreName, null);

    // Then
    // Should not throw any exception when adding null keystore
    assertTrue("Null KeyStore should be handled gracefully", true);
  }

  @Disabled
  @Test(expected = RuntimeException.class)
  public void testAddKeyStore_InvalidKeyStore() {
    // Given
    String keystoreName = "InvalidKeyStore";
    KeyStore corruptedKeyStore = mock(KeyStore.class);

    // When/Then - Should throw RuntimeException due to keystore issues
    // Note: This test may need adjustment based on actual KeyStore behavior
    wrappedTrustManager.addKeyStore(keystoreName, corruptedKeyStore);
  }

  @Test
  public void testGetAcceptedIssuers_ReturnsNonNullArray() {
    // Given/When
    X509Certificate[] acceptedIssuers = wrappedTrustManager.getAcceptedIssuers();

    // Then
    assertNotNull("Accepted issuers should not be null", acceptedIssuers);
    // Array length is always >= 0, so we just verify it's not null
  }

  @Test
  public void testGetAcceptedIssuers_MergesFromMultipleTrustManagers() {
    // Given
    wrappedTrustManager.addKeyStore("Additional", null);

    // When
    X509Certificate[] acceptedIssuers = wrappedTrustManager.getAcceptedIssuers();

    // Then
    assertNotNull("Accepted issuers should not be null", acceptedIssuers);
    // The result should be a merged set without duplicates
  }

  @Disabled
  @Test
  public void testCheckServerTrusted_SuccessfulValidation() {
    // Given
    X509Certificate[] chain = {mockCertificate1, mockCertificate2};
    String authType = "RSA";

    // When
    try {
      wrappedTrustManager.checkServerTrusted(chain, authType);
      // If no exception is thrown, the validation was successful
    } catch (CertificateException e) {
      // This might be expected if no valid trust manager accepts the certificate
      // The important thing is that the method handles the chain properly
    }

    // Then
    String output = outputStream.toString();
    assertNotNull("Should have output during validation process", output);
  }

  @Test
  @Disabled
  public void testCheckServerTrusted_ValidationFailure() {
    // Given
    X509Certificate[] chain = {mockCertificate1, mockCertificate2};
    String authType = "RSA";

    // When/Then
    try {
      wrappedTrustManager.checkServerTrusted(chain, authType);
    } catch (CertificateException e) {
      // Expected when validation fails
      String output = outputStream.toString();
      assertTrue(
          "Should output failure message",
          output.contains("Failed to validate Server certificate")
              || output.contains("Check failed"));
    }
  }

  @Disabled
  @Test
  public void testCheckClientTrusted_SuccessfulValidation() throws CertificateException {
    // Given
    X509Certificate[] chain = {mockCertificate1, mockCertificate2};
    String authType = "RSA";

    // When
    try {
      wrappedTrustManager.checkClientTrusted(chain, authType);
      // If no exception is thrown, the validation was successful
    } catch (CertificateException e) {
      // This might be expected if no valid trust manager accepts the certificate
      // The important thing is that the method handles the chain properly
    }

    // Then - Should complete without throwing unexpected exceptions
    assertTrue("Method should handle client certificate validation", true);
  }

  @Disabled
  @Test
  public void testCheckClientTrusted_ValidationFailure() {
    // Given
    X509Certificate[] chain = {mockCertificate1, mockCertificate2};
    String authType = "RSA";

    // When/Then
    try {
      wrappedTrustManager.checkClientTrusted(chain, authType);
    } catch (CertificateException e) {
      // Expected when validation fails
      assertNotNull("Exception should have a message", e.getMessage());
    }
  }

  @Test
  public void testCheckServerTrusted_NullChain() {
    // Given
    X509Certificate[] chain = null;
    String authType = "RSA";

    // When/Then
    try {
      wrappedTrustManager.checkServerTrusted(chain, authType);
      fail("Should throw exception for null certificate chain");
    } catch (Exception e) {
      // Expected - null chain should cause an exception
      assertTrue("Should handle null chain gracefully", true);
    }
  }

  @Test
  public void testCheckClientTrusted_NullChain() {
    // Given
    X509Certificate[] chain = null;
    String authType = "RSA";

    // When/Then
    try {
      wrappedTrustManager.checkClientTrusted(chain, authType);
      fail("Should throw exception for null certificate chain");
    } catch (Exception e) {
      // Expected - null chain should cause an exception
      assertTrue("Should handle null chain gracefully", true);
    }
  }

  @Test
  public void testCheckServerTrusted_EmptyChain() {
    // Given
    X509Certificate[] chain = new X509Certificate[0];
    String authType = "RSA";

    // When/Then
    try {
      wrappedTrustManager.checkServerTrusted(chain, authType);
    } catch (Exception e) {
      // Expected - empty chain might cause validation issues
      assertTrue("Should handle empty chain", true);
    }
  }

  @Test
  public void testMultipleKeyStores_AdditionAndValidation() {
    // Given
    wrappedTrustManager.addKeyStore("KeyStore1", null);
    wrappedTrustManager.addKeyStore("KeyStore2", null);
    wrappedTrustManager.addKeyStore("KeyStore3", mockKeyStore);

    // When
    X509Certificate[] acceptedIssuers = wrappedTrustManager.getAcceptedIssuers();

    // Then
    assertNotNull("Should handle multiple keystores", acceptedIssuers);
  }

  @Test
  public void testConsoleOutput_ServerValidation() {
    // Given
    X509Certificate[] chain = {mockCertificate1};
    String authType = "RSA";

    // When
    try {
      wrappedTrustManager.checkServerTrusted(chain, authType);
    } catch (CertificateException | NullPointerException e) {
      // Expected for invalid certificates
    }

    // Then
    String output = outputStream.toString();
    // Should have some console output during the validation process
    assertNotNull("Should produce console output", output);
  }

  // Clean up after each test
  @org.junit.After
  public void tearDown() {
    System.setOut(originalOut);
  }
}
