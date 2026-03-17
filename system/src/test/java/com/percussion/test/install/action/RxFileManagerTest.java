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
package com.percussion.test.install.action;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.install.OSEnum;
import com.percussion.install.RxFileManager;
import com.percussion.util.PSOsTool;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/***
 * Tests to cover the RxFileManager utilities.
 *
 * @author nate
 *
 */
public class RxFileManagerTest {

  @Test
  @Disabled("Failing on Windows")
  public void testisDTSDirValid() {
    assertTrue(
        RxFileManager.isDTSDir(this.getClass().getResource(".").getPath()),
        "Valid directory not detected.");
    assertFalse(
        RxFileManager.isDTSDir("this ain't a real directory!"),
        "Invalid DTS directory returned as valid");
    assertFalse(RxFileManager.isDTSDir(null), "Null is not a valid directory");
    assertTrue(
        RxFileManager.isDTSDir(this.getClass().getResource("Percussion").getPath()),
        "Percussion not detected as a valid DTS dir");
    assertTrue(
        RxFileManager.isDTSDir(
            this.getClass().getResource("New Folder").getPath().replace("%20", " ")),
        "Folder with spaces not detected as a valid DTS dir");
  }

  /***
   * Test the installation properties file.
   */
  @Test
  public void testInstallationPropFile() {

    RxFileManager o = new RxFileManager();

    try {
      o.setSystemInstallationPropertiesFile(null);
    } catch (IllegalArgumentException e) {
      // Good
    }

    try {
      o.setSystemInstallationPropertiesFile("");
    } catch (IllegalArgumentException e) {
      // Good
    }

    try {
      o.setSystemInstallationPropertiesFile("     ");
    } catch (IllegalArgumentException e) {
      // Good
    }

    o.setSystemInstallationPropertiesFile("dtsinstallation.properties");
    assertEquals("dtsinstallation.properties", "dtsinstallation.properties");
  }

  /***
   * Test the
   * @throws IOException
   */
  @Test
  public void testSystemInstallationPropertiesAbsolute() throws IOException {

    Properties p = new Properties();
    URL prop_file = this.getClass().getResource("dtsinstall.properties");
    OSEnum os = OSEnum.Linux;

    if (PSOsTool.isWindowsPlatform()) os = OSEnum.Windows;

    if (prop_file == null)
      throw new IllegalStateException(
          "Test resource 'dtsinstall.properties' not found in classpath");

    p.put(
        RxFileManager.INSTALL_PROP,
        prop_file.getPath().substring(prop_file.getPath().lastIndexOf("/")));

    // Setup the properties file
    RxFileManager.saveProperties(p, prop_file.getPath());
    RxFileManager.setSystemInstallationPropertiesFile("dtsinstall.properties");
    RxFileManager.setProgramDir(this.getClass().getResource("Program Files").getPath());

    os = OSEnum.Windows;
    assertEquals(
        this.getClass().getResource("Program Files").getPath()
            + File.separatorChar
            + "Percussion"
            + File.separatorChar
            + "dtsinstall.properties",
        RxFileManager.getSystemInstallationPropertiesAbsolute(os));

    os = OSEnum.Linux;
    assertEquals(
        this.getClass().getResource("Program Files").getPath()
            + File.separatorChar
            + "dtsinstall.properties",
        RxFileManager.getSystemInstallationPropertiesAbsolute(os));

    // Test X86
    os = OSEnum.Windows;
    RxFileManager.setProgramDir(this.getClass().getResource("Program Files (x86)").getPath());
    assertEquals(
        this.getClass().getResource("Program Files (x86)").getPath()
            + File.separatorChar
            + "Percussion"
            + File.separatorChar
            + "dtsinstall.properties",
        RxFileManager.getSystemInstallationPropertiesAbsolute(os));

    // Test X86
    os = OSEnum.Linux;
    RxFileManager.setProgramDir(this.getClass().getResource("Program Files (x86)").getPath());
    assertEquals(
        this.getClass().getResource("Program Files (x86)").getPath()
            + File.separatorChar
            + "dtsinstall.properties",
        RxFileManager.getSystemInstallationPropertiesAbsolute(os));
  }

  /***
   * Test retrieval of DTS system properties.
   * @throws IOException
   */
  @Test
  @Disabled("Failing on Windows")
  public void testGetDTSSystemFileProperties() throws IOException {

    OSEnum os = OSEnum.Linux;

    if (PSOsTool.isWindowsPlatform()) os = OSEnum.Windows;

    // Try reading from a typical location on windows.
    if (PSOsTool.isWindowsPlatform()) {
      RxFileManager.setProgramDir(
          this.getClass().getResource("Program Files").getPath().replace("%20", " "));
      Properties props =
          RxFileManager.getDTSSystemFileProperties(
              "dtsinstall.properties", "cm1install.properties", os);

      assertEquals(
          "/home/percussion/DTS;",
          props.getProperty(RxFileManager.INSTALL_PROP, ""),
          "Property file value didn't match!");

      // Now lets test the CM1 fallback.
      RxFileManager.setProgramDir(
          this.getClass().getResource("home/percussion").getPath().replace("%20", " "));
      props =
          RxFileManager.getDTSSystemFileProperties(
              "dtsinstall.properties", "cm1install.properties", os);

      assertEquals(
          "C:\\Program Files\\Percussion\\;C:\\Percussion\\CM1",
          props.getProperty(RxFileManager.INSTALL_PROP, ""),
          "CM1 fail over property file not matched.");
    } else {
      // Linux

    }
  }

  @Test
  public void testNullProgramDir() {
    assertThrows(IllegalArgumentException.class, () -> RxFileManager.setProgramDir(null));
  }

  @Test
  public void testEmptyProgramDir() {
    assertThrows(IllegalArgumentException.class, () -> RxFileManager.setProgramDir(""));
  }

  @Test
  public void testProgramDirTrailing() {
    RxFileManager.setProgramDir("test" + File.separator);
    assertEquals("test", RxFileManager.getProgramDir(), "ProgramDir should have trimmed /");
  }

  @Test
  public void testLoadPropertiesNull() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> RxFileManager.loadProperties(null));
  }

  @Test
  public void testLoadPropertiesEmpty() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> RxFileManager.loadProperties(""));
  }

  @Test
  public void testSavePropertiesEmpty() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> RxFileManager.saveProperties(null, null));
  }

  @Test
  public void testSavePropertiesNullPropFile() throws IOException {
    assertThrows(
        IllegalArgumentException.class, () -> RxFileManager.saveProperties(new Properties(), null));
  }

  @Test
  public void testSavePropertiesNullEmptyFile() throws IOException {
    assertThrows(
        IllegalArgumentException.class, () -> RxFileManager.saveProperties(new Properties(), ""));
  }

  @Test
  public void testRootDir() {
    @SuppressWarnings("unused") // the constructor supports it and it is used so need to test it.
    RxFileManager r =
        new RxFileManager(
            "Root"); // We know this is sketch - still needs tested as so much legacy code is
    // present. @TODO: Rewrite the installer.

    assertEquals("Root", RxFileManager.getRootDir(), "Root should match");

    RxFileManager.setRootDir("NewRoot");
    assertEquals("NewRoot", RxFileManager.getRootDir(), "Root should match");
  }

  @Test
  public void getServerConfigLocation() {
    RxFileManager r = new RxFileManager();

    assertEquals(
        r.getInstallerConfigLocation() + File.separator + RxFileManager.REPOSITORY_FILE,
        r.getRepositoryFile());
  }
}
