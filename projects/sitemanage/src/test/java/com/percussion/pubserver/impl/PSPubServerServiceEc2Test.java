/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.percussion.rx.delivery.impl.PSAmazonS3DeliveryHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PSPubServerService#isEC2Instance()} delegation to the shared IMDSv2-aware
 * probe in {@link PSAmazonS3DeliveryHandler} (issue #2284).
 */
@ExtendWith(MockitoExtension.class)
class PSPubServerServiceEc2Test {

  @Test
  void isEC2Instance_delegatesToDeliveryHandler() {
    try (MockedStatic<PSAmazonS3DeliveryHandler> handler =
        mockStatic(PSAmazonS3DeliveryHandler.class)) {
      handler.when(PSAmazonS3DeliveryHandler::isEC2Instance).thenReturn(true);
      assertTrue(PSPubServerService.isEC2Instance());

      handler.when(PSAmazonS3DeliveryHandler::isEC2Instance).thenReturn(false);
      assertFalse(PSPubServerService.isEC2Instance());
    }
  }
}
