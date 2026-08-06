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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TLSUtilsTest {

  @Mock private X509Certificate mockCertificate;

  @Test
  public void testConvertToPem_ValidCertificate() throws Exception {
    // Given
    byte[] testCertBytes = "test certificate data".getBytes();
    when(mockCertificate.getEncoded()).thenReturn(testCertBytes);

    // When
    String result = TLSUtils.convertToPem(mockCertificate);

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
    assertThrows(CertificateEncodingException.class, () -> TLSUtils.convertToPem(mockCertificate));
  }

  @Test
  public void testConvertToPem_EmptyCertificateData() throws Exception {
    // Given
    byte[] emptyCertBytes = new byte[0];
    when(mockCertificate.getEncoded()).thenReturn(emptyCertBytes);

    // When
    String result = TLSUtils.convertToPem(mockCertificate);

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
  public void testConvertToPem_LargeCertificateData() throws Exception {
    // Given
    byte[] largeCertBytes = new byte[2048];
    for (int i = 0; i < largeCertBytes.length; i++) {
      largeCertBytes[i] = (byte) (i % 256);
    }
    when(mockCertificate.getEncoded()).thenReturn(largeCertBytes);

    // When
    String result = TLSUtils.convertToPem(mockCertificate);

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
  public void testConvertToPem_VerifyBase64Formatting() throws Exception {
    // Given
    byte[] testCertBytes = "Hello World Certificate Data".getBytes();
    when(mockCertificate.getEncoded()).thenReturn(testCertBytes);

    // When
    String result = TLSUtils.convertToPem(mockCertificate);

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
}
