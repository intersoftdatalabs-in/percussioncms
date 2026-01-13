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

package com.percussion.rest.assets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.MainTest;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("IntegrationTest")
public class AssetsTest extends MainTest {

  private static final Logger log = LogManager.getLogger(AssetsTest.class);

  @Test
  public void testRenameAsset() {
    var assetEntity = Entity.entity("{}", MediaType.APPLICATION_JSON_TYPE);

    try {
      var response =
          target("assets/rename/Assets/path1/pathsub/pathsub2/page1.png/newname.png")
              .request()
              .post(assetEntity, Asset.class);
      assertTrue(response.getName().equals("newname.png"), "New Name Should Match");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    }
  }
}
