/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.feeds.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.feeds.data.PSFeedDTO;
import com.percussion.delivery.utils.security.PSHttpClient;
import com.percussion.error.PSExceptionUtils;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSFeedsServiceTests {

  private static final Logger log = LogManager.getLogger(PSFeedsServiceTests.class);

  @TempDir Path temporaryFolder;
  private String rxdeploydir;

  @BeforeEach
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());
  }

  @AfterEach
  public void teardown() {
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  @Autowired private IPSFeedDao feedsDao;

  @Autowired private PSHttpClient httpClient;

  public void PSFeedsServiceTest() {
    // noop
  }

  @Test
  public void testExternalRSSFeed() throws UnsupportedEncodingException, PSEncryptionException {

    PSFeedService svc = new PSFeedService(feedsDao, httpClient);

    PSFeedDTO feedDTO = new PSFeedDTO();

    String url =
        PSEncryptor.encryptString(
            temporaryFolder.toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR),
            "https://www.nasa.gov/rss/dyn/breaking_news.rss");

    feedDTO.setFeedsUrl(url);
    String xml = svc.readExternalFeed(feedDTO);
    log.info(xml);
    assertTrue(xml != null);
    assertTrue(xml.toLowerCase().contains("nasa"));
  }

  @Test
  public void testInvalidFileURL() throws PSEncryptionException {

    PSFeedService svc = new PSFeedService(feedsDao, httpClient);

    PSFeedDTO feedDTO = new PSFeedDTO();

    String url =
        PSEncryptor.encryptString(
            temporaryFolder.toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR),
            "file://www.nasa.gov/rss/dyn/breaking_news.txt");
    feedDTO.setFeedsUrl(url);

    boolean passed = false;
    try {
      String xml = svc.readExternalFeed(feedDTO);
      assertTrue(StringUtils.isEmpty(xml));
      passed = true;
    } catch (WebApplicationException x) {
      log.error(PSExceptionUtils.getMessageForLog(x));
      log.debug(x);
    }

    assertTrue(passed);
  }

  @Test
  public void testInvaliddataURL() throws PSEncryptionException {

    PSFeedService svc = new PSFeedService(feedsDao, httpClient);

    PSFeedDTO feedDTO = new PSFeedDTO();

    // Feed should come back empty as the url is not valid
    String url =
        PSEncryptor.encryptString(
            temporaryFolder.toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR),
            "data://www.nasa.gov/rss/dyn/breaking_news.jar");
    feedDTO.setFeedsUrl(url);

    boolean passed = false;
    try {
      String xml = svc.readExternalFeed(feedDTO);
      assertTrue(StringUtils.isEmpty(xml), "Feed should be empty.");
      passed = true;
    } catch (WebApplicationException x) {
      log.error(PSExceptionUtils.getMessageForLog(x));
      log.debug(x);
    }

    assertTrue(passed);
  }
}
