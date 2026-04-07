package com.percussion.pagemanagement;

import static org.junit.Assert.*;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegionWidgets;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.share.dao.PSSerializerUtils;
import java.util.Map;
import org.junit.Test;

public class PSWidgetPropertyRoundtripTest {

  @Test
  public void testWrapperPropertyUnmarshalMarshal() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Page>"
            + "  <regionBranches>"
            + "    <regions><region><regionId>header</regionId></region></regions>"
            + "    <regionWidgetAssociations>"
            + "      <regionWidget>"
            + "        <regionId>header</regionId>"
            + "        <widgetItems>"
            + "          <widgetItem>"
            + "            <id>209898460</id>"
            + "            <definitionId>percTitle</definitionId>"
            + "            <properties>"
            + "              <property>"
            + "                <name>wrapper</name>"
            + "                <value>\"h2\"</value>"
            + "              </property>"
            + "            </properties>"
            + "            <cssProperties/>"
            + "          </widgetItem>"
            + "        </widgetItems>"
            + "      </regionWidget>"
            + "    </regionWidgetAssociations>"
            + "  </regionBranches>"
            + "</Page>";

    PSPage page = PSSerializerUtils.unmarshal(xml, PSPage.class);
    assertNotNull(page);
    assertNotNull(page.getRegionBranches());

    // navigate to region widget items
    assertNotNull(page.getRegionBranches().getRegionWidgetAssociations());
    boolean found = false;
    for (PSRegionWidgets rw : page.getRegionBranches().getRegionWidgetAssociations()) {
      if ("header".equals(rw.getRegionId())) {
        for (PSWidgetItem wi : rw.getWidgetItems()) {
          if ("percTitle".equals(wi.getDefinitionId())) {
            Map<String, Object> props = wi.getProperties();
            assertNotNull(props);
            Object wrapper = props.get("wrapper");
            // Expect the JSON string value "h2" to unmarshal to Java String "h2"
            assertEquals("h2", wrapper);
            found = true;
          }
        }
      }
    }
    assertTrue(found);

    // Marshal back and ensure wrapper value is preserved as JSON string
    String marshaled = PSSerializerUtils.marshal(page.getRegionBranches());
    assertNotNull(marshaled);
    assertTrue(marshaled.contains("<name>wrapper</name>"));
    assertTrue(marshaled.contains("<value>\"h2\"</value>") || marshaled.contains("&quot;h2&quot;"));
  }
}
