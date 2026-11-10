/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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

package com.percussion.cx;

import com.percussion.cx.error.PSContentExplorerException;
import com.percussion.util.PSHttpConnection;
import com.percussion.webservices.security.data.PSLogin;
import com.percussion.webservices.securityservices.LoginRequest;
import com.percussion.webservices.securityservices.LoginResponse;
import com.percussion.webservices.securityservices.NotAuthenticatedFaultMessage;
import com.percussion.webservices.securityservices.Security;
import jakarta.xml.ws.BindingProvider;
import java.awt.Window;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Manages the Content Explorer user session, including login, periodic server-side session state
 * checks, idle/timeout warnings, and logout. Runs as a scheduled task that polls the server and
 * prompts the user to extend or end an expiring session.
 */
public class PSCESessionManager implements Runnable {
  private static final String LOGOUT_NOW = "Logout Now";

  private static final String EXTEND_SESSION = "Extend Session";

  private static final Logger log = LogManager.getLogger(PSCESessionManager.class);

  private ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

  private JDialog dialog = null;

  private JLabel timeLabel = new JLabel("00:00:00");

  private long expireTime = 0;

  private long warningTime = 0;

  private long lastCheckTime = 0;

  private ScheduledFuture<?> countdown;

  private PSLogin loginInfo = null;

  private boolean extendSession = false;

  private AtomicBoolean dialogOn = new AtomicBoolean(false);

  private static volatile PSCESessionManager instance = null;

  private String protocol;

  private String server;

  private String port;

  private boolean isLoggedIn = false;

  private String userName = "unknown";

  /** Default constructor that initializes the time-remaining label name. */
  public PSCESessionManager() {
    timeLabel.setName("timeRemaining");
  }

  /**
   * Starts the session manager and schedules the first session state check.
   *
   * @param protocol the server protocol (e.g. <code>http</code> or <code>https</code>), assumed not
   *     <code>null</code> or empty.
   * @param server the server host name, assumed not <code>null</code> or empty.
   * @param port the server port, assumed not <code>null</code> or empty.
   * @param loginInfo the login information for the session, assumed not <code>null</code>.
   */
  public void start(String protocol, String server, String port, PSLogin loginInfo) {
    this.protocol = protocol;
    this.server = server;
    this.port = port;
    this.loginInfo = loginInfo;
    ex = Executors.newSingleThreadScheduledExecutor();
    ex.schedule(this, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public void run() {
    try {
      PSContentExplorerApplet applet = PSContentExplorerApplication.getApplet();

      SessionStatus status = pollSessionState();
      long expiry = status.expiry();
      long warning = status.warning();

      log.debug("session status check: expiry={} warning={}", expiry, warning);

      if (isExpired(expiry)) {
        handleExpiredSession();
      } else if (isWarningState(expiry, warning)) {
        handleSessionWarning(applet, expiry);
      } else {
        handleHealthySession(expiry, warning);
      }

    } catch (Exception e) {
      log.error("Error with session check thread", e);
    }
  }

  /**
   * Branch predicate for an expired session: no usable time remains.
   *
   * @param expiry remaining session time in milliseconds.
   * @return {@code true} when the session has expired.
   */
  static boolean isExpired(long expiry) {
    return expiry <= 0;
  }

  /**
   * Branch predicate for the expiring-session warning state.
   *
   * @param expiry remaining session time in milliseconds.
   * @param warning remaining warning-window time in milliseconds.
   * @return {@code true} when the warning dialog should be shown.
   */
  static boolean isWarningState(long expiry, long warning) {
    return expiry > 0 && warning > 0 && expiry <= warning;
  }

  /**
   * Builds the session-check resource path, requesting a session extension when one was banked by
   * a previous warning-dialog choice.
   *
   * @param extendSession {@code true} to request a session extension.
   * @return the session-check resource path, never {@code null}.
   */
  static String sessionCheckResource(boolean extendSession) {
    String resource = "/Rhythmyx/sessioncheck";
    if (extendSession) {
      resource += "?extendSession=true";
    }
    return resource;
  }

  /**
   * Computes the fallback session status used when the server cannot be reached, assuming the
   * previously observed countdown is still running.
   *
   * @param expireTime the last known absolute expiry time in milliseconds.
   * @param warningTime the last known absolute warning time in milliseconds.
   * @param now the current time in milliseconds.
   * @return the remaining expiry and warning times, never {@code null}.
   */
  static SessionStatus fallbackStatus(long expireTime, long warningTime, long now) {
    return new SessionStatus(expireTime - now, warningTime - now);
  }

  /**
   * Polls the server for the current session state, falling back to the previously observed
   * countdown when the server cannot be reached. Updates {@link #lastCheckTime},
   * {@link #expireTime}, and {@link #warningTime} on a successful check.
   *
   * @return the remaining expiry and warning times in milliseconds, never {@code null}.
   */
  private SessionStatus pollSessionState() {
    boolean extend = extendSession;
    if (extend) {
      log.debug("Extending session");
      extendSession = false;
    }
    try {
      String resource = sessionCheckResource(extend);
      int portNumber = Integer.parseInt(port);
      URL url = PSContentExplorerUtils.toURL(protocol, server, portNumber, resource);

      PSHttpConnection httpConnection =
          new PSHttpConnection(
              PSContentExplorerUtils.toURL(protocol, server, portNumber, "/Rhythmyx/sys_resources"),
              loginInfo.getSessionId());
      JSONObject sessionObject = httpConnection.getJSON(url);

      long expiry = sessionObject.getLong("expiry");
      long warning = sessionObject.getLong("warning");
      lastCheckTime = System.currentTimeMillis();
      expireTime = lastCheckTime + expiry;
      warningTime = expireTime - warning;
      return new SessionStatus(expiry, warning);

    } catch (Exception e) {
      // Assume that countdown is continuing
      log.warn(
          "Failed to check session state server may be down or network connection lost: {}",
          e.getLocalizedMessage());
      log.debug(
          "Failed to check session state server may be down or network connection lost: {}",
          e.getLocalizedMessage(),
          e);
      return fallbackStatus(expireTime, warningTime, System.currentTimeMillis());
    }
  }

  /** Logs out and shuts down when the session has expired. No-op when not logged in. */
  private void handleExpiredSession() {
    if (!isLoggedIn) {
      return;
    }
    if (dialog != null) {
      dialog.dispose();
    }
    PSContentExplorerApplication.getBaseFrame().logout();
    shutdown();
  }

  /**
   * Shows the expiring-session warning dialog (once) and schedules the next session check.
   *
   * @param applet the content explorer applet, assumed not {@code null}.
   * @param expiry remaining session time in milliseconds.
   */
  private void handleSessionWarning(PSContentExplorerApplet applet, long expiry)
      throws Exception {
    if (dialogOn.compareAndSet(false, true)) {
      showSessionWarningDialog(applet);
      saveAppletStateQuietly(applet);
    }
    if (!ex.isShutdown()) ex.schedule(this, expiry, TimeUnit.MILLISECONDS);
    log.debug("Waiting for logout in {}ms", expiry);
  }

  /**
   * Dismisses a visible warning when the session recovered and schedules the next check for the
   * start of the warning window.
   *
   * @param expiry remaining session time in milliseconds.
   * @param warning remaining warning-window time in milliseconds.
   */
  private void handleHealthySession(long expiry, long warning) {
    if (dialogOn.compareAndSet(true, false)) {
      log.debug("Turning off warning");
      SwingUtilities.invokeLater(
          () -> {
            // Stop countdown timer
            if (countdown != null) countdown.cancel(false);
            if (dialog != null && dialog.isVisible()) {
              dialog.dispose();
            }
          });
    }
    if (!ex.isShutdown()) ex.schedule(this, (expiry - warning), TimeUnit.MILLISECONDS);
    log.debug("Waiting for warning in {}ms", expiry - warning);
  }

  /**
   * Shows the "still there?" warning dialog on the event-dispatch thread with a live countdown.
   *
   * @param applet the content explorer applet, assumed not {@code null}.
   */
  private void showSessionWarningDialog(PSContentExplorerApplet applet) {
    Object[] options = {
      applet.getResourceString(getClass(), EXTEND_SESSION),
      applet.getResourceString(getClass(), LOGOUT_NOW)
    };

    SwingUtilities.invokeLater(
        () -> {
          if (!ex.isShutdown())
            countdown = ex.scheduleAtFixedRate(new Countdown(), 0, 1, TimeUnit.SECONDS);

          Object[] message = {
            applet.getResourceString(getClass(), "You will be automatically logged out in\n"),
            timeLabel
          };
          JOptionPane optionPane =
              new JOptionPane(
                  message,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
                  null,
                  options,
                  options[0]);

          dialog =
              optionPane.createDialog(
                  applet.getDialogParentFrame(),
                  applet.getResourceString(getClass(), "Are you still there?"));
          dialog.pack();
          dialog.setVisible(true);

          countdown.cancel(false);
          handleWarningChoice(optionPane, options);
        });
  }

  /**
   * Applies the user's warning-dialog choice: logout, bank a session extension, or dismiss.
   *
   * @param optionPane the answered option pane, assumed not {@code null}.
   * @param options the dialog options, {@code options[0]} extends and {@code options[1]} logs out.
   */
  private void handleWarningChoice(JOptionPane optionPane, Object[] options) {
    Object value = optionPane.getValue();
    if (value == null) {
      log.debug("Close / Cross dialog button clicked.");
      if (countdown != null) {
        countdown.cancel(false);
      }

      if (dialog != null && dialog.isVisible()) {
        dialog.dispose();
      }
      return;
    }
    if (value.equals(options[1])) {
      PSContentExplorerApplication.getBaseFrame().logout();
      isLoggedIn = false;
      shutdown();
    } else if (value.equals(options[0])) {
      log.debug("Setting extendSession");
      extendSession = true;
      if (!ex.isShutdown()) ex.schedule(this, 0, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Saves the applet state while the session is idle. Must not touch the session clock.
   *
   * @param applet the content explorer applet, assumed not {@code null}.
   */
  private static void saveAppletStateQuietly(PSContentExplorerApplet applet)
      throws PSContentExplorerException {
    // Take the chance to save the applet state while there is no
    // activity. Need to make sure that saving the state does not reset session time.
    if (applet.getOptionsManager() != null) {
      applet.saveAppletState();
      applet.getOptionsManager().save(true);
    }
  }

  /** Remaining session-time snapshot in milliseconds, as reported by the last session check. */
  record SessionStatus(long expiry, long warning) {}

  /**
   * Shuts down the session manager, disposing any open warning dialog and stopping the scheduler.
   */
  public void shutdown() {
    SwingUtilities.invokeLater(
        () -> {
          if (!ex.isShutdown()) {
            if (dialog != null && dialog.isVisible()) {
              dialog.dispose();
            }
            ex.shutdown();
          }
        });
  }

  private class Countdown implements Runnable {
    @Override
    public void run() {
      long remainTime = expireTime - System.currentTimeMillis();
      SwingUtilities.invokeLater(
          () -> {
            String timeText = DurationFormatUtils.formatDuration(remainTime, "HH:mm:ss");
            log.debug("Remaining Time: {} -- Time Text: {}", remainTime, timeText);
            timeLabel.setText(timeText);
            if (remainTime <= 0
                || DurationFormatUtils.formatDuration(remainTime, "HH:mm:ss")
                    .equalsIgnoreCase("00:00:00")) {
              log.debug("Stopping timer and logging out after timeout.");
              if (countdown != null) {
                countdown.cancel(false);
                log.debug("Timer stopped.");
              }

              if (dialog != null && dialog.isVisible()) {
                dialog.dispose();
              }
              closeAllDialog();
              PSContentExplorerApplication.getBaseFrame().logout();
              isLoggedIn = false;
              shutdown();
              return;
            }
          });
    }
  }

  private void closeAllDialog() {
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      if (window instanceof JDialog jDialog) {
        if (jDialog.getContentPane().getComponents().length > 0
            && jDialog.getContentPane().getComponent(0) instanceof JOptionPane) {
          jDialog.dispose();
        }
      }
    }
  }

  /** Stops the session manager scheduler without disposing any open dialog. */
  public void stop() {
    ex.shutdown();
  }

  /**
   * Logs the user in to the server, establishes the session, and starts the session state monitor.
   *
   * @param protocol the server protocol (e.g. <code>http</code> or <code>https</code>), assumed not
   *     <code>null</code> or empty.
   * @param server the server host name, assumed not <code>null</code> or empty.
   * @param port the server port, assumed not <code>null</code> or empty.
   * @param uid the user identifier, assumed not <code>null</code> or empty.
   * @param password the user password, assumed not <code>null</code> or empty.
   * @param locale the locale code to use for the session, assumed not <code>null</code> or empty.
   * @throws Exception if authentication fails, the server cannot be reached, or the connection
   *     times out.
   */
  public void login(
      String protocol, String server, String port, String uid, String password, String locale)
      throws Exception {
    int timeoutInMs = 30_000;
    try {
      // Clear all cookies that may have been left over from previous sessions
      CookieManager manager = new CookieManager();
      manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
      CookieHandler.setDefault(manager);

      PSWsUtils.setConnectionInfo(protocol, server, Integer.parseInt(port));

      Security securitySession = PSWsUtils.getSecurityService();

      // Set HTTP connection and receive timeouts via JAX-WS BindingProvider
      Map<String, Object> requestContext = ((BindingProvider) securitySession).getRequestContext();
      requestContext.put("jakarta.xml.ws.client.connectionTimeout", timeoutInMs);
      requestContext.put("jakarta.xml.ws.client.receiveTimeout", timeoutInMs);

      LoginRequest logRequest = new LoginRequest();
      logRequest.setUsername(uid);
      logRequest.setPassword(password);
      logRequest.setLocaleCode(locale);

      log.debug("Logging in ...");
      LoginResponse resp = securitySession.login(logRequest);
      log.debug("Got login response");
      PSLogin login = resp.getPSLogin();
      String session = login.getSessionId();
      PSWsUtils.setRxSessionHeader(securitySession, session);

      if (!login.getCommunities().getPSCommunity().isEmpty()) {
        log.info(
            "Logged in using community {} locale = {}",
            login.getDefaultCommunity(),
            login.getDefaultLocaleCode());
        log.info("session={}", login.getSessionId());
      }

      // Replicated javascript session check
      // Hold the resulting login info in loginInfo
      this.userName = uid;
      this.start(protocol, server, port, login);
      isLoggedIn = true;

    } catch (NotAuthenticatedFaultMessage e) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to login. Resetting applet", e);
      } else {
        log.info(
            "Header login failed. Failed to authenticate with username and password. {}",
            e.getMessage());
      }
      PSContentExplorerApplication.getBaseFrame().logout();
      isLoggedIn = false;
      shutdown();
      throw new RuntimeException("Authentication failed. Invalid User Name and/or Password.");
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to login. Resetting applet", e);
      } else {
        log.info(
            "Header login failed. Session may have expired or connection lost {}", e.getMessage());
      }
      PSContentExplorerApplication.getBaseFrame().logout();
      isLoggedIn = false;
      shutdown();
      throw new RuntimeException("Cannot connect to server or timed out");
    }
  }

  /**
   * Gets the singleton session manager instance, creating it on first access.
   *
   * @return the singleton {@link PSCESessionManager} instance, never <code>null</code>.
   */
  public static PSCESessionManager getInstance() {
    if (instance == null) {
      instance = new PSCESessionManager();
    }
    return instance;
  }

  /**
   * Gets the login information for the current session.
   *
   * @return the session login information, may be <code>null</code> if not yet logged in.
   */
  public PSLogin getLoginInfo() {
    return loginInfo;
  }

  /**
   * Determines whether the user is currently logged in.
   *
   * @return <code>true</code> if the user is logged in, <code>false</code> otherwise.
   */
  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  /**
   * Sets the logged-in state of the session.
   *
   * @param isLoggedIn <code>true</code> to mark the session as logged in, <code>false</code>
   *     otherwise.
   */
  public void setLoggedIn(boolean isLoggedIn) {
    this.isLoggedIn = isLoggedIn;
  }

  /**
   * Gets the name of the currently logged-in user.
   *
   * @return the user name, never <code>null</code> (defaults to <code>"unknown"</code>).
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the name of the currently logged-in user.
   *
   * @param userName the user name, assumed not <code>null</code>.
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }
}
