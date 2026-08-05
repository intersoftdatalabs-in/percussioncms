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

import com.percussion.security.error.PSExceptionUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility methods used by the Percussion CMS service wrapper and the Jetty/DTS wrappers for process
 * management, logging, and configuration.
 *
 * <p>This class loads the installation's <code>java.properties</code> at class initialization,
 * provides portable helpers for locating the Percussion installation directory, locating and
 * killing OS processes bound to a given port, formatting debug / info / error messages, and reading
 * additional properties files.
 */
public class JettyStartUtils {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public JettyStartUtils() {
    // utility class - no instance state
  }

  private static final Logger log = LogManager.getLogger(JettyStartUtils.class);

  /** Classpath path of the bundled usage text shown by {@link #outputHelp()}. */
  public static final String USAGE_RESOURCE_PATH = "usage.txt";

  private static final String JAVA_PROPS_PATH = "java.properties";
  private static boolean debugEnabled = false;

  private static PrintStream logOut = System.out;
  private static PrintStream logErr = System.err;

  private static Properties javaProps = new Properties();

  private static String OS = System.getProperty("os.name").toLowerCase();

  /** Load java.properties from root directory */
  static {
    File rxDir = locateRxDir();
    File javaPropsFile = new File(rxDir.getAbsolutePath() + "/" + JAVA_PROPS_PATH);

    if (javaPropsFile.exists() && javaPropsFile.isFile()) {
      try (InputStream is = new FileInputStream(javaPropsFile)) {
        javaProps.load(is);
      } catch (FileNotFoundException fileNotFoundException) {
        error("Could not find resource: %s", fileNotFoundException, JAVA_PROPS_PATH);
      } catch (IOException ioException) {
        error("IOException loading resource: %s", ioException, JAVA_PROPS_PATH);
      }
    } else {
      error("%s was not found in server directory: %s.", JAVA_PROPS_PATH, rxDir);
    }
  }

  // from https://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
  /**
   * Discovers the operating system process id of the Java process listening on the supplied TCP
   * port using <code>wmic</code> on Windows and <code>ps</code>/<code>grep</code>/<code>awk</code>
   * on Linux/macOS.
   *
   * @param port the TCP port the Java process was started with via <code>http.port</code>
   * @return the discovered process id, or <code>-1</code> if no matching process is found or the
   *     lookup fails
   */
  static int getRunningPid(int port) {
    int pid = -1;
    String windowsCmd =
        "wmic process where \"CommandLine like '%http.port="
            + port
            + " %' and name='java.exe'\" get Name,ProcessId";
    String[] linuxCmd = {
      "/bin/sh",
      "-c",
      "ps -ef | grep \"\\http.port=" + port + " \" | grep -v grep | awk '{print $2}'"
    };

    Object command = isWindows() ? windowsCmd : linuxCmd;
    try {
      Process proc = null;
      if (isWindows()) proc = Runtime.getRuntime().exec(windowsCmd);
      else proc = Runtime.getRuntime().exec(linuxCmd);

      // process the response
      String line = "";
      String errorLine = "";
      try (BufferedReader error =
          new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
        try (BufferedReader input =
            new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
          while ((errorLine = error.readLine()) != null) {
            debug("get pid error line=%s", errorLine);
          }
          while ((line = input.readLine()) != null) {
            String idString = line;
            if (idString.startsWith("java.exe")) {
              idString = line.split("java.exe")[1];
            }
            try {
              pid = Integer.parseInt(idString.trim());
              break;
            } catch (NumberFormatException | NullPointerException e) {
              continue;
            }
          }
        }
      }
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return pid;
  }

  /**
   * Forcefully terminates the process with the supplied id using <code>taskkill /F /T</code> on
   * Windows and <code>kill -9</code> on Linux/macOS.
   *
   * @param pid the operating system process id to terminate
   */
  static void killProcess(int pid) {
    System.out.println("Killing process " + pid);

    String cmd = isWindows() ? "taskkill /F /T /PID " + pid : "kill -9 " + pid;

    Process process;
    int exitCode = -1;
    try {
      process = Runtime.getRuntime().exec(cmd);
      exitCode = process.waitFor();
    } catch (IOException e) {
      System.out.println("Error cannot kill process with command :" + cmd);
      System.out.println("exit code " + exitCode);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Loads a {@link Properties} file from disk.
   *
   * @param file the properties file to load, must exist and be readable
   * @return the loaded properties, never <code>null</code>
   */
  static Properties loadProperties(File file) {
    Properties prop = new Properties();
    try (InputStream input = new FileInputStream(file)) {
      prop.load(input);
    } catch (IOException ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
      System.exit(1);
    }
    return prop;
  }

  /**
   * Indicates whether the current operating system appears to be Windows.
   *
   * @return <code>true</code> if the <code>os.name</code> system property contains <code>"win"
   *     </code>; <code>false</code> otherwise
   */
  public static boolean isWindows() {
    return (OS.indexOf("win") >= 0);
  }

  /**
   * Walks up the supplied file's ancestors looking for a directory containing an <code>rxconfig
   * </code> subdirectory, which marks the Percussion installation root.
   *
   * @param f the starting file or directory; may be <code>null</code>, in which case <code>null
   *     </code> is returned
   * @return the first ancestor directory that contains <code>rxconfig</code>, or <code>null</code>
   *     if no such ancestor exists
   */
  static File locateRxDir(File f) {
    if (f == null) return null;
    if (f.isFile()) return locateRxDir(f.getParentFile());
    else if (f.isDirectory()) {
      File configDir = new File(f, "rxconfig");
      if (configDir.exists()) return f;
      else return locateRxDir(f.getParentFile());
    }
    return null;
  }

  /**
   * Locates the Percussion installation root directory using (in order) the <code>rxdeploydir
   * </code> system property, the <code>rxDir</code> system property, and finally the location of
   * this class' own JAR file when the system properties are not set.
   *
   * @return the resolved installation directory, or <code>null</code> if it cannot be determined;
   *     if a <code>URISyntaxException</code> occurs while resolving the JAR location, the JVM exits
   *     with status <code>1</code>
   */
  public static File locateRxDir() {
    String deploydir = System.getProperty("rxdeploydir");

    if (deploydir == null) deploydir = System.getProperty("rxDir");

    if (deploydir != null) return new File(deploydir);

    debug("No rxdeploydir system property set calculating from start jar location");

    try {
      final File f =
          new File(
              PSServiceWrapper.class
                  .getProtectionDomain()
                  .getCodeSource()
                  .getLocation()
                  .toURI()
                  .getPath());
      return locateRxDir(f);
    } catch (URISyntaxException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      System.exit(1);
    }
    return null;
  }

  /**
   * Prints the bundled usage text resource ({@link #USAGE_RESOURCE_PATH}) to standard output.
   *
   * <p>If the resource cannot be found on the classpath, an error message is written to standard
   * error instead.
   */
  public static void outputHelp() {
    try (InputStream is = JettyStartUtils.class.getResourceAsStream(USAGE_RESOURCE_PATH)) {
      byte[] buf = new byte[1024];
      int nr = is.read(buf);
      while (nr != -1) {
        System.out.write(buf, 0, nr);
        nr = is.read(buf);
      }
    } catch (IOException e) {
      error("Cannot find help resource in classpath for %s", e, USAGE_RESOURCE_PATH);
    }
  }

  /**
   * Returns the value of the requested property from the root java.properties file.
   *
   * @param name the property to retrieve from the file.
   * @return the value of the requested property, may be <code>empty</code>, never <code>null</code>
   *     .
   */
  public static String getJavaProperty(String name) {
    return javaProps.getProperty(name, "");
  }

  /**
   * Returns the java.properties file from the system root.
   *
   * @return Server's root java.properties file if it exists, otherwise returns an empty Properties
   *     object.
   */
  public static Properties getJavaProperties() {
    return javaProps == null ? new Properties() : javaProps;
  }

  /**
   * Writes a formatted debug message to {@link #getLogOut()} when debug output is enabled.
   *
   * @param s the message format string, see {@link String#format(String, Object...)}
   * @param args the format arguments; may be omitted if <code>s</code> has no format specifiers
   */
  public static void debug(String s, Object... args) {
    if (debugEnabled) logOut.println(String.format(s, args));
  }

  /**
   * Writes a formatted debug message together with a {@link Throwable} to standard output / log.
   *
   * @param s the message format string, see {@link String#format(String, Object...)}
   * @param t the throwable to log alongside the formatted message; currently unused for output but
   *     logged via log4j
   * @param args the format arguments; may be omitted if <code>s</code> has no format specifiers
   */
  public static void debug(String s, Throwable t, Object... args) {
    if (debugEnabled) {
      logOut.println(String.format(s, args));
      log.error(logErr);
      log.debug(logErr);
    }
  }

  /**
   * Writes a formatted error message to {@link #getLogerr()}.
   *
   * @param s the message format string, see {@link String#format(String, Object...)}
   * @param args the format arguments; may be omitted if <code>s</code> has no format specifiers
   */
  public static void error(String s, Object... args) {
    logErr.println(String.format(s, args));
  }

  /**
   * Writes a formatted error message together with a {@link Throwable} to standard error / log.
   *
   * @param s the message format string, see {@link String#format(String, Object...)}
   * @param t the throwable to log alongside the formatted message; currently unused for output but
   *     logged via log4j
   * @param args the format arguments; may be omitted if <code>s</code> has no format specifiers
   */
  public static void error(String s, Throwable t, Object... args) {
    logErr.println(String.format(s, args));
    log.error(logErr);
    log.debug(logErr);
  }

  /**
   * Writes a formatted informational message to {@link #getLogOut()}.
   *
   * @param s the message format string, see {@link String#format(String, Object...)}
   * @param args the format arguments; may be omitted if <code>s</code> has no format specifiers
   */
  public static void info(String s, Object... args) {
    logOut.println(String.format(s, args));
  }

  /**
   * Indicates whether debug output is currently enabled.
   *
   * @return <code>true</code> if debug logging has been enabled via {@link
   *     #setDebugEnabled(boolean)}, <code>false</code> otherwise
   */
  public static boolean isDebugEnabled() {
    return debugEnabled;
  }

  /**
   * Enables or disables debug output globally for the service wrapper.
   *
   * @param debugEnabled <code>true</code> to enable debug output, <code>false</code> to disable
   */
  public static void setDebugEnabled(boolean debugEnabled) {
    JettyStartUtils.debugEnabled = debugEnabled;
  }

  /**
   * Returns the {@link PrintStream} used for debug and info messages.
   *
   * @return the current standard-out stream, never <code>null</code>
   */
  public static PrintStream getLogOut() {
    return logOut;
  }

  /**
   * Sets the {@link PrintStream} used for debug and info messages.
   *
   * @param logOut the new standard-out stream, must not be <code>null</code>
   */
  public static void setLogOut(PrintStream logOut) {
    JettyStartUtils.logOut = logOut;
  }

  /**
   * Returns the {@link PrintStream} used for error messages.
   *
   * @return the current standard-error stream, never <code>null</code>
   */
  public static PrintStream getLogerr() {
    return logErr;
  }

  /**
   * Sets the {@link PrintStream} used for error messages.
   *
   * @param logerr the new standard-error stream, must not be <code>null</code>
   */
  public static void setLogerr(PrintStream logerr) {
    JettyStartUtils.logErr = logerr;
  }
}
