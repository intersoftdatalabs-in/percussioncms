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
/*
 * test.percussion.pso.preview PreviewLocationTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.preview.PreviewLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PreviewLocationTest {

  PreviewLocation cut;

  @BeforeEach
  public void setUp() {
=======
import static org.junit.Assert.*;

import com.percussion.pso.preview.PreviewLocation;
import org.junit.Before;
import org.junit.Test;

public class PreviewLocationTest {

  PreviewLocation cut;

  @Before
  public void setUp() throws Exception {
>>>>>>> development-8.1.x
    cut = new PreviewLocation();
    cut.setPath("myPath");
    cut.setSiteName("mySite");
    cut.setUrl("//myUrl");
  }

  @Test
<<<<<<< HEAD
  void testCompareTo() {
=======
  public final void testCompareTo() {
>>>>>>> development-8.1.x
    PreviewLocation other = new PreviewLocation();
    other.setPath("myPath");
    other.setSiteName("mySite");

    int result = cut.compareTo(other);
    assertEquals(0, result);
  }
}
