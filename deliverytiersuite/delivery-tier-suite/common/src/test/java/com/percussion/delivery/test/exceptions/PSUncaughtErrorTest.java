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
package com.percussion.delivery.test.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.exceptions.PSUncaughtError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #6343: the catch-all mapper must log the original failure at ERROR (the cell ships no
 * per-service log file, so a WARN never surfaces) and still redirect to the local error page.
 */
public class PSUncaughtErrorTest {

  private Logger logger;
  private CollectingAppender appender;

  /** Minimal in-memory appender (log4j-core main artifact has no ListAppender). */
  private static final class CollectingAppender extends AbstractAppender {
    private final List<LogEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

    CollectingAppender() {
      super("PSUncaughtErrorTest", null, null, false, null);
    }

    @Override
    public void append(LogEvent event) {
      events.add(event.toImmutable());
    }

    List<LogEvent> events() {
      return events;
    }
  }

  @BeforeEach
  public void attachListAppender() {
    logger = (Logger) LogManager.getLogger(PSUncaughtError.class);
    appender = new CollectingAppender();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  public void detachListAppender() {
    logger.removeAppender(appender);
  }

  private List<LogEvent> errorEvents() {
    return appender.events().stream()
        .filter(event -> event.getLevel().isMoreSpecificThan(Level.WARN))
        .toList();
  }

  @Test
  public void toResponseWithoutContextLogsCauseAtError() {
    PSUncaughtError mapper = new PSUncaughtError();

    assertNull(mapper.toResponse(new IllegalStateException("boom-6343")));

    List<LogEvent> errors = errorEvents();
    assertTrue(!errors.isEmpty(), "expected an ERROR log for the uncaught failure");
    assertTrue(
        errors.stream()
            .anyMatch(
                event ->
                    event.getMessage().getFormattedMessage().contains("boom-6343")),
        "ERROR log must carry the original cause");
  }

  @Test
  public void toResponseRedirectsToLocalErrorPageAndLogs() throws Exception {
    AtomicReference<String> redirect = new AtomicReference<>();
    HttpServletRequest request =
        (HttpServletRequest)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) ->
                    "getContextPath".equals(method.getName()) ? "/ctx" : null);
    HttpServletResponse response =
        (HttpServletResponse)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                  if ("sendRedirect".equals(method.getName())) {
                    redirect.set((String) args[0]);
                    return null;
                  }
                  if ("getStatus".equals(method.getName())) {
                    return 500;
                  }
                  return null;
                });

    PSUncaughtError mapper = new PSUncaughtError();
    injectInto(mapper, "request", request);
    injectInto(mapper, "response", response);

    assertNull(mapper.toResponse(new IllegalStateException("boom-6343")));

    assertEquals("/ctx/error.html", redirect.get());
    assertTrue(
        errorEvents().stream()
            .anyMatch(
                event ->
                    event.getMessage().getFormattedMessage().contains("boom-6343")),
        "ERROR log must carry the original cause alongside the redirect");
  }

  private static void injectInto(PSUncaughtError mapper, String name, Object value)
      throws Exception {
    Field field = PSUncaughtError.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(mapper, value);
  }
}
