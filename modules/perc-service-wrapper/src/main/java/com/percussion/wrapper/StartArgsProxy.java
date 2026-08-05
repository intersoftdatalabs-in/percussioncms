/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import static com.percussion.wrapper.JettyStartUtils.error;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Reflection-based proxy around the Jetty <code>org.eclipse.jetty.start.Main</code> class' start
 * arguments result.
 *
 * <p>Jetty's <code>processCommandLine</code> method returns an opaque configuration object that is
 * referenced reflectively to avoid a hard compile-time dependency on the internal Jetty start API.
 * This proxy exposes the small subset of fields that the Percussion CMS wrapper needs to consume
 * when starting Jetty in-process.
 */
public class StartArgsProxy {

  private Object instance;

  /**
   * Constructs a proxy wrapping the supplied Jetty start-args instance.
   *
   * @param startArgsInstance the opaque object returned by Jetty's <code>processCommandLine</code>
   *     method, never <code>null</code>
   */
  public StartArgsProxy(Object startArgsInstance) {
    instance = startArgsInstance;
  }

  /**
   * Indicates whether Jetty determined it should run in foreground (versus starting in the
   * background and exiting).
   *
   * @return <code>true</code> if Jetty should run in the foreground, <code>false</code> otherwise;
   *     returns <code>false</code> if the underlying Jetty method could not be invoked
   */
  public boolean isRun() {
    try {
      Method isRunMethod = instance.getClass().getMethod("isRun");
      return (Boolean) isRunMethod.invoke(instance);
    } catch (IllegalAccessException e) {
      error("Error accessing jetty method", e);
    } catch (InvocationTargetException e) {
      error("Error accessing jetty method", e);
    } catch (NoSuchMethodException e) {
      error("Error accessing jetty method", e);
    }
    return false;
  }

  /**
   * Returns the main command-line arguments Jetty will execute when started in foreground mode.
   *
   * @return the list of command-line arguments assembled by Jetty, or <code>null</code> if the
   *     underlying Jetty method could not be invoked reflectively
   */
  public List<String> getMainArgs() {
    try {
      Method getMainArgs = instance.getClass().getMethod("getMainArgs", boolean.class);
      Object cmdlineBuild = getMainArgs.invoke(instance, new Object[] {true});
      Method getArgsMethod = cmdlineBuild.getClass().getMethod("getArgs");
      return (List<String>) getArgsMethod.invoke(cmdlineBuild);
    } catch (IllegalAccessException e) {
      error("Error accessing jetty method", e);
    } catch (InvocationTargetException e) {
      error("Error accessing jetty method", e);
    } catch (NoSuchMethodException e) {
      error("Error accessing jetty method", e);
    }
    return null;
  }

  /**
   * Returns the opaque Jetty start-args instance being proxied.
   *
   * @return the wrapped Jetty start-args instance, may be <code>null</code> if the proxy was
   *     constructed with a <code>null</code> argument
   */
  public Object getInstance() {
    return instance;
  }
}
