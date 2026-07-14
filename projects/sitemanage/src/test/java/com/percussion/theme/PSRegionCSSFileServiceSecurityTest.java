package com.percussion.theme;

import com.percussion.theme.data.PSRegionCSS;
import com.percussion.theme.service.impl.PSRegionCSSFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the CWE-22 path-traversal defense in
 * {@link PSRegionCSSFileService} (spec 004 / T043, PR #1209).
 *
 * <p>These verify that {@code requireSafeFilePath} validates the resolved
 * canonical path against the trusted, server-controlled roots injected via
 * {@link PSRegionCSSFileService#setAllowedRoots} (not against an
 * input-derived parent, which would be a tautology that still permits
 * traversal).
 */
public class PSRegionCSSFileServiceSecurityTest {

  @Test
  public void traversalWithDotDotOutsideRootIsRejected(@TempDir java.nio.file.Path root)
      throws Exception {
    PSRegionCSSFileService svc = new PSRegionCSSFileService();
    svc.setAllowedRoots(root.toFile());

    // A payload that escapes the trusted root via ".." must be blocked.
    String evil =
        new File(root.toFile(), "theme/perc/../../../../etc/passwd").getAbsolutePath();

    List<PSRegionCSS> empty = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> svc.write(evil, empty));
  }

  @Test
  public void absolutePathOutsideRootIsRejected(@TempDir java.nio.file.Path root)
      throws Exception {
    PSRegionCSSFileService svc = new PSRegionCSSFileService();
    svc.setAllowedRoots(root.toFile());

    // Reference a path that resolves to a sibling directory of the allowed
    // root, guaranteeing canonical resolution outside the trusted root
    // regardless of platform path semantics (a bare "/etc/passwd" on
    // Windows is treated as relative and ends up under the temp dir, so we
    // must traverse above root explicitly).
    File sibling =
        new File(
            root.getParent().toFile(),
            "ps-region-css-security-test-" + System.nanoTime() + ".txt");
    sibling.deleteOnExit();

    List<PSRegionCSS> empty = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> svc.write(sibling.getAbsolutePath(), empty));
  }

  @Test
  public void pathUnderAllowedRootIsAccepted(@TempDir java.nio.file.Path root) throws Exception {
    PSRegionCSSFileService svc = new PSRegionCSSFileService();
    svc.setAllowedRoots(root.toFile());

    File target = new File(root.toFile(), "theme/perc/perc_region.css");
    target.getParentFile().mkdirs();

    List<PSRegionCSS> empty = new ArrayList<>();
    // Must not throw a traversal exception for a legitimate in-root path.
    svc.write(target.getAbsolutePath(), empty);
    assertTrue(target.exists(), "region CSS file should have been written under the allowed root");
  }
}