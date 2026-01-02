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
package percussion.soln.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
<<<<<<< HEAD
import org.mockito.Mockito;

/** // REFACTORED: CP-JAVA11 */
public class JCRMocks {
  public static Property mockProperty(String mockName, Node node, String name, String value)
      throws Exception {
    var property = Mockito.mock(Property.class, mockName);
    Mockito.when(node.getProperty(name)).thenReturn(property);
    Mockito.when(property.getString()).thenReturn(value);
    return property;
  }

  public static Property mockProperty(String mockName, Node node, String name, long value)
      throws Exception {
    var property = Mockito.mock(Property.class, mockName);
    Mockito.when(node.getProperty(name)).thenReturn(property);
    Mockito.when(property.getLong()).thenReturn(value);
=======
import org.jmock.Expectations;
import org.jmock.Mockery;

public class JCRMocks {

  Mockery mockery;

  public JCRMocks(Mockery mockery) {
    super();
    this.mockery = mockery;
  }

  public Property expectProperty(
      String mockName, final Node node, final String name, final String value) throws Exception {
    final Property property = mockery.mock(Property.class, mockName);
    mockery.checking(
        new Expectations() {
          {
            allowing(node).getProperty(name);
            will(returnValue(property));

            allowing(property).getString();
            will(returnValue(value));
          }
        });

    return property;
  }

  public Property expectProperty(
      String mockName, final Node node, final String name, final long value) throws Exception {
    final Property property = mockery.mock(Property.class, mockName);
    mockery.checking(
        new Expectations() {
          {
            allowing(node).getProperty(name);
            will(returnValue(property));

            allowing(property).getLong();
            will(returnValue(value));
          }
        });

>>>>>>> development-8.1.x
    return property;
  }
}
