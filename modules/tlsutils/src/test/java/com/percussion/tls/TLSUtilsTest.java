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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

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
    assertNotNull("Result should not be null", result);
    assertTrue(
        "Result should start with BEGIN CERTIFICATE",
        result.startsWith("-----BEGIN CERTIFICATE-----"));
    assertTrue(
        "Result should end with END CERTIFICATE", result.endsWith("-----END CERTIFICATE-----"));
    assertTrue(
        "Result should contain base64 encoded data",
        result.contains("dGVzdCBjZXJ0aWZpY2F0ZSBkYXRh")); // base64 of "test certificate data"

    verify(mockCertificate).getEncoded();
  }

  @Test(expected = CertificateEncodingException.class)
  public void testConvertToPem_CertificateEncodingException() throws Exception {
    // Given
    when(mockCertificate.getEncoded())
        .thenThrow(new CertificateEncodingException("Encoding failed"));

    // When
    TLSUtils.convertToPem(mockCertificate);

    // Then - exception should be thrown
  }

  @Test
  public void testConvertToPem_EmptyCertificateData() throws Exception {
    // Given
    byte[] emptyCertBytes = new byte[0];
    when(mockCertificate.getEncoded()).thenReturn(emptyCertBytes);

    // When
    String result = TLSUtils.convertToPem(mockCertificate);

    // Then
    assertNotNull("Result should not be null", result);
    assertTrue(
        "Result should start with BEGIN CERTIFICATE",
        result.startsWith("-----BEGIN CERTIFICATE-----"));
    assertTrue(
        "Result should end with END CERTIFICATE", result.endsWith("-----END CERTIFICATE-----"));

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
    assertNotNull("Result should not be null", result);
    assertTrue(
        "Result should start with BEGIN CERTIFICATE",
        result.startsWith("-----BEGIN CERTIFICATE-----"));
    assertTrue(
        "Result should end with END CERTIFICATE", result.endsWith("-----END CERTIFICATE-----"));
    assertTrue("Result should contain multiple lines", result.split("\n").length > 3);

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
    assertTrue("Should have at least 3 lines", lines.length >= 3);
    assertEquals("First line should be BEGIN CERTIFICATE", "-----BEGIN CERTIFICATE-----", lines[0]);
    assertEquals(
        "Last line should be END CERTIFICATE",
        "-----END CERTIFICATE-----",
        lines[lines.length - 1]);

    // Verify that middle lines contain valid base64 content
    for (int i = 1; i < lines.length - 1; i++) {
      if (!lines[i].trim().isEmpty()) {
        assertTrue(
            "Line should contain valid base64 characters",
            lines[i].matches("^[A-Za-z0-9+/=\\s]*$"));
      }
    }

    verify(mockCertificate).getEncoded();
  }
}
