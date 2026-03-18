package com.percussion.packages;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;

/**
 * Verifies that PSPackageBuilder produces .ppkg files with the correct zip structure by comparing
 * against reference packages built by the original ant-based tool. Structure (entry names) must be
 * identical. Content is compared for binary files; XML and script files may legitimately differ
 * between releases.
 */
public class PSPackageBuilderTest {

  /** Relative path from module basedir to the system Packages directory. */
  private static final String SOURCE_DIR = "../../system/Packages";

  /** Relative path to reference packages built by the previous release. */
  private static final String REFERENCE_DIR = "src/test/resources/Packages/Percussion";

  /** Temp output directory for generated packages. */
  private static final String OUTPUT_DIR = "target/test-packages";

  /**
   * For each reference .ppkg in the test resources, builds the corresponding package from source
   * and compares zip entry names (must be identical) and content (must be identical for binary
   * files; XML/JS/properties files are allowed to differ between versions).
   */
  @Test
  public void testAllPackagesMatchReferenceStructure() throws IOException {
    File referenceDir = new File(REFERENCE_DIR);
    assertTrue(
        "Reference package directory not found: " + referenceDir.getAbsolutePath(),
        referenceDir.exists());

    File outputDir = new File(OUTPUT_DIR);
    outputDir.mkdirs();

    File sourceDir = new File(SOURCE_DIR);
    assertTrue(
        "Source package directory not found: " + sourceDir.getAbsolutePath(), sourceDir.exists());

    File[] referencePkgs = referenceDir.listFiles(f -> f.getName().endsWith(".ppkg") && f.isFile());
    assertNotNull("No reference packages found", referencePkgs);
    assertTrue("Expected at least one reference package", referencePkgs.length > 0);

    Arrays.sort(referencePkgs);

    List<String> failures = new ArrayList<>();

    for (File refPkg : referencePkgs) {
      String pkgName = refPkg.getName().replace(".ppkg", "");
      File pkgSourceDir = new File(sourceDir, pkgName);

      if (!pkgSourceDir.exists()) {
        System.out.println("SKIP (no source): " + pkgName);
        continue;
      }

      File outputPkg = new File(outputDir, pkgName + ".ppkg");
      try {
        PSPackageBuilder.buildPackage(pkgSourceDir, pkgName, outputPkg);
        comparePackage(pkgName, outputPkg, refPkg, failures);
      } catch (Exception e) {
        failures.add(pkgName + ": exception during build/compare: " + e.getMessage());
      }
    }

    if (!failures.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append(failures.size()).append(" package(s) failed:\n");
      for (String f : failures) {
        sb.append("  ").append(f).append("\n");
      }
      fail(sb.toString());
    }
  }

  private void comparePackage(String pkgName, File generated, File reference, List<String> failures)
      throws IOException {
    Map<String, byte[]> genEntries = readZipEntries(generated);
    Map<String, byte[]> refEntries = readZipEntries(reference);

    Set<String> genNames = new TreeSet<>(genEntries.keySet());
    Set<String> refNames = new TreeSet<>(refEntries.keySet());

    Set<String> missing = new HashSet<>(refNames);
    missing.removeAll(genNames);

    Set<String> extra = new HashSet<>(genNames);
    extra.removeAll(refNames);

    if (!missing.isEmpty()) {
      failures.add(pkgName + ": missing entries: " + new TreeSet<>(missing));
    }
    if (!extra.isEmpty()) {
      failures.add(pkgName + ": unexpected extra entries: " + new TreeSet<>(extra));
    }

    // Compare content for binary entries (when structure matches)
    if (missing.isEmpty() && extra.isEmpty()) {
      List<String> contentDiff = new ArrayList<>();
      for (String entry : genNames) {
        if (isChangeableFile(entry)) {
          continue;
        }
        byte[] genBytes = genEntries.get(entry);
        byte[] refBytes = refEntries.get(entry);
        if (!Arrays.equals(genBytes, refBytes)) {
          contentDiff.add(entry);
        }
      }
      if (!contentDiff.isEmpty()) {
        failures.add(pkgName + ": content differs in: " + contentDiff);
      } else {
        System.out.println("OK: " + pkgName);
      }
    }
  }

  /**
   * Returns true for files whose content may legitimately differ between release versions (version
   * numbers, updated widget code, etc.).
   */
  private boolean isChangeableFile(String entryName) {
    String lower = entryName.toLowerCase();
    return lower.endsWith(".xml")
        || lower.endsWith(".js")
        || lower.endsWith(".properties")
        || lower.endsWith(".html")
        || lower.endsWith(".htm")
        || lower.endsWith(".css")
        || lower.endsWith(".txt")
        || lower.endsWith(".md");
  }

  /** Reads all entries (files and directories) from a zip file into a name-to-content map. */
  private Map<String, byte[]> readZipEntries(File zipFile) throws IOException {
    Map<String, byte[]> entries = new HashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = zis.read(buf)) > 0) {
          baos.write(buf, 0, len);
        }
        entries.put(entry.getName(), baos.toByteArray());
        zis.closeEntry();
      }
    }
    return entries;
  }
}
