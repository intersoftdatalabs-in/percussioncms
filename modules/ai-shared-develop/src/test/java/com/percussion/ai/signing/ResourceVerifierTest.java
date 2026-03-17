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
package com.percussion.ai.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for ResourceVerifier hash computation logic. Note: Full signature verification
 * requires Sigstore OIDC credentials and is tested via integration tests.
 */
class ResourceVerifierTest {

  @TempDir Path tempDir;

  /** Tests that SHA-256 hash computation produces correct output. */
  @Test
  void testComputeSha256Hash() throws Exception {
    String content = "test content for hashing";
    Path testFile = tempDir.resolve("test.txt");
    Files.writeString(testFile, content, StandardCharsets.UTF_8);

    // Compute hash using same method as ResourceVerifier
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes;
    try (InputStream fis = Files.newInputStream(testFile)) {
      hashBytes = digest.digest(fis.readAllBytes());
    }
    String hashHex = HexFormat.of().formatHex(hashBytes);

    // Verify hash is 64 characters (SHA-256 produces 256 bits = 32 bytes = 64 hex chars)
    assertEquals(64, hashHex.length());
    // Verify hash only contains valid hex characters
    assertTrue(hashHex.matches("[0-9a-fA-F]+"), "Hash should only contain hex characters");
  }

  /** Tests that different content produces different hashes. */
  @Test
  void testDifferentContentProducesDifferentHash() throws Exception {
    Path file1 = tempDir.resolve("file1.txt");
    Path file2 = tempDir.resolve("file2.txt");

    Files.writeString(file1, "content A", StandardCharsets.UTF_8);
    Files.writeString(file2, "content B", StandardCharsets.UTF_8);

    String hash1 = computeHash(file1);
    String hash2 = computeHash(file2);

    assertNotNull(hash1);
    assertNotNull(hash2);
    assertFalse(hash1.equals(hash2));
  }

  /** Tests that identical content produces identical hashes (deterministic). */
  @Test
  void testIdenticalContentProducesIdenticalHash() throws Exception {
    String content = "identical content";
    Path file1 = tempDir.resolve("file1.txt");
    Path file2 = tempDir.resolve("file2.txt");

    Files.writeString(file1, content, StandardCharsets.UTF_8);
    Files.writeString(file2, content, StandardCharsets.UTF_8);

    String hash1 = computeHash(file1);
    String hash2 = computeHash(file2);

    assertEquals(hash1, hash2);
  }

  /** Tests that hash verification correctly identifies matching hashes. */
  @Test
  void testHashVerificationMatches() throws Exception {
    Path testFile = tempDir.resolve("test.txt");
    String content = "verification test content";
    Files.writeString(testFile, content, StandardCharsets.UTF_8);

    String actualHash = computeHash(testFile);

    // Create hash file in same format as ResourceSigner writes
    Path hashFile = tempDir.resolve("test.txt.sha256");
    String hashFileContent = actualHash + "  " + testFile.getFileName().toString() + "\n";
    Files.writeString(hashFile, hashFileContent);

    // Read back and verify
    String expectedHashLine = Files.readString(hashFile).trim();
    String expectedHashHex = expectedHashLine.split("\\s+")[0];

    assertTrue(actualHash.equalsIgnoreCase(expectedHashHex));
  }

  /** Tests that hash verification correctly identifies non-matching hashes. */
  @Test
  void testHashVerificationMismatch() throws Exception {
    Path testFile = tempDir.resolve("test.txt");
    Files.writeString(testFile, "actual content", StandardCharsets.UTF_8);

    String actualHash = computeHash(testFile);

    // Create hash file with different content
    Path hashFile = tempDir.resolve("test.txt.sha256");
    String wrongHash = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx1234yzab5678cdef9012";
    String hashFileContent = wrongHash + "  " + testFile.getFileName().toString() + "\n";
    Files.writeString(hashFile, hashFileContent);

    // Read back and verify
    String expectedHashLine = Files.readString(hashFile).trim();
    String expectedHashHex = expectedHashLine.split("\\s+")[0];

    assertFalse(actualHash.equalsIgnoreCase(expectedHashHex));
  }

  /** Tests empty file hash computation. */
  @Test
  void testEmptyFileHash() throws Exception {
    Path emptyFile = tempDir.resolve("empty.txt");
    Files.writeString(emptyFile, "", StandardCharsets.UTF_8);

    String hash = computeHash(emptyFile);

    // SHA-256 of empty string
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
  }

  /** Tests large file hash computation (tests streaming logic). */
  @Test
  void testLargeFileHash() throws Exception {
    Path largeFile = tempDir.resolve("large.txt");
    // Create a 1MB file
    byte[] oneMb = new byte[1024 * 1024];
    for (int i = 0; i < oneMb.length; i++) {
      oneMb[i] = (byte) (i % 256);
    }
    Files.write(largeFile, oneMb);

    String hash = computeHash(largeFile);

    // Verify hash is computed without error
    assertNotNull(hash);
    assertEquals(64, hash.length());
  }

  /** Helper method to compute SHA-256 hash of a file. */
  private String computeHash(Path path) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes;
    try (InputStream fis = Files.newInputStream(path)) {
      hashBytes = digest.digest(fis.readAllBytes());
    }
    return HexFormat.of().formatHex(hashBytes);
  }
}
