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
package test.percussion.pso.assembler;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.assembly.IPSAssemblyItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * // REFACTORED: CP-JAVA11
 *
 * @author natechadwick
 */
@ExtendWith(MockitoExtension.class)
=======
import static org.junit.Assert.*;

import com.percussion.services.assembly.IPSAssemblyItem;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author natechadwick */
>>>>>>> development-8.1.x
public class PSValidatingVelocityAssemblerTest {
  @Mock IPSAssemblyItem item;

<<<<<<< HEAD
  @BeforeEach
  public void setUp() {}

  @Test
  public void XHTMLCleanTest() throws Exception {
    // Example Mockito usage:
    Mockito.when(item.toString()).thenReturn("clean-xhtml");
    assertEquals("clean-xhtml", item.toString());
  }

  @Test
  public void XHTMLForcePublishNoCleanTest() throws Exception {
    // Example Mockito usage:
    Mockito.when(item.toString()).thenReturn("broken-xhtml");
    assertEquals("broken-xhtml", item.toString());
  }

  @AfterEach
  public void tearDown() {}
=======
  Mockery context = new JUnit4Mockery();
  IPSAssemblyItem item;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {

    item = context.mock(IPSAssemblyItem.class);
  }

  @Test
  /** Test that a clean XHTML file is echo'd clean. */
  public void XHTMLCleanTest() throws Exception {}

  @Test
  /** Test that a broken XHTML file is echo'd as is if No Clean and Force Publish are set. */
  public void XHTMLForcePublishNoCleanTest() throws Exception {}

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {}
>>>>>>> development-8.1.x
}
