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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.wrapper;

import static com.percussion.wrapper.JettyStartUtils.debug;
import static com.percussion.wrapper.JettyStartUtils.error;
import static com.percussion.wrapper.JettyStartUtils.getRunningPid;
import static com.percussion.wrapper.JettyStartUtils.info;

import com.percussion.security.error.PSExceptionUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for wrappers that start, stop, and monitor an external service process (for example
 * the embedded Jetty server or a Tomcat-based DTS instance).
 *
 * <p>Concrete subclasses ({@link JettyStartWrapper}, {@link DtsStartWrapper}) populate the wrapper
 * with the start command, ports, and shutdown key specific to the wrapped service. {@code
 * StartWrapper} then provides:
 *
 * <ul>
 *   <li>Process id discovery via {@link JettyStartUtils#getRunningPid(int)};
 *   <li>State persistence to a per-service state file with a watcher that reloads state when the
 *       file changes;
 *   <li>Graceful shutdown via a TCP stop command to the configured shutdown port and key, with an
 *       optional forced kill fallback;
 *   <li>A shutdown hook that reloads state and stops the service when the JVM exits.
 * </ul>
 *
 * <p>This class is thread-safe: mutations of the {@link #state} field are guarded by an internal
 * monitor so that {@link #waitForStart()} and the state watcher thread observe a consistent value.
 */
public abstract class StartWrapper {

  private static final Logger log = LogManager.getLogger(StartWrapper.class);

  /** Local filesystem path separator, equivalent to {@link File#separator}. */
  protected static final String FS = File.separator;

  /** Local classpath separator, equivalent to {@link java.io.File#pathSeparator}. */
  protected static final String CPS = System.getProperty("path.separator");

  /** The installation root directory the wrapped service is launched from. */
  protected final File rootDir;

  /** Monitor guarding reads and writes of the {@link #state} field. */
  private final Object stateMonitor = new Object();

  /** Human-readable label used in log output, must not be {@code null}. */
  protected String name;

  /** Raw command-line arguments the wrapper was constructed with. */
  protected String[] args;

  /** Whether this wrapper represents an installed and launchable service. */
  protected boolean active;

  /** Whether the wrapped service runs in the foreground (versus backgrounding and exiting). */
  protected boolean isRun = true;

  /** Optional substring matched against the wrapped process' log output to detect startup. */
  protected String startupCheckString = null;

  /** TCP port the wrapped service listens on for shutdown commands. */
  protected int shutdownPort;

  /** Stop key expected by the wrapped service's shutdown port. */
  protected String stopKey = "SHUTDOWN";

  /** Cached process id of the running wrapped service, or {@code 0} when not running. */
  protected int pid;

  /** HTTP port the wrapped service binds to. */
  protected int port;

  /** Maximum number of seconds to wait for the wrapped service to report started. */
  protected int startTimeout = 240;

  /** Maximum number of seconds to wait for a graceful stop response from the wrapped service. */
  protected int stopTimeout;

  /** Suffix appended to the shutdown key when sending the stop command (e.g. Jetty's CR/LF). */
  protected String stopKeySuffix = "";

  /** Response line the wrapped service emits once it has stopped. */
  protected String stopResponse = null;

  /** File the wrapper reads and writes the current {@link ProcState} to. */
  protected File stateFile;

  /** Working directory the wrapped service is launched with. */
  protected File currentDirectory;

  /** Whether state changes should be persisted to {@link #stateFile}. */
  protected boolean writeToStateFile = true;

  /** Current lifecycle state of the wrapped service. */
  protected ProcState state = ProcState.STOPPED;

  /** Background watcher that re-reads {@link #stateFile} when it changes on disk. */
  private StateFileWatcher stateFileWatcher = null;

  /** Cached command line that is used to launch the wrapped service. */
  private String[] startCmd;

  /**
   * Constructs a wrapper with the supplied label, installation root, and command-line arguments.
   *
   * @param name a human-readable label used in log output, never {@code null}
   * @param rootDir the installation root directory, never {@code null}
   * @param args raw command-line arguments forwarded to the wrapped service; may be {@code null}
   */
  public StartWrapper(String name, File rootDir, String[] args) {
    this.name = name;
    this.rootDir = rootDir;
    this.currentDirectory = rootDir;
    this.args = args;
  }

  /**
   * Returns the working directory the wrapped service will be launched from.
   *
   * @return the current directory, never {@code null}
   */
  public File getCurrentDirectory() {
    return currentDirectory;
  }

  /**
   * Sets the working directory the wrapped service will be launched from.
   *
   * @param currentDirectory the new current directory, must not be {@code null}
   */
  public void setCurrentDirectory(File currentDirectory) {
    this.currentDirectory = currentDirectory;
  }

  /**
   * Returns the installation root directory the wrapper was constructed with.
   *
   * @return the root directory, never {@code null}
   */
  public File getRootDir() {
    return rootDir;
  }

  /**
   * Returns the human-readable label used in log output.
   *
   * @return the wrapper name, never {@code null}
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the raw command-line arguments the wrapper was constructed with.
   *
   * @return the argument array; may be {@code null} if no arguments were supplied
   */
  public String[] getArgs() {
    return args;
  }

  /**
   * Overrides the command-line arguments that will be forwarded to the wrapped service.
   *
   * @param args the new argument array; may be {@code null}
   */
  public void setArgs(String[] args) {
    this.args = args;
  }

  /**
   * Indicates whether the wrapped service is installed and can be started.
   *
   * @return {@code true} if the wrapper is active, {@code false} otherwise
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Marks the wrapper as active or inactive. When transitioning to inactive the current state is
   * reset to {@link ProcState#NOT_INSTALLED}.
   *
   * @param active {@code true} to mark the wrapped service as installed and launchable, {@code
   *     false} otherwise
   */
  public void setActive(boolean active) {
    if (active == false) state = ProcState.NOT_INSTALLED;
    this.active = active;
  }

  /**
   * Returns the TCP port the wrapped service listens on for shutdown commands.
   *
   * @return the shutdown port number
   */
  public int getShutdownPort() {
    return shutdownPort;
  }

  /**
   * Sets the TCP port the wrapped service listens on for shutdown commands.
   *
   * @param shutdownPort the new shutdown port number
   */
  public void setShutdownPort(int shutdownPort) {
    this.shutdownPort = shutdownPort;
  }

  /**
   * Indicates whether the wrapped service runs in the foreground.
   *
   * @return {@code true} if the service runs in the foreground, {@code false} otherwise
   */
  public boolean isRun() {
    return isRun;
  }

  /**
   * Returns the cached process id of the running wrapped service.
   *
   * @return the process id, or {@code 0} when the service is not running
   */
  public int getPid() {
    return pid;
  }

  /**
   * Overrides the cached process id used for management operations.
   *
   * @param pid the new process id; pass {@code 0} to indicate "not running"
   */
  public void setPid(int pid) {
    this.pid = pid;
  }

  /**
   * Returns the HTTP port the wrapped service binds to.
   *
   * @return the HTTP port number
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the HTTP port the wrapped service binds to.
   *
   * @param port the new HTTP port number
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Discovers the process id currently bound to {@link #port} via {@link
   * JettyStartUtils#getRunningPid(int)} and caches it for later retrieval via {@link #getPid()}.
   *
   * @return the discovered process id, or {@code -1} when no matching process could be found
   */
  public int updatePid() {
    pid = getRunningPid(port);
    return pid;
  }

  /**
   * Returns the cached command line used to launch the wrapped service.
   *
   * @return the start command array; may be {@code null} if the wrapper is not active
   */
  public String[] getStartCmd() {
    return startCmd;
  }

  /**
   * Sets the command line used to launch the wrapped service.
   *
   * @param startCmd the new start command array; may be {@code null}
   */
  public void setStartCmd(String[] startCmd) {
    this.startCmd = startCmd;
  }

  /**
   * Returns the suffix appended to the shutdown key when sending the stop command.
   *
   * @return the suffix, never {@code null}
   */
  public String getStopKeySuffix() {
    return stopKeySuffix;
  }

  /**
   * Sets the suffix appended to the shutdown key when sending the stop command.
   *
   * @param stopKeySuffix the new suffix; may be {@code null} in which case an empty string is
   *     stored
   */
  public void setStopKeySuffix(String stopKeySuffix) {
    this.stopKeySuffix = stopKeySuffix;
  }

  /**
   * Returns the substring matched against the wrapped process' log output to detect successful
   * startup.
   *
   * @return the startup check string, or {@code null} when no check is performed
   */
  public String getStartupCheckString() {
    return startupCheckString;
  }

  /**
   * Sets the substring matched against the wrapped process' log output to detect successful
   * startup.
   *
   * @param startupCheckString the new startup check string; may be {@code null} to disable the
   *     check
   */
  public void setStartupCheckString(String startupCheckString) {
    this.startupCheckString = startupCheckString;
  }

  /**
   * Sets the file the wrapper reads and writes the current {@link ProcState} to.
   *
   * @param stateFile the state file, must not be {@code null}
   */
  public void setStateFile(File stateFile) {
    this.stateFile = stateFile;
  }

  /**
   * Returns the response line the wrapped service emits once it has stopped.
   *
   * @return the stop response line, or {@code null} when no response check is performed
   */
  public String getStopResponse() {
    return stopResponse;
  }

  /**
   * Sets the response line the wrapped service emits once it has stopped.
   *
   * @param stopResponse the new stop response line; may be {@code null} to disable the check
   */
  public void setStopResponse(String stopResponse) {
    this.stopResponse = stopResponse;
  }

  /**
   * Returns the stop key expected by the wrapped service's shutdown port.
   *
   * @return the stop key, never {@code null}
   */
  public String getStopKey() {
    return stopKey;
  }

  /**
   * Sets the stop key expected by the wrapped service's shutdown port.
   *
   * @param stopKey the new stop key; must not be {@code null}
   */
  public void setStopKey(String stopKey) {
    this.stopKey = stopKey;
  }

  /**
   * Indicates whether state changes are persisted to {@link #stateFile}.
   *
   * @return {@code true} when state changes are written to disk, {@code false} otherwise
   */
  public boolean isWriteToStateFile() {
    return writeToStateFile;
  }

  /**
   * Enables or disables persistence of state changes to {@link #stateFile}.
   *
   * @param writeToStateFile {@code true} to write state changes to disk, {@code false} otherwise
   */
  public void setWriteToStateFile(boolean writeToStateFile) {
    this.writeToStateFile = writeToStateFile;
  }

  /**
   * Initializes the wrapper's state by re-discovering the running process id, reloading any
   * persisted state, and starting the {@link StateFileWatcher} when the persisted state indicates
   * the wrapped service is currently starting.
   */
  protected void initState() {
    updatePid();
    loadStateFile();
    if (state == ProcState.STARTING) {
      stateFileWatcher = new StateFileWatcher(stateFile);
      stateFileWatcher.start();
    }
  }

  /**
   * Transitions the wrapper to the supplied state and persists the change to {@link #stateFile}
   * according to {@link #writeToStateFile}.
   *
   * @param toState the new state, must not be {@code null}
   */
  protected void setState(ProcState toState) {
    debug("Setting State method");
    setState(toState, writeToStateFile);
  }

  /**
   * Transitions the wrapper to the supplied state, optionally writing the change to {@link
   * #stateFile}.
   *
   * @param toState the new state, must not be {@code null}
   * @param write {@code true} to persist the new state to {@link #stateFile}, {@code false}
   *     otherwise
   */
  protected void setState(ProcState toState, boolean write) {

    synchronized (stateMonitor) {
      if (state != toState) {
        debug("Setting %s state from %s to %s", name, state, toState);
        state = toState;
        if (write) {
          writeStateToFile(toState);
        }
        if (state == ProcState.STARTING) {
          stateFileWatcher = new StateFileWatcher(stateFile);
          stateFileWatcher.start();
        }
        stateMonitor.notifyAll();
      }
    }
  }

  private void writeStateToFile(ProcState toState) {
    try (PrintWriter out = new PrintWriter(stateFile, StandardCharsets.UTF_8.name())) {
      debug("Writing state %s to file %s", this.toString(), stateFile.toString());
      out.println(String.format("%s %s", toState, this.toString()));
    } catch (FileNotFoundException e) {
      error("Problem setting state to state %s in file %s", toState, stateFile.getAbsolutePath());
    } catch (UnsupportedEncodingException e) {
      // Not going to happen for UTF-8
    }
  }

  /**
   * Blocks the calling thread until the wrapper transitions to {@link ProcState#STARTED} or the
   * configured {@link #startTimeout} expires, whichever happens first.
   *
   * @return {@code true} if the wrapped service reported started within the timeout, {@code false}
   *     otherwise
   */
  protected boolean waitForStart() {
    synchronized (stateMonitor) {
      try {
        stateMonitor.wait(this.startTimeout * 1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return (state == ProcState.STARTED);
  }

  /**
   * Re-reads the persisted state from {@link #stateFile} and updates the wrapper's current {@link
   * #state} accordingly. When the persisted state indicates the wrapped service is running but no
   * matching process id can be found, the state is reset to {@link ProcState#STOPPED} on the
   * assumption that the process was killed externally.
   */
  public void loadStateFile() {
    String lastLine = "";
    ProcState returnState = ProcState.STOPPED;

    if (!stateFile.exists()) {
      debug("State file did not exist. Setting state to STOPPED.");
      setState(ProcState.STOPPED);
      return;
    }
    try (BufferedReader br = new BufferedReader(new FileReader(stateFile))) {
      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null) {
        if (sCurrentLine.trim().length() > 0) lastLine = sCurrentLine;
      }
    } catch (FileNotFoundException e) {
      debug("Cannot find state file ", e);
    } catch (IOException e) {
      debug("Cannot read state file ", e);
    }
    if (lastLine.length() > 0) {
      String fileState = lastLine.split(" ")[0];
      returnState = ProcState.valueOf(fileState);
    }

    if (returnState != ProcState.STOPPED && pid <= 0) {
      debug(
          "Process not running for port %s but state file %s shows state %s process must have been"
              + " killed",
          port, stateFile.getName(), returnState);
      returnState = ProcState.STOPPED;
    }
    // do not re write to file
    setState(returnState, false);
  }

  /**
   * Starts the wrapped service if it is currently {@link ProcState#STOPPED}.
   *
   * <p>Launches the configured {@link #startCmd} in the configured {@link #currentDirectory},
   * drains the process' standard streams via {@link PSStreamGobbler}, registers a JVM shutdown hook
   * that reloads state and stops the service when the JVM exits, and finally monitors the child
   * process.
   */
  public void startServer() {
    if (state == ProcState.STOPPED) {
      info("Starting %s with http port %s", name, port);
      if (stateFile.exists()) stateFile.delete();
      setState(ProcState.STARTING);

      try {
        debug("The commands being used to start the server are: %s", Arrays.toString(startCmd));
        final Process tomcatProc = Runtime.getRuntime().exec(startCmd, null, currentDirectory);
        try (InputStream is = tomcatProc.getInputStream()) {
          new PSStreamGobbler(name, is).start();
          if (startupCheckString != null) {
            try (InputStream eis = tomcatProc.getErrorStream()) {
              new PSStreamGobbler(name, eis, startupCheckString, () -> setState(ProcState.STARTED))
                  .start();
            }
          } else {
            try (InputStream eis = tomcatProc.getErrorStream()) {
              new PSStreamGobbler(name, eis).start();
            }
          }
        }
        updatePid();

        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                      debug(" In shutdown hook, shutting down %s", name);
                      // reload state process could be killed from other wrapper
                      loadStateFile();
                      if (state != ProcState.STOPPING || state != ProcState.STOPPED) stopServer();
                    }));

        monitorNewProcess(tomcatProc);

      } catch (IOException e) {
        error("Cannot start %", e, name);
        System.exit(1);
      }
    } else {
      // NOTE: this text is grepped in install-dts-service.sh
      info(
          "%s Server with http port %s is already running with process id %s state=%s",
          name, port, pid, state);
    }
  }

  /**
   * Stops the wrapped service using the previously configured shutdown settings. Equivalent to
   * {@link #stopServer(boolean)} with {@code force == false}.
   */
  public void stopServer() {
    stopServer(false);
  }

  /**
   * Stops the wrapped service, optionally killing the process forcefully if it does not stop
   * gracefully within the configured {@link #stopTimeout}.
   *
   * @param force {@code true} to kill the process via {@link JettyStartUtils#killProcess(int)} if
   *     it is still running after the graceful stop attempt, {@code false} to leave it running in
   *     that case
   */
  public void stopServer(boolean force) {

    updatePid();
    if (state == ProcState.STOPPED) {
      info("%s already stopped", name);
    } else if (state == ProcState.STOPPING) {
      info("%s  already stopping", name);
    } else if (state == ProcState.STARTING) {
      info("%s Waiting to start before stopping", name);
      waitForStart();
    }

    if (state == ProcState.STARTED) {
      this.stop();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      updatePid();
      if (pid > 1) {
        if (force) {
          JettyStartUtils.killProcess(pid);
          info("Server not stopped on port %s with pid %s Killing process", port, pid);
        } else
          info("Server not stopped on port %s with pid %s use --force option to kill", port, pid);
        updatePid();
      }
      loadStateFile();
    }
  }

  /**
   * Sends the configured stop command to the wrapped service's shutdown port and waits up to {@link
   * #stopTimeout} seconds for it to report stopped.
   *
   * @return {@code true} if the wrapped service stopped successfully, {@code false} otherwise
   */
  public boolean stop() {
    if (pid > 0) {
      setState(ProcState.STOPPING);
      info("Stopping %s with http port %s and shutdown port %s", name, port, shutdownPort);
      try (Socket s = new Socket()) {
        s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), shutdownPort), 2000);
        if (stopTimeout > 0) {
          s.setSoTimeout(stopTimeout * 1000);
        }

        try (OutputStream out = s.getOutputStream()) {
          out.write((stopKey + stopKeySuffix).getBytes());
          out.flush();
        } catch (SocketTimeoutException e) {
          error("Timeout on connection to shutdown port");
        } catch (IOException e) {
          error("Error connecting to shutdown port", e);
        }

        if (stopTimeout > 0 && stopResponse != null) {
          info("Waiting for stop response '%s'", stopResponse);
          try (InputStreamReader isr = new InputStreamReader(s.getInputStream())) {
            try (LineNumberReader lin = new LineNumberReader(isr)) {
              String response;
              while ((response = lin.readLine()) != null) {

                // "Stopped" for jetty
                if (stopResponse.equals(response)) {
                  info("Server reports itself as Stopped");
                  return true;
                } else {
                  debug("Received \"%s\"", response);
                }
              }
            }
          }
        }
      } catch (SocketTimeoutException e) {
        error("Timeout on connection to shutdown port");
      } catch (SocketException e) {
        error("Error connecting to shutdown port", e);
      } catch (UnknownHostException e) {
        error("Error connecting to shutdown port", e);
      } catch (IOException e) {
        error("Error connecting to shutdown port", e);
      }

      try {
        updatePid();
        int count = 20;
        while (pid > 0 && count > 0) {
          Thread.sleep(2000);
          updatePid();
          count--;
        }
        if (pid > 0) {
          debug("Timeout waiting for server to stop after request");
          return false;
        } else {
          debug("Process ID was not greater than 0, setting state to STOPPED.");
          setState(ProcState.STOPPED);
          return true;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        debug("Interrupted exception waiting for server to stop");
      }
    }
    return false;
  }

  /**
   * Monitors the supplied child process in a background thread, transitioning the wrapper's state
   * to {@link ProcState#STOPPED} once the process exits.
   *
   * @param tomcatProc the wrapped service process to monitor, must not be {@code null}
   */
  protected void monitorNewProcess(Process tomcatProc) {

    Thread t =
        new Thread(
            () -> {
              boolean finished = false;
              while (!finished) {
                try {
                  finished = tomcatProc.waitFor(5, TimeUnit.SECONDS);
                  if (!finished) {
                    debug("Not finished");
                  } else {
                    int exitValue = tomcatProc.exitValue();

                    info("DTS stopped with code %s", exitValue);
                    setState(ProcState.STOPPED);
                  }
                } catch (InterruptedException e) {
                  debug("DTS interrupted with code %s", tomcatProc.exitValue());
                  setState(ProcState.STOPPED);
                  Thread.currentThread().interrupt();
                }
              }
            });
  }

  private class StateFileWatcher extends Thread {

    Path myDir;
    WatchService watcher;
    String fileToCheck;

    StateFileWatcher(File file) {
      try {
        // do not wait on this thread
        this.setDaemon(true);
        myDir = Paths.get(file.getParent());
        fileToCheck = file.getName();
        watcher = myDir.getFileSystem().newWatchService();
        myDir.register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    public void run() {
      while (true) {
        try {
          WatchKey watchKey = watcher.take();
          List<WatchEvent<?>> events = watchKey.pollEvents();
          Path dir = (Path) watchKey.watchable();
          for (WatchEvent<?> event : events) {
            Path changed = (Path) event.context();
            if (changed.toString().equals(fileToCheck)) {
              loadStateFile();
            }
          }
          watchKey.reset();
        } catch (Exception e) {
          error("Error: %s", e, e.toString());
        }
      }
    }
  }

  /**
   * Prints the status of the services on the machine. Called via the --status flag in the
   * command-line arguments. <br>
   * <br>
   * Note: The order of the following output is used by the percussion-dts.sh file. Changing the
   * order of the output would require a change of the 'grep' and 'sed' commands in that file.
   */
  public void printStateString() {
    if (state == ProcState.NOT_INSTALLED) {
      info("%s is not installed", name);
    } else if (state == ProcState.STOPPED) {
      info("%s is %s and is configured with port %s", name, state, port);
    } else {
      info(
          "%s is %s, configured with port: %s, and running on process id: %s",
          name, state, port, pid);
    }
  }
}
