package com.percussion.ai.signing;

import dev.sigstore.KeylessVerifier;
import dev.sigstore.VerificationOptions;
import dev.sigstore.bundle.Bundle;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Utility to verify AI resource files using Sigstore.
 */
public class ResourceVerifier {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java ResourceVerifier <file1> [file2] ...");
            System.exit(1);
        }

        try {
            KeylessVerifier verifier = new KeylessVerifier.Builder().build();
            List<String> failed = new ArrayList<>();

            for (String arg : args) {
                Path path = Paths.get(arg);
                Path hashPath = Paths.get(arg + ".sha256");
                Path sigPath = Paths.get(arg + ".sha256.sig");

                if (!Files.exists(path)) {
                    System.err.println("File not found: " + arg);
                    failed.add(arg);
                    continue;
                }

                if (!Files.exists(hashPath)) {
                    System.err.println("Hash sidecar missing for: " + arg);
                    failed.add(arg);
                    continue;
                }

                if (!Files.exists(sigPath)) {
                    System.err.println("Signature sidecar missing for: " + arg);
                    failed.add(arg);
                    continue;
                }

                System.out.println("Verifying: " + path);
                try {
                    // 1. Verify Integrity (Hash Check)
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] actualHashBytes;
                    try (InputStream fis = Files.newInputStream(path)) {
                        actualHashBytes = digest.digest(fis.readAllBytes());
                    }
                    String actualHashHex = HexFormat.of().formatHex(actualHashBytes);
                    
                    String expectedHashLine = Files.readString(hashPath).trim();
                    String expectedHashHex = expectedHashLine.split("\\s+")[0];
                    
                    if (!actualHashHex.equalsIgnoreCase(expectedHashHex)) {
                        throw new RuntimeException("Integrity check FAILED (hash mismatch)");
                    }
                    System.out.println("Integrity check PASSED for: " + path);

                    // 2. Verify Authenticity (Signature Check)
                    Bundle bundle = Bundle.from(Files.newBufferedReader(sigPath));
                    // Verifies the hash file against its bundle
                    verifier.verify(hashPath, bundle, VerificationOptions.builder().build());
                    System.out.println("Authenticity check PASSED for: " + path);
                    
                    System.out.println("Verification successful for: " + path);
                } catch (Exception e) {
                    System.err.println("Verification FAILED for " + arg + ": " + e.getMessage());
                    failed.add(arg);
                }
            }

            if (!failed.isEmpty()) {
                System.err.println("The following files failed verification: " + failed);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Verifier initialization failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
