package com.percussion.ai.skill;

import dev.sigstore.KeylessVerifier;
import dev.sigstore.VerificationOptions;
import dev.sigstore.bundle.Bundle;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service to verify the integrity and authenticity of AI resources (skills, prompts, instructions).
 * Uses Sigstore for keyless verification.
 */
public class SkillVerificationService {

    private static final Logger log = LogManager.getLogger(SkillVerificationService.class);
    private final KeylessVerifier verifier;
    
    // Cache for verification status: Path -> Last verified SHA-256 hash
    private final ConcurrentHashMap<Path, String> verifiedCache = new ConcurrentHashMap<>();

    public SkillVerificationService() {
        try {
            this.verifier = new KeylessVerifier.Builder().build();
        } catch (Exception e) {
            log.error("Failed to initialize Sigstore KeylessVerifier", e);
            throw new RuntimeException("Sigstore initialization failed", e);
        }
    }

    /**
     * Verifies the signature of a file using the sidecar strategy.
     * Uses an internal cache to avoid re-verifying unchanged files.
     *
     * @param contentPath Path to the file content.
     * @return true if verification passes, false otherwise.
     */
    public boolean verify(Path contentPath) {
        if (!Files.exists(contentPath)) {
            log.warn("Content file not found: {}", contentPath);
            return false;
        }

        try {
            // 1. Calculate current SHA-256 Hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actualHashBytes;
            try (InputStream fis = Files.newInputStream(contentPath)) {
                actualHashBytes = digest.digest(fis.readAllBytes());
            }
            String actualHashHex = HexFormat.of().formatHex(actualHashBytes);

            // 2. Check Cache
            String cachedHash = verifiedCache.get(contentPath);
            if (actualHashHex.equals(cachedHash)) {
                log.debug("Cache hit: Verification skipped for {}", contentPath);
                return true;
            }

            // 3. Perform Full Sidecar Verification
            Path hashPath = Paths.get(contentPath.toString() + ".sha256");
            Path sigPath = Paths.get(contentPath.toString() + ".sha256.sig");

            if (!Files.exists(hashPath) || !Files.exists(sigPath)) {
                log.warn("Sidecar files missing for: {}", contentPath);
                return false;
            }

            // A. Verify Integrity (Hash Check)
            String expectedHashLine = Files.readString(hashPath).trim();
            String expectedHashHex = expectedHashLine.split("\\s+")[0];
            
            if (!actualHashHex.equalsIgnoreCase(expectedHashHex)) {
                log.error("Integrity check FAILED (hash mismatch) for: {}", contentPath);
                return false;
            }

            // B. Verify Authenticity (Signature Check)
            Bundle bundle = Bundle.from(Files.newBufferedReader(sigPath));
            verifier.verify(hashPath, bundle, VerificationOptions.builder().build());
            
            log.info("Successfully verified sidecar signature for: {}", contentPath);
            
            // 4. Update Cache
            verifiedCache.put(contentPath, actualHashHex);
            return true;
        } catch (Exception e) {
            log.error("Signature verification failed for: {}. Error: {}", contentPath, e.getMessage());
            return false;
        }
    }

    /**
     * Legacy verify method for direct signature path passing.
     * @deprecated Use verify(Path contentPath) with sidecar strategy.
     */
    @Deprecated
    public boolean verify(Path contentPath, Path signaturePath) {
        return verify(contentPath);
    }
}
