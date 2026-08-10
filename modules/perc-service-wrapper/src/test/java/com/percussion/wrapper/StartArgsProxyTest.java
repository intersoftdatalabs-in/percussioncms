/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link StartArgsProxy}: reflective access to Jetty start-args without
 * unchecked casts (issue #2025).
 */
public class StartArgsProxyTest {

  @Test
  @DisplayName("isRun delegates to the wrapped instance")
  void isRunDelegates() {
    StartArgsProxy runProxy = new StartArgsProxy(new FakeStartArgs(true, List.of("a")));
    StartArgsProxy noRunProxy = new StartArgsProxy(new FakeStartArgs(false, List.of("a")));
    assertTrue(runProxy.isRun());
    assertFalse(noRunProxy.isRun());
  }

  @Test
  @DisplayName("getMainArgs returns parameterized String list from reflective payload")
  void getMainArgsReturnsStringList() {
    List<String> expected = Arrays.asList("org.eclipse.jetty.xml.XmlConfiguration", "jetty.xml");
    StartArgsProxy proxy = new StartArgsProxy(new FakeStartArgs(true, expected));
    assertEquals(expected, proxy.getMainArgs());
  }

  @Test
  @DisplayName("getMainArgs returns null when reflective getArgs is not a List")
  void getMainArgsRejectsNonList() {
    StartArgsProxy proxy = new StartArgsProxy(new FakeStartArgsNonList());
    assertNull(proxy.getMainArgs());
  }

  @Test
  @DisplayName("toStringList copies String elements and rejects null or non-String elements")
  void toStringListValidatesElements() {
    assertNull(StartArgsProxy.toStringList(null));
    assertNull(StartArgsProxy.toStringList("not-a-list"));
    assertEquals(Collections.singletonList("x"), StartArgsProxy.toStringList(List.of("x")));
    // Null elements are not String; reject the payload (avoids NPE for non-null List<String>
    // consumers)
    assertNull(StartArgsProxy.toStringList(Arrays.asList("a", null, "b")));
    assertNull(StartArgsProxy.toStringList(List.of("ok", 42)));
  }

  @Test
  @DisplayName("getInstance returns the wrapped object")
  void getInstanceReturnsWrapped() {
    FakeStartArgs fake = new FakeStartArgs(true, List.of());
    assertEquals(fake, new StartArgsProxy(fake).getInstance());
  }

  /** Stand-in for Jetty's opaque start-args object (methods resolved by name). */
  public static final class FakeStartArgs {
    private final boolean run;
    private final List<String> args;

    FakeStartArgs(boolean run, List<String> args) {
      this.run = run;
      this.args = args;
    }

    public boolean isRun() {
      return run;
    }

    public FakeCmdline getMainArgs(boolean includeAll) {
      return new FakeCmdline(args);
    }
  }

  /** Stand-in for Jetty's command-line builder returned by getMainArgs. */
  public static final class FakeCmdline {
    private final List<String> args;

    FakeCmdline(List<String> args) {
      this.args = args;
    }

    public List<String> getArgs() {
      return args;
    }
  }

  /** getMainArgs returns an object whose getArgs is not a List. */
  public static final class FakeStartArgsNonList {
    public boolean isRun() {
      return true;
    }

    public FakeCmdlineNonList getMainArgs(boolean includeAll) {
      return new FakeCmdlineNonList();
    }
  }

  public static final class FakeCmdlineNonList {
    public String getArgs() {
      return "not-a-list";
    }
  }
}
