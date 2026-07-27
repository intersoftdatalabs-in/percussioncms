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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.theme;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.webservices.content.IPSContentWs;
import java.io.InputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link PSAssetCreator}. Sunny Sal says: "Testing asset creation like a
 * boss!"
 */
@IntegrationTest
@ExtendWith(SpringExtension.class)
class PSAssetCreatorTest {

  private static final String TEMP_PREFIX = "TemplateTest";

  private PSSiteDataServletTestCaseFixture fixture;
  private IPSAssetService assetService;
  private IPSContentWs contentWs;
  private final PSAssetCreator assetCreator = new PSAssetCreator();

  @BeforeEach
  void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    fixture = new PSSiteDataServletTestCaseFixture(request, response);
    fixture.setUp("Admin", "demo", "Default");
  }

  @AfterEach
  void tearDown() throws Exception {
    fixture.tearDown();
    fixture.templateCleanUp(TEMP_PREFIX);
  }

  @Test
  void testCreateAssetForImage() {
    var folderPath = "/Assets/uploads/www.percussion.com/image.JPG";
    try (InputStream in = getClass().getResourceAsStream("image.JPG")) {
      var newAsset = assetCreator.createAssetIfNeeded(in, folderPath);
      assertNotNull(newAsset);
      assertEquals("image.JPG", newAsset.getName());
    } catch (Exception e) {
      fail("Error creating the Asset.", e);
    }
  }

  @Test
  void testCreateAssetForGifImage() {
    var folderPath = "/Assets/uploads/www.percussion.com/widgetIconPreviewPageOver.gif";
    try (InputStream in = getClass().getResourceAsStream("widgetIconPreviewPageOver.gif")) {
      var newAsset = assetCreator.createAssetIfNeeded(in, folderPath);
      assertNotNull(newAsset);
      assertEquals("widgetIconPreviewPageOver.gif", newAsset.getName());
    } catch (Exception e) {
      fail("Error creating the Asset.", e);
    }
  }

  @Test
  void testCreateAssetForFlash() {
    // Note: test name kept for history; flash widget removed (task_1780430034917 / GH#685),
    // .swf now creates percFileAsset via generic path. QA verified no widget refs remain.
    var folderPath = "/Assets/uploads/www.percussion.com/flash.swf";
    try (InputStream in = getClass().getResourceAsStream("flash.swf")) {
      var newAsset = assetCreator.createAssetIfNeeded(in, folderPath);
      assertNotNull(newAsset);
      assertEquals("flash.swf", newAsset.getName());
    } catch (Exception e) {
      fail("Error creating the Asset.", e);
    }
  }

  @Test
  void testCreateAssetForWord() {
    var folderPath = "/Assets/uploads/www.percussion.com/testWordDoc.doc";
    try (InputStream in = getClass().getResourceAsStream("testWordDoc.doc")) {
      var newAsset = assetCreator.createAssetIfNeeded(in, folderPath);
      assertNotNull(newAsset);
      assertEquals("testWordDoc.doc", newAsset.getName());
    } catch (Exception e) {
      fail("Error creating the Asset.", e);
    }
  }

  @Test
  void testCreateAssetForPdf() {
    var folderPath = "/Assets/uploads/www.percussion.com/testPdf.pdf";
    try (InputStream in = getClass().getResourceAsStream("testPdf.pdf")) {
      var newAsset = assetCreator.createAssetIfNeeded(in, folderPath);
      assertNotNull(newAsset);
      assertEquals("testPdf.pdf", newAsset.getName());
    } catch (Exception e) {
      fail("Error creating the Asset.", e);
    }
  }

  public IPSAssetService getAssetService() {
    return assetService;
  }

  public void setAssetService(IPSAssetService assetService) {
    this.assetService = assetService;
  }

  public IPSContentWs getContentWs() {
    return contentWs;
  }

  public void setContentWs(IPSContentWs contentWs) {
    this.contentWs = contentWs;
  }
}
