package com.percussion.ai.signing;

import dev.sigstore.KeylessSigner;
import dev.sigstore.bundle.Bundle;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Utility to sign AI resource files using Sigstore keyless signing. */
public class ResourceSigner {
  public ResourceSigner() {
    super();
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: java ResourceSigner <file1> [file2] ...");
      System.exit(1);
    }

    KeylessSigner signer = null;
    try {
      signer = KeylessSigner.builder().build();
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null) {
        System.err.println("WARNING: Signer initialization failed with no message.");
        e.printStackTrace();
        msg = "Unknown error";
      }
      System.err.println("WARNING: Signer initialization failed: " + msg);
      System.err.println(
          "Proceeding with HASH-ONLY integrity check. Authenticity signatures (.sha256.sig) will be"
              + " skipped.");
      System.err.println(
          "To fix this, ensure you have a browser available or valid Sigstore credentials.");
    }

    try {
      List<String> failed = new ArrayList<>();

      for (String arg : args) {
        Path path = Paths.get(arg);
        if (!Files.exists(path)) {
          System.err.println("File not found: " + arg);
          failed.add(arg);
          continue;
        }

        System.out.println("Processing: " + path);
        try {
          // 1. Calculate SHA-256 Hash
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          byte[] hashBytes;
          try (InputStream fis = Files.newInputStream(path)) {
            hashBytes = digest.digest(fis.readAllBytes());
          }
          String hashHex = HexFormat.of().formatHex(hashBytes);
          String hashFileContent = hashHex + "  " + path.getFileName().toString() + "\n";

          Path hashPath = Paths.get(arg + ".sha256");
          Files.writeString(hashPath, hashFileContent);
          System.out.println("Hash written to: " + hashPath);

          // 2. Sign the Hash File (if signer available)
          if (signer != null) {
            try {
              Bundle bundle = signer.signFile(hashPath);
              Path sigPath = Paths.get(arg + ".sha256.sig");
              Files.writeString(sigPath, bundle.toJson());
              System.out.println("Signature (Bundle) written to: " + sigPath);
            } catch (Exception e) {
              System.err.println("Signing failed for " + arg + ": " + e.getMessage());
              failed.add(arg);
            }
          } else {
            System.err.println("Authenticity signature skipped for: " + arg);
          }

        } catch (Exception e) {
          System.err.println("Failed to process " + arg + ": " + e.getMessage());
          failed.add(arg);
        }
      }

      if (!failed.isEmpty()) {
        System.err.println("The following files failed to sign: " + failed);
        System.exit(1);
      }
    } catch (Exception e) {
      System.err.println("Signer initialization failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
