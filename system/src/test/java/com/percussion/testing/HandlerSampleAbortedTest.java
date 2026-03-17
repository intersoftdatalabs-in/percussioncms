package com.percussion.testing;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class HandlerSampleAbortedTest {

  @Test
  public void shouldBeAborted() {
    Assumptions.assumeTrue(false, "force abort");
  }
}
