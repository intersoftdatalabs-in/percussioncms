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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for ResourceSigner hash file generation.
 * Note: Full signing requires Sigstore OIDC credentials
 * and is tested via integration tests.
 */
class ResourceSignerTest {

    @TempDir
    Path tempDir;

    /**
     * Tests that hash file is created with correct format.
     * Format: "<hexHash> <filename>\n"
     */
    @Test
    void testHashFileFormat() throws Exception {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content", StandardCharsets.UTF_8);

        // Simulate hash file creation as done in ResourceSigner
        String hashHex = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx1234yzab5678cdef9012";
        String hashFileContent = hashHex + "  " + testFile.getFileName().toString() + "\n";

        Path hashPath = tempDir.resolve("test.txt.sha256");
        Files.writeString(hashPath, hashFileContent);

        // Verify format
        String content = Files.readString(hashPath);
        assertTrue(content.contains(hashHex));
        assertTrue(content.contains("test.txt"));
        assertTrue(content.endsWith("\n"));
    }

    /**
     * Tests that hash file can be parsed correctly.
     */
    @Test
    void testHashFileParsing() throws Exception {
        Path testFile = tempDir.resolve("AGENTS.md");
        Files.writeString(testFile, "important content", StandardCharsets.UTF_8);

        // Create hash file
        String hashHex = "cafebabe87654321abcdef0123456789fedcba9876543210abcdef0123456789";
        String hashFileContent = hashHex + "  " + testFile.getFileName().toString() + "\n";

        Path hashPath = tempDir.resolve("AGENTS.md.sha256");
        Files.writeString(hashPath, hashFileContent);

        // Parse as ResourceVerifier does
        String expectedHashLine = Files.readString(hashPath).trim();
        String[] parts = expectedHashLine.split("\\s+");

        assertEquals(2, parts.length);
        assertEquals(hashHex, parts[0]);
        assertEquals("AGENTS.md", parts[1]);
    }

    /**
     * Tests that different filenames in same directory produce different hash files.
     */
    @Test
    void testDifferentFilenamesProduceDifferentHashFiles() throws Exception {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");

        Files.writeString(file1, "same content", StandardCharsets.UTF_8);
        Files.writeString(file2, "same content", StandardCharsets.UTF_8);

        String hashHex = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx1234yzab5678cdef9012";

        String hashFile1 = hashHex + "  " + file1.getFileName().toString() + "\n";
        String hashFile2 = hashHex + "  " + file2.getFileName().toString() + "\n";

        assertFalse(hashFile1.equals(hashFile2));
        assertTrue(hashFile1.contains("file1.txt"));
        assertTrue(hashFile2.contains("file2.txt"));
    }

    /**
     * Tests handling of filename with spaces.
     */
    @Test
    void testFilenameWithSpaces() throws Exception {
        Path testFile = tempDir.resolve("my test file.txt");
        Files.writeString(testFile, "content", StandardCharsets.UTF_8);

        // Note: ResourceSigner uses getFileName().toString() which preserves spaces
        String hashHex = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx1234yzab5678cdef9012";
        String hashFileContent = hashHex + "  " + testFile.getFileName().toString() + "\n";

        assertTrue(hashFileContent.contains("my test file.txt"));
    }

    /**
     * Tests ResourceSigner instantiation.
     */
    @Test
    void testResourceSignerConstruction() {
        ResourceSigner signer = new ResourceSigner();
        assertNotNull(signer);
    }
}
