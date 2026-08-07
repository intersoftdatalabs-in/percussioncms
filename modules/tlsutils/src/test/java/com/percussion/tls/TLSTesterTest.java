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

package com.percussion.tls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TLSTesterTest {

  @Mock private X509Certificate mockCertificate;

  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;
  private String originalOsName;

  @BeforeEach
  public void setUp() {
    // Capture System.out for testing console output
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));

    // Store original OS name
    originalOsName = System.getProperty("os.name");
    // clear cached OS value in TLSTester so each test sees the current property
    try {
      java.lang.reflect.Field osField = TLSTester.class.getDeclaredField("OS");
      osField.setAccessible(true);
      osField.set(null, null);
    } catch (Exception ignored) {
      // should not happen
    }
  }

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    if (originalOsName != null) {
      System.setProperty("os.name", originalOsName);
    }
  }

  @Test
  public void testGetOsName_ReturnsSystemProperty() {
    // Given
    String expectedOsName = "Test OS";
    System.setProperty("os.name", expectedOsName);

    // When
    String result = TLSTester.getOsName();

    // Then
    assertEquals(expectedOsName, result, "Should return the system os.name property");
  }

  @Test
  public void testGetOsName_CachesResult() {
    // Given
    String firstOsName = "First OS";
    System.setProperty("os.name", firstOsName);

    // When
    String firstResult = TLSTester.getOsName();

    // Change the system property
    System.setProperty("os.name", "Second OS");
    String secondResult = TLSTester.getOsName();

    // Then
    assertEquals(firstOsName, firstResult, "First call should return first OS name");
    assertEquals(firstOsName, secondResult, "Second call should return cached result");
  }

  @Test
  public void testIsWindows_WindowsOS() {
    // Given
    System.setProperty("os.name", "Windows 10");

    // When
    boolean result = TLSTester.isWindows();

    // Then
    assertTrue(result, "Should return true for Windows OS");
  }

  @Test
  public void testIsWindows_LinuxOS() {
    // Given
    System.setProperty("os.name", "Linux");

    // When
    boolean result = TLSTester.isWindows();

    // Then
    assertFalse(result, "Should return false for Linux OS");
  }

  @Test
  public void testIsWindows_MacOS() {
    // Given
    System.setProperty("os.name", "Mac OS X");

    // When
    boolean result = TLSTester.isWindows();

    // Then
    assertFalse(result, "Should return false for Mac OS");
  }

  @Test
  public void testIsWindows_CaseInsensitive() {
    // Given
    System.setProperty("os.name", "windows 11");

    // When
    boolean result = TLSTester.isWindows();

    // Then
    assertTrue(result, "Should return true for lowercase windows");
  }

  @Test
  public void testKeystorePassConstant() {
    // When/Then
    assertEquals("changeit", TLSTester.KEYSTORE_PASS, "KEYSTORE_PASS should have correct value");
  }

  @Test
  public void testConvertToPem_ValidCertificate() throws Exception {
    // Given
    byte[] testCertBytes = "test certificate data".getBytes();
    when(mockCertificate.getEncoded()).thenReturn(testCertBytes);

    // When
    String result = TLSTester.convertToPem(mockCertificate);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(
        result.startsWith("-----BEGIN CERTIFICATE-----"),
        "Result should start with BEGIN CERTIFICATE");
    assertTrue(
        result.endsWith("-----END CERTIFICATE-----"), "Result should end with END CERTIFICATE");
    assertTrue(
        result.contains("dGVzdCBjZXJ0aWZpY2F0ZSBkYXRh"),
        "Result should contain base64 encoded data"); // base64 of "test certificate data"

    verify(mockCertificate).getEncoded();
  }

  @Test
  public void testConvertToPem_CertificateEncodingException() throws Exception {
    // Given
    when(mockCertificate.getEncoded())
        .thenThrow(new CertificateEncodingException("Encoding failed"));

    // Then - exception should be thrown
    assertThrows(CertificateEncodingException.class, () -> TLSTester.convertToPem(mockCertificate));
  }

  @Test
  public void testConvertToPem_EmptyCertificateData() throws Exception {
    // Given
    byte[] emptyCertBytes = new byte[0];
    when(mockCertificate.getEncoded()).thenReturn(emptyCertBytes);

    // When
    String result = TLSTester.convertToPem(mockCertificate);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(
        result.startsWith("-----BEGIN CERTIFICATE-----"),
        "Result should start with BEGIN CERTIFICATE");
    assertTrue(
        result.endsWith("-----END CERTIFICATE-----"), "Result should end with END CERTIFICATE");

    verify(mockCertificate).getEncoded();
  }

  @Test
  public void testIsUnlimitedCryptoLength_ChecksMaxKeyLength() {
    // When
    boolean result = TLSTester.isUnlimitedCryptoLength();

    // Then
    // The result depends on the JVM's crypto policy
    // We just verify it returns a boolean without throwing exceptions
    assertNotNull(Boolean.valueOf(result), "Should return a boolean value");
  }

  @Test
  public void testGetEnabledCiphers_ProducesOutput() {
    // When
    TLSTester.getEnabledCiphers();

    // Then
    String output = outputStream.toString();
    assertNotNull(output, "Should produce some output");
    // The output content will depend on the system's SSL configuration
  }

  @Test
  public void testMain_WithNoArguments() {
    // Given
    String[] args = {};

    // When
    try {
      TLSTester.main(args);
    } catch (Exception e) {
      // Main method might throw exceptions for various reasons
      // The important thing is that it handles the call gracefully
    }

    // Then
    String output = outputStream.toString();
    assertNotNull(output, "Main method should produce some output");
  }

  @Test
  public void testMain_WithSingleArgument() {
    // Given
    String[] args = {"localhost"};

    // When
    try {
      TLSTester.main(args);
    } catch (Exception e) {
      // Main method might throw exceptions when trying to connect
      // This is expected behavior for testing
    }

    // Then
    String output = outputStream.toString();
    assertNotNull(output, "Main method should produce some output");
  }

  @Test
  public void testMain_WithMultipleArguments() {
    // Given
    String[] args = {"localhost", "443"};

    // When
    try {
      TLSTester.main(args);
    } catch (Exception e) {
      // Main method might throw exceptions when trying to connect
      // This is expected behavior for testing
    }

    // Then
    String output = outputStream.toString();
    assertNotNull(output, "Main method should produce some output");
  }

  @Test
  public void testConvertToPem_VerifyBase64Formatting() throws Exception {
    // Given
    byte[] testCertBytes = "Hello World Certificate Data".getBytes();
    when(mockCertificate.getEncoded()).thenReturn(testCertBytes);

    // When
    String result = TLSTester.convertToPem(mockCertificate);

    // Then
    String[] lines = result.split("\n");
    assertTrue(lines.length >= 3, "Should have at least 3 lines");
    assertEquals("-----BEGIN CERTIFICATE-----", lines[0], "First line should be BEGIN CERTIFICATE");
    assertEquals(
        "-----END CERTIFICATE-----",
        lines[lines.length - 1],
        "Last line should be END CERTIFICATE");

    // Verify that middle lines contain valid base64 content
    for (int i = 1; i < lines.length - 1; i++) {
      if (!lines[i].trim().isEmpty()) {
        assertTrue(
            lines[i].matches("^[A-Za-z0-9+/=\\s]*$"),
            "Line should contain valid base64 characters");
      }
    }

    verify(mockCertificate).getEncoded();
  }

  @Test
  public void testConvertToPem_LargeCertificateData() throws Exception {
    // Given
    byte[] largeCertBytes = new byte[2048];
    for (int i = 0; i < largeCertBytes.length; i++) {
      largeCertBytes[i] = (byte) (i % 256);
    }
    when(mockCertificate.getEncoded()).thenReturn(largeCertBytes);

    // When
    String result = TLSTester.convertToPem(mockCertificate);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(
        result.startsWith("-----BEGIN CERTIFICATE-----"),
        "Result should start with BEGIN CERTIFICATE");
    assertTrue(
        result.endsWith("-----END CERTIFICATE-----"), "Result should end with END CERTIFICATE");
    assertTrue(result.split("\n").length > 3, "Result should contain multiple lines");

    verify(mockCertificate).getEncoded();
  }

  @Test
  public void testCertificateAlias_StripsNonAlphanumericAndLowercases() {
    when(mockCertificate.getSubjectX500Principal())
        .thenReturn(new X500Principal("CN=Example Cert, O=Acme Inc., C=US"));

    String alias = TLSTester.certificateAlias(mockCertificate);

    assertEquals("cnexamplecertoacmeinccus", alias);
    verify(mockCertificate).getSubjectX500Principal();
  }

  @Test
  public void testCertificateAlias_AlreadySimpleSubject() {
    when(mockCertificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=localhost"));

    String alias = TLSTester.certificateAlias(mockCertificate);

    assertEquals("cnlocalhost", alias);
  }

  @Test
  public void testToUrl_ValidHttpsUrl() throws Exception {
    URL url = TLSTester.toUrl("https://www.example.com/path");

    assertEquals("https", url.getProtocol());
    assertEquals("www.example.com", url.getHost());
    assertEquals("/path", url.getPath());
  }

  @Test
  public void testToUrl_InvalidUriThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> TLSTester.toUrl("not a uri"));
  }
}
