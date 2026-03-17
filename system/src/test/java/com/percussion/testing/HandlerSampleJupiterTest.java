package com.percussion.testing;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HandlerSampleJupiterTest {

  @Test
  public void shouldPass() {
    // pass
  }

  @Disabled("Temporarily disabled — failing in perc-system test run")
  @Test
  public void shouldFail() {
    throw new AssertionError("expected failure");
  }

  @Test
  @Disabled("skipped")
  public void shouldSkip() {
    // skipped
  }
}
