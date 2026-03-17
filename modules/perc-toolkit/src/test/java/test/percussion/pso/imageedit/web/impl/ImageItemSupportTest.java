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
package test.percussion.pso.imageedit.web.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.IPSFieldValue;
import com.percussion.cms.objectstore.IPSItemAccessor;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemChild;
import com.percussion.cms.objectstore.PSItemChildEntry;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSItemFieldMeta;
import com.percussion.cms.objectstore.PSTextValue;
import com.percussion.pso.imageedit.data.ImageData;
import com.percussion.pso.imageedit.data.ImageMetaData;
import com.percussion.pso.imageedit.data.ImageSizeDefinition;
import com.percussion.pso.imageedit.data.MasterImageMetaData;
import com.percussion.pso.imageedit.data.SizedImageMetaData;
import com.percussion.pso.imageedit.services.ImageSizeDefinitionManager;
import com.percussion.pso.imageedit.services.cache.ImageCacheManager;
import com.percussion.pso.imageedit.web.impl.ImageItemSupport;
import com.percussion.pso.utils.RxItemUtils;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * // REFACTORED: CP-JAVA11
 *
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
public class ImageItemSupportTest {
  private static final Logger log = LogManager.getLogger(ImageItemSupportTest.class);

  @Mock IPSContentWs cws;
  @Mock IPSGuidManager gmgr;
  @Mock ImageSizeDefinitionManager isdm;
  @Mock ImageCacheManager cache;
  // make inner class static to avoid Mockito instantiation problem
  @InjectMocks TestableImageItemSupport cut;

  @BeforeEach
  public void setUp() {
    cut.setCws(cws);
    cut.setGmgr(gmgr);
    cut.setIsdm(isdm);
    cut.setCache(cache);
  }

  @Test
  public void testGetChild() {
    var child = Mockito.mock(PSItemChild.class);
    var item = Mockito.mock(PSCoreItem.class);
    Mockito.when(isdm.getSizedImageNodeName()).thenReturn("foo");
    Mockito.when(item.getChildByName("foo")).thenReturn(child);
    var c2 = cut.getChild(item);
    assertNotNull(c2);
    assertEquals(child, c2);
  }

  @Test
  public void testFindChildEntry() throws Exception {
    var entrya = Mockito.mock(PSItemChildEntry.class);
    var entryb = Mockito.mock(PSItemChildEntry.class);
    var entries = new ArrayList<PSItemChildEntry>();
    entries.add(entrya);
    entries.add(entryb);
    var flda = Mockito.mock(PSItemField.class);
    var fldb = Mockito.mock(PSItemField.class);
    var vala = Mockito.mock(IPSFieldValue.class);
    var valb = Mockito.mock(IPSFieldValue.class);
    Mockito.when(isdm.getSizedImagePropertyName()).thenReturn("size");
    Mockito.when(entrya.getFieldByName("size")).thenReturn(flda);
    Mockito.when(entryb.getFieldByName("size")).thenReturn(fldb);
    Mockito.when(flda.getValue()).thenReturn(vala);
    Mockito.when(fldb.getValue()).thenReturn(valb);
    Mockito.when(vala.getValueAsString()).thenReturn("a");
    Mockito.when(valb.getValueAsString()).thenReturn("b");
    var result = cut.findChildEntry(entries, "b");
    assertNotNull(result);
    assertEquals(entryb, result);
  }

  @Test
  public void testReadMetaData() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var master = new MasterImageMetaData();
    var fldmap = new HashMap<String, String>();
    fldmap.put("alt", "fld");
    var fld = Mockito.mock(PSItemField.class);
    var val = Mockito.mock(IPSFieldValue.class);
    var meta = Mockito.mock(PSItemFieldMeta.class);
    Mockito.when(item.getFieldByName("fld")).thenReturn(fld);
    Mockito.when(fld.getItemFieldMeta()).thenReturn(meta);
    Mockito.when(meta.getBackendDataType()).thenReturn(PSItemFieldMeta.DATATYPE_TEXT);
    Mockito.when(fld.getValue()).thenReturn(val);
    Mockito.when(val.getValue()).thenReturn("Alt Text");
    cut.readMetaData(item, master, fldmap);
    assertEquals("Alt Text", master.getAlt());
  }

  @Test
  public void testReadMetaDataBinary() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var master = new MasterImageMetaData();
    var fldmap = new HashMap<String, String>();
    fldmap.put("imageKey", "fld");
    // simulate binary value and cache interaction
    var bval = "The quick brown fox jumps over the lazy dog";
    Mockito.when(cache.addImage(Mockito.any(ImageData.class))).thenReturn("4345364345");
    try (MockedStatic<RxItemUtils> rx = Mockito.mockStatic(RxItemUtils.class)) {
      rx.when(() -> RxItemUtils.isBinaryField(item, "fld")).thenReturn(true);
      rx.when(() -> RxItemUtils.getFieldBinary(item, "fld")).thenReturn(bval.getBytes());
      // ensure readBinaryMetaData uses expected values
      rx.when(() -> RxItemUtils.getFieldValue(item, "fld" + "_filename")).thenReturn("fn");
      rx.when(() -> RxItemUtils.getFieldValue(item, "fld" + "_ext")).thenReturn("jpg");
      rx.when(() -> RxItemUtils.getFieldValue(item, "fld" + "_type")).thenReturn("image/jpeg");
      rx.when(() -> RxItemUtils.getFieldNumeric(item, "fld" + "_size")).thenReturn(123L);
      rx.when(() -> RxItemUtils.getFieldNumeric(item, "fld" + "_height")).thenReturn(10);
      rx.when(() -> RxItemUtils.getFieldNumeric(item, "fld" + "_width")).thenReturn(20);
      cut.readMetaData(item, master, fldmap);
    }
    assertNotNull(master.getImageKey());
    assertEquals("4345364345", master.getImageKey());
  }

  @Test
  public void testReadMetaDataImageSize() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var sized = new SizedImageMetaData();
    var fldmap = new HashMap<String, String>();
    fldmap.put("sizeDefinition", "fld");
    var sdef = new ImageSizeDefinition();
    sdef.setCode("sizeA");
    sdef.setLabel("Size A");
    var fld = Mockito.mock(PSItemField.class);
    var val = Mockito.mock(IPSFieldValue.class);
    var meta = Mockito.mock(PSItemFieldMeta.class);
    Mockito.when(item.getFieldByName("fld")).thenReturn(fld);
    Mockito.when(fld.getValue()).thenReturn(val);
    Mockito.when(val.getValueAsString()).thenReturn("sizeA");
    Mockito.when(isdm.getImageSize("sizeA")).thenReturn(sdef);
    cut.readMetaData(item, sized, fldmap);
    assertNotNull(sized.getSizeDefinition());
  }

  @Test
  public void testWriteMetadata() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var master = new MasterImageMetaData();
    master.setAlt("Alt Text");
    var fldmap = new HashMap<String, String>();
    fldmap.put("alt", "fld");
    var fld = Mockito.mock(PSItemField.class);
    var meta = Mockito.mock(PSItemFieldMeta.class);
    Mockito.when(item.getFieldByName("fld")).thenReturn(fld);
    Mockito.when(fld.getItemFieldMeta()).thenReturn(meta);
    Mockito.doNothing().when(fld).clearValues();
    Mockito.doNothing().when(fld).addValue(Mockito.any(PSTextValue.class));
    Mockito.when(meta.getBackendDataType()).thenReturn(PSItemFieldMeta.DATATYPE_TEXT);
    cut.writeMetaData(item, master, fldmap);
  }

  @Test
  public void testWriteMetadataBinary() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var master = new MasterImageMetaData();
    master.setImageKey("133245");
    var fldmap = new HashMap<String, String>();
    fldmap.put("imageKey", "fld");
    var fld = Mockito.mock(PSItemField.class);
    var meta = Mockito.mock(PSItemFieldMeta.class);
    var image = new ImageData();
    image.setBinary("Some String".getBytes());
    image.setFilename("file.name");
    image.setExt("jpg");
    image.setMimeType("text/plain");
    image.setSize(457L);
    image.setHeight(42);
    image.setWidth(37);
    Mockito.when(cache.getImage("133245")).thenReturn(image);
    // indicate binary field so writeMetaData takes that branch
    try (MockedStatic<RxItemUtils> rx = Mockito.mockStatic(RxItemUtils.class)) {
      rx.when(() -> RxItemUtils.isBinaryField(item, "fld")).thenReturn(true);
      // allow other static helpers to operate normally or be stubbed implicitly
      cut.writeMetaData(item, master, fldmap);
    }
  }

  @Test
  public void testWriteMetadataSizeDefinition() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var sized = new SizedImageMetaData();
    var sdef = new ImageSizeDefinition();
    sdef.setCode("sizeA");
    sdef.setLabel("Size A");
    sdef.setHeight(42);
    sdef.setWidth(37);
    sized.setSizeDefinition(sdef);
    var fldmap = new HashMap<String, String>();
    fldmap.put("sizeDefinition", "fld");
    var fld = Mockito.mock(PSItemField.class);
    Mockito.when(item.getFieldByName("fld")).thenReturn(fld);
    Mockito.doNothing().when(fld).clearValues();
    Mockito.doNothing().when(fld).addValue(Mockito.any(PSTextValue.class));
    cut.writeMetaData(item, sized, fldmap);
  }

  @Test
  public void testReadBinaryMetaData() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var image = new ImageMetaData();
    var fld_filename = Mockito.mock(PSItemField.class);
    var fld_ext = Mockito.mock(PSItemField.class);
    var fld_type = Mockito.mock(PSItemField.class);
    var fld_size = Mockito.mock(PSItemField.class);
    var fld_height = Mockito.mock(PSItemField.class);
    var fld_width = Mockito.mock(PSItemField.class);
    var val_filename = Mockito.mock(IPSFieldValue.class);
    var val_ext = Mockito.mock(IPSFieldValue.class);
    var val_type = Mockito.mock(IPSFieldValue.class);
    var val_size = Mockito.mock(IPSFieldValue.class);
    var val_height = Mockito.mock(IPSFieldValue.class);
    var val_width = Mockito.mock(IPSFieldValue.class);
    Mockito.when(item.getFieldByName("fld_filename")).thenReturn(fld_filename);
    Mockito.when(item.getFieldByName("fld_ext")).thenReturn(fld_ext);
    Mockito.when(item.getFieldByName("fld_type")).thenReturn(fld_type);
    Mockito.when(item.getFieldByName("fld_size")).thenReturn(fld_size);
    Mockito.when(item.getFieldByName("fld_height")).thenReturn(fld_height);
    Mockito.when(item.getFieldByName("fld_width")).thenReturn(fld_width);
    Mockito.when(fld_filename.getValue()).thenReturn(val_filename);
    Mockito.when(fld_ext.getValue()).thenReturn(val_ext);
    Mockito.when(fld_type.getValue()).thenReturn(val_type);
    Mockito.when(fld_size.getValue()).thenReturn(val_size);
    Mockito.when(fld_height.getValue()).thenReturn(val_height);
    Mockito.when(fld_width.getValue()).thenReturn(val_width);
    Mockito.when(val_filename.getValueAsString()).thenReturn("file.name");
    Mockito.when(val_ext.getValueAsString()).thenReturn("jpg");
    Mockito.when(val_type.getValueAsString()).thenReturn("text/html");
    Mockito.when(val_size.getValueAsString()).thenReturn("487");
    Mockito.when(val_height.getValueAsString()).thenReturn("42");
    Mockito.when(val_width.getValueAsString()).thenReturn("37");
    cut.readBinaryMetaData(item, image, "fld");
    assertEquals(37, image.getWidth());
    assertEquals(42, image.getHeight());
    assertEquals(487L, image.getSize());
    assertEquals("file.name", image.getFilename());
  }

  @Test
  public void testWriteBinaryMetaData() throws Exception {
    var item = Mockito.mock(IPSItemAccessor.class);
    var image = new ImageMetaData();
    image.setFilename("file.name");
    image.setExt("jpg");
    image.setMimeType("text/html");
    image.setSize(487L);
    image.setHeight(42);
    image.setWidth(37);
    var fld_filename = Mockito.mock(PSItemField.class);
    var fld_ext = Mockito.mock(PSItemField.class);
    var fld_type = Mockito.mock(PSItemField.class);
    var fld_size = Mockito.mock(PSItemField.class);
    var fld_height = Mockito.mock(PSItemField.class);
    var fld_width = Mockito.mock(PSItemField.class);
    Mockito.when(item.getFieldByName("fld_filename")).thenReturn(fld_filename);
    Mockito.when(item.getFieldByName("fld_ext")).thenReturn(fld_ext);
    Mockito.when(item.getFieldByName("fld_type")).thenReturn(fld_type);
    Mockito.when(item.getFieldByName("fld_size")).thenReturn(fld_size);
    Mockito.when(item.getFieldByName("fld_height")).thenReturn(fld_height);
    Mockito.when(item.getFieldByName("fld_width")).thenReturn(fld_width);
    Mockito.doNothing().when(fld_filename).clearValues();
    Mockito.doNothing().when(fld_ext).clearValues();
    Mockito.doNothing().when(fld_type).clearValues();
    Mockito.doNothing().when(fld_size).clearValues();
    Mockito.doNothing().when(fld_height).clearValues();
    Mockito.doNothing().when(fld_width).clearValues();
    Mockito.doNothing().when(fld_filename).addValue(Mockito.any(IPSFieldValue.class));
    Mockito.doNothing().when(fld_ext).addValue(Mockito.any(IPSFieldValue.class));
    Mockito.doNothing().when(fld_type).addValue(Mockito.any(IPSFieldValue.class));
    Mockito.doNothing().when(fld_size).addValue(Mockito.any(IPSFieldValue.class));
    Mockito.doNothing().when(fld_height).addValue(Mockito.any(IPSFieldValue.class));
    Mockito.doNothing().when(fld_width).addValue(Mockito.any(IPSFieldValue.class));
    cut.writeBinaryMetaData(item, image, "fld");
  }

  private static class TestableImageItemSupport extends ImageItemSupport {
    @Override
    public PSItemChildEntry findChildEntry(List<PSItemChildEntry> entries, String code)
        throws Exception {
      return super.findChildEntry(entries, code);
    }

    @Override
    public PSItemChild getChild(PSCoreItem item) {
      return super.getChild(item);
    }

    @Override
    public void readBinaryMetaData(IPSItemAccessor item, ImageMetaData image, String fieldName)
        throws Exception {
      super.readBinaryMetaData(item, image, fieldName);
    }

    @Override
    public void readMetaData(IPSItemAccessor item, Object bean, Map<String, String> fieldMap)
        throws Exception {
      super.readMetaData(item, bean, fieldMap);
    }

    @Override
    public void writeBinaryMetaData(IPSItemAccessor item, ImageMetaData image, String fieldName)
        throws Exception {
      super.writeBinaryMetaData(item, image, fieldName);
    }

    @Override
    public void writeMetaData(IPSItemAccessor item, Object bean, Map<String, String> fieldMap)
        throws Exception {
      super.writeMetaData(item, bean, fieldMap);
    }
  }
}
