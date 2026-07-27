/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.cx.javafx.PSWindowManager;
import com.percussion.security.xml.PSSecureXMLUtils;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * The JavaFX {@link javafx.application.Application} entry point for the Percussion Desktop Content
 * Explorer (DCE). It bootstraps the runtime environment, configures logging and the look and feel,
 * and launches the {@link PSContentExplorerFrame} which hosts the {@link PSContentExplorerApplet}.
 */
public class PSContentExplorerApplication extends Application {

  /**
   * Constructs the application. The real initialization happens in {@link #start(Stage)} once the
   * JavaFX toolkit is ready.
   */
  public PSContentExplorerApplication() {
    super();
  }

  static Logger log = LogManager.getLogger(PSContentExplorerApplication.class);

  private static File configDir;
  private static Dimension dimension = new Dimension(1180, 750);

  /** Flag indicating the number of times a session-expired condition has been detected. */
  public static int sessionExpired = 0;

  private static File logConfig;

  private static final String DEFAULT_CONFIG_FOLDER_NAME = ".perc_config";

  /**
   * The JavaFX application entry point. Launches the JavaFX application with the supplied command
   * line arguments.
   *
   * @param args the command line arguments passed to the application.
   */
  @SuppressWarnings("java:S106")
  public static void main(String[] args) {
    System.out.println(Arrays.toString(args));
    launch(args);
  }

  /**
   * The base desktop content explorer frame hosted by this application, may be <code>null</code>.
   */
  private static PSContentExplorerFrame baseFrame = null;

  /**
   * Gets the base desktop content explorer frame hosted by this application.
   *
   * @return the base frame, may be <code>null</code> if the application has not yet started.
   */
  public static PSContentExplorerFrame getBaseFrame() {
    return baseFrame;
  }

  /**
   * Sets the base desktop content explorer frame hosted by this application.
   *
   * @param baseFrame the new base frame, may be <code>null</code>.
   */
  public static void setBaseFrame(PSContentExplorerFrame baseFrame) {
    PSContentExplorerApplication.baseFrame = baseFrame;
  }

  private void addOSXKeyStrokes(InputMap inputMap) {
    inputMap.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK),
        DefaultEditorKit.copyAction);
    inputMap.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK),
        DefaultEditorKit.cutAction);
    inputMap.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK),
        DefaultEditorKit.pasteAction);
    inputMap.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK),
        DefaultEditorKit.selectAllAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), "copy");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "selectAll");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), "paste");
  }

  @Override
  @SuppressWarnings("java:S106")
  public void start(Stage primaryStage) {
    Platform.setImplicitExit(false);
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      // This must be performed immediately after the LaF has been set
      if (System.getProperty("os.name", "").startsWith("Mac")) {
        // Ensure OSX key bindings are used for copy, paste etc
        // Use the Nimbus keys and ensure this occurs before any component creation
        addOSXKeyStrokes((InputMap) UIManager.get("EditorPane.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("PasswordField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("TextField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("TextPane.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("TextArea.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("Table.ancestorInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("Tree.focusInputMap"));
      }
    } catch (Exception e) {
      log.error(e);
    }

    String version = this.getClass().getPackage().getImplementationVersion();

    Parameters parameters = getParameters();

    Map<String, String> params = this.getParameters().getNamed();
    String codebase = params.get("codebase");
    if (codebase == null) codebase = "http://localhost:9992";
    String protocol = null;
    String host = null;
    int port = -1;
    URI uri = null;
    String clientConfigDir = DEFAULT_CONFIG_FOLDER_NAME;
    try {
      if (StringUtils.isNotEmpty(codebase)) {
        uri = new URI(codebase);
        protocol = uri.getScheme();
        host = uri.getHost();
        port = uri.getPort();
        if (port == -1) port = ("https".equals(protocol)) ? 443 : 80;

        clientConfigDir += File.separator + host.replace(".", "_");

        if (port < 0) clientConfigDir += "_" + port;
      }

    } catch (URISyntaxException e) {
      log.error("Codebase parameter is not a valid url " + codebase);
    }

    configDir =
        new File(
            System.getProperty("user.home") + File.separator + clientConfigDir + File.separator);

    configDir.mkdirs();

    logConfig = new File(configDir, "log4j.properties");

    System.out.println("Setting log4j config to " + logConfig);
    System.setProperty("configDir", configDir.getAbsolutePath());
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (!logConfig.exists()) {
      URL inputUrl = loader.getResource("dce_log4j.properties");
      if (inputUrl != null) {
        Configurator.initialize(null, inputUrl.toString());

        try {
          FileUtils.copyURLToFile(inputUrl, logConfig);
        } catch (IOException e) {
          log.error("Cannot write user log config to " + logConfig.getAbsolutePath());
        }
      }
    } else {
      Configurator.initialize(null, logConfig.getAbsolutePath());
    }
    PSSecureXMLUtils.setupJAXPDefaults();

    //      System.setProperty("javax.xml.parsers.SAXParserFactory",
    //            "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    //      System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    //            "com.percussion.xml.PSDocumentBuilderFactoryImpl");
    //      System.setProperty("javax.xml.transform.TransformerFactory",
    //            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    //      System.setProperty("javax.xml.xpath.XPathFactory",
    // "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl");
    //      System.setProperty("javax.xml.datatype.DatatypeFactory",
    //              "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl");

    log.info("USING JAVA:   " + System.getProperty("java.version"));
    log.info("DesktopContentExplorer.jar version :   " + version);

    WebView web = new WebView();
    log.info("Java Version:   " + System.getProperty("java.runtime.version"));
    log.info("JavaFX Version: " + System.getProperty("javafx.runtime.version"));
    log.info(
        "OS:             " + System.getProperty("os.name") + ", " + System.getProperty("os.arch"));
    log.info("User Agent:     " + web.getEngine().getUserAgent());

    Map<String, String> namedParameters = parameters.getNamed();
    List<String> rawArguments = parameters.getRaw();
    List<String> unnamedParameters = parameters.getUnnamed();

    log.debug("\nnamedParameters -");
    for (Map.Entry<String, String> entry : namedParameters.entrySet())
      log.debug(entry.getKey() + " : " + entry.getValue());

    for (String raw : rawArguments) log.debug(raw);

    for (String unnamed : unnamedParameters) log.debug(unnamed);

    log.debug("params =" + this.getParameters().getNamed());

    log.debug("protocol =" + protocol);
    log.debug("host =" + host);
    log.debug("port =" + port);

    baseFrame = new PSContentExplorerFrame(uri);

    PSWindowManager.getInstance().addRoot(baseFrame);

    log.info("Launching desktop content explorer");
  }

  /**
   * Gets the content explorer applet hosted by the base frame, if any.
   *
   * @return the applet instance, may be <code>null</code> if the base frame has no applet.
   */
  public static PSContentExplorerApplet getApplet() {
    return baseFrame.getApplet();
  }

  /**
   * Gets the configuration directory used by the desktop content explorer.
   *
   * @return the configuration directory, may be <code>null</code> if not yet initialized.
   */
  public static File getConfigDir() {
    return configDir;
  }

  /**
   * Sets the configuration directory used by the desktop content explorer.
   *
   * @param configDir the new configuration directory, may be <code>null</code>.
   */
  public static void setConfigDir(File configDir) {
    PSContentExplorerApplication.configDir = configDir;
  }

  /**
   * Logs the current user out of the desktop content explorer. Triggers cleanup of the base frame
   * and shows the login panel again.
   */
  public static void logout() {
    SwingUtilities.invokeLater(
        () -> {
          baseFrame.logout();
        });
  }

  @Override
  public void stop() throws Exception {
    if (baseFrame != null) {
      SwingUtilities.invokeLater(
          () -> {
            baseFrame.cleanup();
            Platform.exit();
            System.exit(0);
          });
    }
  }

  /**
   * Gets the preferred main frame size used by the desktop content explorer. Equivalent to {@link
   * #getDimension()}.
   *
   * @return the main frame size, never <code>null</code>.
   */
  public static Dimension getMainFrameSize() {

    return getDimension();
  }

  /**
   * Gets the current main frame size used by the desktop content explorer.
   *
   * @return the main frame size, never <code>null</code>.
   */
  public static Dimension getDimension() {
    return dimension;
  }

  /**
   * Sets the main frame size used by the desktop content explorer.
   *
   * @param dimension the new main frame size, may be <code>null</code>.
   */
  public static void setDimension(Dimension dimension) {
    PSContentExplorerApplication.dimension = dimension;
  }
}
