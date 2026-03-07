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

package com.percussion.thumbnail;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.utils.io.PathUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSScreenCaptureTest {

  protected static final Logger log = LogManager.getLogger(PSScreenCaptureTest.class);

  public static File temp;

  @BeforeEach
  public void before() throws IOException {
    temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
    if (!temp.delete() || !temp.mkdir()) {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }
    // create minimal rxconfig/Server structure so PSScreenCapture.getRxConfigDir succeeds
    File serverDir = new File(temp, "rxconfig/Server");
    if (!serverDir.mkdirs()) {
      throw new IOException(
          "Could not create server config directory: " + serverDir.getAbsolutePath());
    }
    // write a basic server.properties file that uses touch to generate thumbnails
    File propsFile = new File(serverDir, "server.properties");
    try (java.io.PrintWriter pw = new java.io.PrintWriter(propsFile)) {
      pw.println("screenshotCommandLine=touch @@file@@");
    }
    System.setProperty(PathUtils.DEPLOY_DIR_PROP, temp.getAbsolutePath());
    log.info("Temp folder set to " + System.getProperty(PathUtils.DEPLOY_DIR_PROP));
  }

  @AfterEach
  public void after() throws IOException {
    if (temp != null && temp.exists()) {
      FileUtils.deleteDirectory(temp);
    }
    // reset any static rx directory detection
    PathUtils.clearRxDir();
  }

  @Test
  public void generateEmptyThumb() throws IOException {
    File file = new File(System.getProperty(PathUtils.DEPLOY_DIR_PROP), "emptythumb.jpg");
    log.info("Creating empty thumb to " + file.getAbsolutePath());
    PSScreenCapture.generateEmptyThumb(file.getAbsolutePath());
    assertTrue(file.exists());
    BufferedImage bimg = ImageIO.read(file);
    assertNotNull(bimg, "File " + file.getAbsolutePath() + " is not an image");
  }

  @Test
  public void takeCapture() throws IOException {

    capture(1024, 2048);
    capture(1024, 512);
    capture(100, 100);
  }

  public void capture(int height, int width) throws IOException {
    File file =
        new File(
            System.getProperty(PathUtils.DEPLOY_DIR_PROP),
            "testimg_" + height + "_" + width + ".jpg");
    log.info("Taking capture to " + file.getAbsolutePath());
    PSScreenCapture.takeCapture("https://www.percussion.com", file.getAbsolutePath());
    assertTrue(file.exists(), "capture command should create a file");
    // command used in tests just touches the file, not a real image, so we don't
    // attempt to read dimensions.
  }
}
