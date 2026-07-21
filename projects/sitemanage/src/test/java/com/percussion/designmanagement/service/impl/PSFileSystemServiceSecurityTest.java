package com.percussion.designmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.designmanagement.service.IPSFileSystemService.PSFolderOperationException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for the CWE-22 path-traversal defense in
 * {@link PSFileSystemService} (spec 004 / T043d, PR #1210).
 *
 * <p>These verify that {@code validatePath} rejects every path whose
 * segments contain a traversal marker ({@code .} or {@code ..}) and
 * every path whose resolved canonical location escapes the configured
 * trusted root directory, regardless of which public entry point is
 * called. They also verify that legitimate in-root paths are accepted.
 */
public class PSFileSystemServiceSecurityTest {

  @Test
  public void dotDotSegmentAtAnyDepthIsRejected(@TempDir java.nio.file.Path root) {
    PSFileSystemService svc = new PSFileSystemService(root.toString());

    // Every public entry point must reject the same traversal payload.
    String evil = "themes/site/../../../etc/passwd";
    assertThrows(IllegalArgumentException.class, () -> svc.getChildren(evil));
    assertThrows(IllegalArgumentException.class, () -> svc.getFile(evil));
    assertThrows(IllegalArgumentException.class, () -> svc.addFolder(evil));
    assertThrows(IllegalArgumentException.class, () -> svc.renameFolder(evil, "x"));
    assertThrows(IllegalArgumentException.class, () -> svc.deleteFolder(evil));
    assertThrows(IllegalArgumentException.class, () -> svc.deleteFile(evil));
  }

  @Test
  public void bareDotDotSegmentIsRejected(@TempDir java.nio.file.Path root) {
    PSFileSystemService svc = new PSFileSystemService(root.toString());
    assertThrows(IllegalArgumentException.class, () -> svc.getFile(".."));
    assertThrows(IllegalArgumentException.class, () -> svc.getFile("../etc/passwd"));
    assertThrows(IllegalArgumentException.class, () -> svc.getFile("themes/.."));
  }

  @Test
  public void pathEscapingViaCanonicalizationIsRejected(@TempDir java.nio.file.Path root) {
    PSFileSystemService svc = new PSFileSystemService(root.toString());
    // A path that, after canonicalization, lives entirely outside the
    // configured root must be rejected even though every individual
    // segment is "innocent". On Windows, the parent of @TempDir is a
    // reliable escape target (guaranteed to be outside the root).
    File sibling =
        new File(
            root.getParent().toFile(),
            "ps-filesystem-security-test-" + System.nanoTime() + ".txt");
    sibling.deleteOnExit();
    String escaped = sibling.getAbsolutePath();
    assertThrows(IllegalArgumentException.class, () -> svc.getFile(escaped));
  }

  @Test
  public void legitimateInRootPathIsAccepted(@TempDir Path root) throws Exception {
    PSFileSystemService svc = new PSFileSystemService(root.toString());

    Path target = root.resolve("themes").resolve("site").resolve("page.html");
    Files.createDirectories(target.getParent());
    Files.createFile(target);

    // getFile and getChildren must not throw for an in-root path.
    File resolved = svc.getFile(root.relativize(target).toString());
    assertNotNull(resolved);
    assertTrue(Files.isSameFile(target, resolved.toPath()));

    Path parent = target.getParent();
    List<File> children = svc.getChildren(root.relativize(parent).toString());
    assertNotNull(children);
    assertTrue(Files.exists(parent));
  }

  @Test
  public void renameFolderRejectsBadNewName(@TempDir java.nio.file.Path root) {
    PSFileSystemService svc = new PSFileSystemService(root.toString());
    // renameFolder rejects bad newFolderName values via the existing
    // domain-specific checks: isReservedFilename for `.`/`..` (throws
    // PSInvalidFolderNameException) and containsInvalidChars for path
    // separators and reserved characters (throws
    // PSInvalidCharacterInFolderNameException). Both extend
    // PSFolderOperationException, which is what callers declare.
    assertThrows(PSFolderOperationException.class, () -> svc.renameFolder("themes/site", ".."));
    assertThrows(PSFolderOperationException.class, () -> svc.renameFolder("themes/site", "."));
    assertThrows(PSFolderOperationException.class, () -> svc.renameFolder("themes/site", "a/b"));
  }
}