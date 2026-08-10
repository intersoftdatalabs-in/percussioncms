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
package test.percussion.soln.jcr.data;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.soln.jcr.data.PropertyData;
import com.percussion.soln.jcr.data.ValueData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Serial field hygiene + single-value constructor coverage for {@link PropertyData}. */
public class PropertyDataTest {

  @Test
  void singleValueConstructorStoresValue() {
    ValueData vd = new ValueData("hello");
    PropertyData pd = new PropertyData(vd);
    assertFalse(pd.isMultiple());
    assertEquals(1, pd.getValues().size());
    assertSame(vd, pd.getValues().get(0));
  }

  @Test
  void singleValueConstructorRejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> new PropertyData(null));
  }

  @Test
  void setValuesCopiesIntoSerializableArrayList() {
    PropertyData pd = new PropertyData();
    List<ValueData> input = List.of(new ValueData("a"), new ValueData("b"));
    pd.setValues(input);
    pd.setMultiple(true);
    assertEquals(2, pd.getValues().size());
    assertInstanceOf(ArrayList.class, pd.getValues());
    // defensive copy
    assertNotSame(input, pd.getValues());
  }

  @Test
  void roundTripSerialization() throws Exception {
    PropertyData original = new PropertyData(new ValueData("serial-me"));
    original.setName("prop");

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(original);
      bytes = bos.toByteArray();
    }

    PropertyData restored;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      restored = (PropertyData) ois.readObject();
    }

    assertEquals("prop", restored.getName());
    assertEquals(1, restored.getValues().size());
    assertEquals("serial-me", restored.getValues().get(0).getStringData());
  }
}
