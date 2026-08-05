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

import static com.percussion.wrapper.JettyStartUtils.debug;
import static com.percussion.wrapper.JettyStartUtils.info;
import static org.eclipse.jetty.start.StartLog.error;

import com.percussion.security.error.PSExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reflection-based proxy around the Jetty <code>org.eclipse.jetty.start.Main</code> class.
 *
 * <p>Loads the supplied Jetty <code>start.jar</code> into a dedicated {@link URLClassLoader} so
 * that Jetty can be started in-process from the Percussion CMS service wrapper without forking a
 * new JVM. Methods on this proxy use reflection so that the wrapper does not take a hard
 * compile-time dependency on the internal Jetty start API.
 */
public class MainProxy {

  private static final Logger log = LogManager.getLogger(MainProxy.class);

  private static final String JETTY_START_MAIN_CLASS = "org.eclipse.jetty.start.Main";

  private Object main = null;

  /**
   * Loads the Jetty start class from <code>startJar</code> via a dedicated {@link URLClassLoader}
   * and stores a newly constructed instance for later reflective invocation.
   *
   * @param startJar the Jetty <code>start.jar</code> file, must exist and be readable
   */
  public MainProxy(File startJar) {
    URLClassLoader child = null;
    try {
      child =
          new URLClassLoader(
              new URL[] {startJar.toURI().toURL()}, MainProxy.class.getClassLoader());
      Thread.currentThread().setContextClassLoader(child);

      @SuppressWarnings({"rawtypes", "unchecked"})
      Class<?> cls = Class.forName(JETTY_START_MAIN_CLASS, true, child);
      Constructor<?> constructor = cls.getConstructor();
      main = constructor.newInstance();
    } catch (MalformedURLException
        | ClassNotFoundException
        | NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Invokes Jetty's <code>processCommandLine</code> method via reflection and wraps the returned
   * opaque configuration object in a {@link StartArgsProxy}.
   *
   * @param args the command-line arguments to forward to Jetty, never <code>null</code>
   * @return a {@link StartArgsProxy} wrapping the Jetty result, or <code>null</code> if the
   *     reflective invocation fails for any reason
   */
  public StartArgsProxy processCommandLine(String[] args) {

    try {
      Method proceesCommand = main.getClass().getMethod("processCommandLine", args.getClass());
      return new StartArgsProxy(proceesCommand.invoke(main, new Object[] {args}));
    } catch (IllegalAccessException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (InvocationTargetException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (NoSuchMethodException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return null;
  }

  /**
   * Invokes Jetty's <code>start</code> method via reflection using the supplied wrapped start
   * arguments.
   *
   * @param startArgs the proxy wrapping Jetty's start-args configuration object, must not be <code>
   *     null</code>
   */
  public void start(StartArgsProxy startArgs) {
    try {
      Method startCommand = main.getClass().getMethod("start", startArgs.getInstance().getClass());
      startCommand.invoke(main, new Object[] {startArgs.getInstance()});
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Sends a Jetty-style stop command to the loopback address on <code>port</code> using <code>key
   * </code>, and waits up to <code>timeout</code> seconds for Jetty to report <code>"Stopped"
   * </code>.
   *
   * @param port the loopback TCP port on which Jetty is listening for stop commands
   * @param key the stop key expected by Jetty, appended with <code>"\r\nstop\r\n"</code>
   * @param timeout the maximum number of seconds to wait for the stop response; a non-positive
   *     value disables the wait
   * @return <code>true</code> if Jetty reported <code>"Stopped"</code> within the timeout, <code>
   *     false</code> otherwise
   */
  public boolean stop(int port, String key, int timeout) {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2000);
      if (timeout > 0) {
        s.setSoTimeout(timeout * 1000);
      }

      try (OutputStream out = s.getOutputStream()) {
        out.write((key + "\r\nstop\r\n").getBytes());
        out.flush();

        if (timeout > 0) {
          info("Waiting %,d seconds for jetty to stop%n", timeout);
          try (InputStream io = s.getInputStream()) {
            try (InputStreamReader isr = new InputStreamReader(io)) {
              try (LineNumberReader lin = new LineNumberReader(isr)) {
                String response;
                while ((response = lin.readLine()) != null) {
                  debug("Received \"%s\"", response);
                  if ("Stopped".equals(response)) {
                    debug(String.format("Server reports itself as Stopped"));
                    return true;
                  }
                }
              }
            }
          }
        }
      } catch (SocketTimeoutException e) {
        error("Timeout on connection to shutdown port");
      } catch (IOException e) {
        error("Error sending stop to jetty shutdown port", e);
      }
    } catch (SocketTimeoutException e) {
      error("Timeout on connection to shutdown port", e);
    } catch (SocketException e) {
      error("Timeout on connection to shutdown port", e);
    } catch (UnknownHostException e) {
      error("Error sending stop to jetty shutdown port", e);
    } catch (IOException e) {
      error("Error sending stop to jetty shutdown port", e);
    }
    return false;
  }
}
