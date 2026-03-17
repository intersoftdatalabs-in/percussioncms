package com.percussion.ai.skill;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI tool for AI agents to validate the integrity of resources. This tool is the backend for the
 * 'agent-integrity-validator' skill.
 */
public class IntegrityValidatorTool {

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("{\"verified\": false, \"error\": \"No files provided for validation\"}");
      System.exit(1);
    }

    SkillVerificationService verifier = new SkillVerificationService();
    List<String> verifiedFiles = new ArrayList<>();
    List<String> failedFiles = new ArrayList<>();

    for (String arg : args) {
      Path path = Paths.get(arg);
      if (verifier.verify(path)) {
        verifiedFiles.add(arg);
      } else {
        failedFiles.add(arg);
      }
    }

    boolean allVerified = failedFiles.isEmpty();

    // Return a lean JSON response for the agent
    System.out.print("{");
    System.out.print("\"verified\": " + allVerified);
    System.out.print(", \"checked_resources\": " + formatList(args));
    if (!allVerified) {
      System.out.print(", \"failed_resources\": " + formatList(failedFiles.toArray(new String[0])));
    }
    System.out.println("}");

    if (!allVerified) {
      System.exit(1);
    }
  }

  private static String formatList(String[] items) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < items.length; i++) {
      sb.append("\"").append(items[i]).append("\"");
      if (i < items.length - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
