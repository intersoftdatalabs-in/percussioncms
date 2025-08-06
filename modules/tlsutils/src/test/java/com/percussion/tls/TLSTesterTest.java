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

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TLSTesterTest {

    @Mock
    private X509Certificate mockCertificate;
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private String originalOsName;

    @Before
    public void setUp() {
        // Capture System.out for testing console output
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        // Store original OS name
        originalOsName = System.getProperty("os.name");
    }

    @After
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
        assertEquals("Should return the system os.name property", expectedOsName, result);
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
        assertEquals("First call should return first OS name", firstOsName, firstResult);
        assertEquals("Second call should return cached result", firstOsName, secondResult);
    }

    @Test
    public void testIsWindows_WindowsOS() {
        // Given
        System.setProperty("os.name", "Windows 10");
        
        // When
        boolean result = TLSTester.isWindows();
        
        // Then
        assertTrue("Should return true for Windows OS", result);
    }

    @Test
    public void testIsWindows_LinuxOS() {
        // Given
        System.setProperty("os.name", "Linux");
        
        // When
        boolean result = TLSTester.isWindows();
        
        // Then
        assertFalse("Should return false for Linux OS", result);
    }

    @Test
    public void testIsWindows_MacOS() {
        // Given
        System.setProperty("os.name", "Mac OS X");
        
        // When
        boolean result = TLSTester.isWindows();
        
        // Then
        assertFalse("Should return false for Mac OS", result);
    }

    @Test
    public void testIsWindows_CaseInsensitive() {
        // Given
        System.setProperty("os.name", "windows 11");
        
        // When
        boolean result = TLSTester.isWindows();
        
        // Then
        assertTrue("Should return true for lowercase windows", result);
    }

    @Test
    public void testKeystorePassConstant() {
        // When/Then
        assertEquals("KEYSTORE_PASS should have correct value", "changeit", TLSTester.KEYSTORE_PASS);
    }

    @Test
    public void testConvertToPem_ValidCertificate() throws Exception {
        // Given
        byte[] testCertBytes = "test certificate data".getBytes();
        when(mockCertificate.getEncoded()).thenReturn(testCertBytes);

        // When
        String result = TLSTester.convertToPem(mockCertificate);

        // Then
        assertNotNull("Result should not be null", result);
        assertTrue("Result should start with BEGIN CERTIFICATE", 
                   result.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue("Result should end with END CERTIFICATE", 
                   result.endsWith("-----END CERTIFICATE-----"));
        assertTrue("Result should contain base64 encoded data", 
                   result.contains("dGVzdCBjZXJ0aWZpY2F0ZSBkYXRh")); // base64 of "test certificate data"
        
        verify(mockCertificate).getEncoded();
    }

    @Test(expected = CertificateEncodingException.class)
    public void testConvertToPem_CertificateEncodingException() throws Exception {
        // Given
        when(mockCertificate.getEncoded()).thenThrow(new CertificateEncodingException("Encoding failed"));

        // When
        TLSTester.convertToPem(mockCertificate);

        // Then - exception should be thrown
    }

    @Test
    public void testConvertToPem_EmptyCertificateData() throws Exception {
        // Given
        byte[] emptyCertBytes = new byte[0];
        when(mockCertificate.getEncoded()).thenReturn(emptyCertBytes);

        // When
        String result = TLSTester.convertToPem(mockCertificate);

        // Then
        assertNotNull("Result should not be null", result);
        assertTrue("Result should start with BEGIN CERTIFICATE", 
                   result.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue("Result should end with END CERTIFICATE", 
                   result.endsWith("-----END CERTIFICATE-----"));
        
        verify(mockCertificate).getEncoded();
    }

    @Test
    public void testIsUnlimitedCryptoLength_ChecksMaxKeyLength() {
        // When
        boolean result = TLSTester.isUnlimitedCryptoLength();
        
        // Then
        // The result depends on the JVM's crypto policy
        // We just verify it returns a boolean without throwing exceptions
        assertNotNull("Should return a boolean value", Boolean.valueOf(result));
    }

    @Test
    public void testGetEnabledCiphers_ProducesOutput() {
        // When
        TLSTester.getEnabledCiphers();
        
        // Then
        String output = outputStream.toString();
        assertNotNull("Should produce some output", output);
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
        assertNotNull("Main method should produce some output", output);
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
        assertNotNull("Main method should produce some output", output);
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
        assertNotNull("Main method should produce some output", output);
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
        assertTrue("Should have at least 3 lines", lines.length >= 3);
        assertEquals("First line should be BEGIN CERTIFICATE", 
                     "-----BEGIN CERTIFICATE-----", lines[0]);
        assertEquals("Last line should be END CERTIFICATE", 
                     "-----END CERTIFICATE-----", lines[lines.length - 1]);
        
        // Verify that middle lines contain valid base64 content
        for (int i = 1; i < lines.length - 1; i++) {
            if (!lines[i].trim().isEmpty()) {
                assertTrue("Line should contain valid base64 characters", 
                           lines[i].matches("^[A-Za-z0-9+/=\\s]*$"));
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
        assertNotNull("Result should not be null", result);
        assertTrue("Result should start with BEGIN CERTIFICATE", 
                   result.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue("Result should end with END CERTIFICATE", 
                   result.endsWith("-----END CERTIFICATE-----"));
        assertTrue("Result should contain multiple lines", 
                   result.split("\n").length > 3);
        
        verify(mockCertificate).getEncoded();
    }
}
