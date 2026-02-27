package com.percussion.install;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link PSLogger}. Primarily exercises static initialization to ensure loading the
 * class does not blow up (see RHYTHMYX-XYZ bug report).
 */
public class TestPSLogger {

  @Test
  public void testClassLoadsAndLogging() {
    // simply invoking a method will cause the class to be loaded and the
    // static logger initialized.  Prior implementation used
    // LogManager.getLogger() which can throw
    // UnsupportedOperationException when the caller cannot be determined.
    PSLogger.logInfo("test message");
    // also try calling init with a temp directory (should not throw)
    String tmp = System.getProperty("java.io.tmpdir");
    PSLogger.init(tmp);
    // no exception means success; nothing to assert beyond that
    assertTrue("Temporary directory should exist", tmp != null && tmp.length() > 0);
  }
}
