// REFACTORED: CP-JAVA11

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.Guid;
import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.MainTest;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Content type catalog list must expose name/label/guid on the wire (Developer SPA table). See
 * issue #1693 hideFromMenu-only regression.
 */
@Tag("UnitTest")
public class ContentTypesTest extends MainTest {

  private final ContentTypesTestAdaptor adaptor = new ContentTypesTestAdaptor();

  @Test
  public void testListContentTypesPopulatesIdentityFields() {
    List<ContentType> list = adaptor.listContentTypes(URI.create("http://localhost/services/"));
    assertNotNull(list);
    assertFalse(list.isEmpty());

    ContentType first = list.get(0);
    assertEquals("percPage", first.getName());
    assertEquals("Page", first.getLabel());
    assertNotNull(first.getGuid());
  }

  @Test
  public void testListContentTypesJsonNotHideFromMenuOnly() {
    List<ContentType> list = adaptor.listContentTypes(URI.create("http://localhost/services/"));
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);
    String json = mapper.writeValueAsString(new ContentTypeList(list));

    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertFalse(
        json.replaceAll("\\s", "").equals("{\"ContentType\":[{\"hideFromMenu\":false}]}")
            || (json.contains("hideFromMenu") && !json.contains("\"name\"")),
        "List JSON must not be hideFromMenu-only: " + json);
  }

  @Test
  public void testContentTypePlainGettersRoundTrip() {
    ContentType ct = new ContentType();
    ct.setName("rffFile");
    ct.setLabel("File");
    ct.setGuid(new Guid("0-2-312"));
    ct.setHideFromMenu(true);

    assertEquals("rffFile", ct.getName());
    assertEquals("File", ct.getLabel());
    assertEquals(312, ct.getGuid().getUuid());
    assertTrue(ct.isHideFromMenu());
  }
}
